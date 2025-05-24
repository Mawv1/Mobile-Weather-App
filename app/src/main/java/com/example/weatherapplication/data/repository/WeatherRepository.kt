package com.example.weatherapplication.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.weatherapplication.BuildConfig
import com.example.weatherapplication.data.api.OpenWeatherMapService
import com.example.weatherapplication.data.local.WeatherCacheDao
import com.example.weatherapplication.data.local.WeatherCacheEntry
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant
import java.time.ZoneId

class WeatherRepository(
    private val apiKey: String = BuildConfig.WEATHER_API_KEY,
    private val cacheDao: WeatherCacheDao,
    private val moshi: Moshi,
    private val sharedPreferences: SharedPreferences,
) {
    companion object {
        private const val UNITS_KEY = "units_key"
        private const val DEFAULT_UNITS = "metric"

        private const val LAST_CITY_LAT_KEY = "last_city_lat"
        private const val LAST_CITY_LON_KEY = "last_city_lon"

        private const val REFRESH_INTERVAL_KEY = "refresh_interval"
        private const val DEFAULT_REFRESH_INTERVAL = 60

        private const val CACHE_TTL_MILLIS = 60 * 60 * 1000L
    }

    private var refreshIntervalMinutes: Int = sharedPreferences.getInt(REFRESH_INTERVAL_KEY, DEFAULT_REFRESH_INTERVAL)

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: OpenWeatherMapService = retrofit.create(OpenWeatherMapService::class.java)

    suspend fun searchCitiesByName(cityName: String): List<CitySearchItem> {
        val response = api.getCities(cityName = cityName, limit = 5, apiKey = apiKey)
        return response.map {
            CitySearchItem(
                name = it.name,
                country = it.country,
                state = it.state ?: "",
                lat = it.lat,
                lon = it.lon
            )
        }
    }

    suspend fun getWeatherByCoordinates(lat: Double, lon: Double, units: String): WeatherResponse {
        return api.getWeatherByCoordinates(
            latitude = lat,
            longitude = lon,
            units = if (units in listOf("metric", "imperial")) units else DEFAULT_UNITS,
            apiKey = apiKey
        )
    }

    suspend fun getForecastByCoordinates(lat: Double, lon: Double, days: Int, units: String): List<DailyForecast> {
        val response = api.getForecast(lat = lat, lon = lon, units = units, apiKey = apiKey)

        val dailyGroups = response.list.groupBy {
            Instant.ofEpochSecond(it.dt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }

        return dailyGroups.entries.take(days).map { (date, items) ->
            val averageTemp = items.map { it.main.temp }.average()
            val minTemp = items.minOf { it.main.temp_min }
            val maxTemp = items.maxOf { it.main.temp_max }
            val weatherIconCode = items.firstOrNull()?.weather?.firstOrNull()?.icon ?: ""

            DailyForecast(
                date = date,
                temperature = averageTemp,
                minTemperature = minTemp,
                maxTemperature = maxTemp,
                weatherIconCode = weatherIconCode
            )
        }.sortedBy { it.date
        }
    }

    suspend fun getWeatherAndForecast(
        city: CitySearchItem,
        units: String,
        days: Int = 5
    ): Pair<WeatherResponse, List<DailyForecast>>? = withContext(Dispatchers.IO) {
        val cityId = getCityId(city)
        val cacheTtl = refreshIntervalMinutes * 60 * 1000L
        val cached = getCachedWeather(cityId, cacheTtl)

        if (cached != null) {
            Log.d("WeatherRepository", "Using cached data for ${city.name}")
            return@withContext cached
        }

        return@withContext try {
            Log.d("WeatherRepository", "Cache expired or missing, refreshing data for ${city.name}")
            val result = refreshWeather(city, units, days)
            saveLastSelectedCity(city)
            result
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Network error: ${e.message}")
            getCachedWeather(cityId, Long.MAX_VALUE)
        }
    }

    suspend fun refreshWeather(city: CitySearchItem, units: String, days: Int = 5): Pair<WeatherResponse, List<DailyForecast>> =
        withContext(Dispatchers.IO) {
            val weather = getWeatherByCoordinates(city.lat, city.lon, units)
            val forecast = getForecastByCoordinates(city.lat, city.lon, days, units)

            val weatherJson = moshi.adapter(WeatherResponse::class.java).toJson(weather)
            val listType = Types.newParameterizedType(List::class.java, DailyForecast::class.java)
            val forecastJson = moshi.adapter<List<DailyForecast>>(listType).toJson(forecast)

            val entry = WeatherCacheEntry(
                cityId = getCityId(city),
                timestamp = System.currentTimeMillis(),
                weatherJson = weatherJson,
                forecastJson = forecastJson
            )

            cacheDao.insert(entry)
            saveLastSelectedCity(city)
            weather to forecast
        }

    private fun getCityId(city: CitySearchItem): String {
        return "${city.lat}_${city.lon}"
    }

    private suspend fun getCachedWeather(cityId: String, ttlMillis: Long): Pair<WeatherResponse, List<DailyForecast>>? {
        val entry = cacheDao.get(cityId) ?: return null
        if (System.currentTimeMillis() - entry.timestamp > ttlMillis) return null

        val weather = moshi.adapter(WeatherResponse::class.java).fromJson(entry.weatherJson)
            ?: return null
        val listType = Types.newParameterizedType(List::class.java, DailyForecast::class.java)
        val forecast = moshi.adapter<List<DailyForecast>>(listType).fromJson(entry.forecastJson)
            ?: return null

        return weather to forecast
    }

    fun saveLastSelectedCity(city: CitySearchItem) {
        sharedPreferences.edit()
            .putString(LAST_CITY_LAT_KEY, city.lat.toString())
            .putString(LAST_CITY_LON_KEY, city.lon.toString())
            .apply()
    }

    fun getLastSelectedCity(): CitySearchItem? {
        val latStr = sharedPreferences.getString(LAST_CITY_LAT_KEY, null)
        val lonStr = sharedPreferences.getString(LAST_CITY_LON_KEY, null)
        if (latStr == null || lonStr == null) return null

        val lat = latStr.toDoubleOrNull() ?: return null
        val lon = lonStr.toDoubleOrNull() ?: return null

        return CitySearchItem(
            name = "Ostatnia lokalizacja",
            country = "",
            state = "",
            lat = lat,
            lon = lon
        )
    }

    fun getUnits(): String = sharedPreferences.getString(UNITS_KEY, DEFAULT_UNITS) ?: DEFAULT_UNITS
    fun setUnits(units: String) = sharedPreferences.edit().putString(UNITS_KEY, units).apply()

    fun getRefreshInterval(): Int = sharedPreferences.getInt(REFRESH_INTERVAL_KEY, DEFAULT_REFRESH_INTERVAL)
    fun setRefreshInterval(value: Int) {
        refreshIntervalMinutes = value
        sharedPreferences.edit().putInt(REFRESH_INTERVAL_KEY, value).apply()
    }
}
