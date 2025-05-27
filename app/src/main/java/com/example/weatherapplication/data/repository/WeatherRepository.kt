package com.example.weatherapplication.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.weatherapplication.BuildConfig
import com.example.weatherapplication.data.api.OpenWeatherMapService
import com.example.weatherapplication.data.local.WeatherCacheDao
import com.example.weatherapplication.data.local.WeatherCacheEntry
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    init {
        Log.d("WeatherRepository", "WeatherRepository instance created: $this")
    }
    companion object {
        private const val UNITS_KEY = "units_key"
        private const val DEFAULT_UNITS = "metric"

        private const val LAST_CITY_LAT_KEY = "last_city_lat"
        private const val LAST_CITY_LON_KEY = "last_city_lon"

        private const val REFRESH_INTERVAL_KEY = "refresh_interval"
        private const val DEFAULT_REFRESH_INTERVAL = 60
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

    suspend fun searchCitiesByName(cityName: String): List<CityWithWeatherResponse> {
        val response = api.getCities(cityName = cityName, limit = 5, apiKey = apiKey)

        return response.map { cityApiModel ->
            val (weather, forecastResponse) = coroutineScope {
                val weatherDeferred = async(Dispatchers.IO) {
                    api.getWeatherByCoordinates(
                        latitude = cityApiModel.lat,
                        longitude = cityApiModel.lon,
                        apiKey = apiKey,
                        units = getCurrentUnits()
                    )
                }

                val forecastDeferred = async(Dispatchers.IO) {
                    api.getForecast(
                        lat = cityApiModel.lat,
                        lon = cityApiModel.lon,
                        apiKey = apiKey,
                        units = getCurrentUnits()
                    )
                }

                weatherDeferred.await() to forecastDeferred.await()
            }

            val dailyForecastList = forecastResponse.toDailyForecastList()

            CityWithWeatherResponse(
                city = CitySearchItem(
                    name = cityApiModel.name,
                    country = cityApiModel.country,
                    state = cityApiModel.state ?: "",
                    lat = cityApiModel.lat,
                    lon = cityApiModel.lon
                ),
                weather = weather,
                forecast = dailyForecastList
            )
        }
    }

    suspend fun getWeatherByCoordinates(
        lat: Double,
        lon: Double,
        units: String
    ): WeatherResponse {
        return api.getWeatherByCoordinates(
            latitude = lat,
            longitude = lon,
            units = if (units in listOf("metric", "imperial", "standard")) units else DEFAULT_UNITS,
            apiKey = apiKey
        )
    }

    suspend fun getForecastByCoordinates(
        lat: Double,
        lon: Double,
        days: Int,
        units: String
    ): List<DailyForecast> {
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
        }.sortedBy { it.date }
    }

    suspend fun getWeatherAndForecast(
        cityWithWeather: CityWithWeatherResponse,
        units: String = getCurrentUnits(),
        days: Int = 5
    ): CityWithWeatherResponse? = withContext(Dispatchers.IO) {
        val city = cityWithWeather.city
        val cityId = getCityId(cityWithWeather)
        val ttlMillis = refreshIntervalMinutes * 60 * 1000L
        val cached = getCachedWeather(cityId, ttlMillis)
        if (cached != null) {
            Log.d("WeatherRepository", "Using cached data for ${city.name}")
            return@withContext CityWithWeatherResponse(
                city = city,
                weather = cached.first,
                forecast = cached.second
            )
        }

        try {
            Log.d("WeatherRepository", "Cache expired or missing, refreshing data for ${city.name}")
            val freshData = refreshWeather(cityWithWeather, units, days)
            saveLastSelectedCity(cityWithWeather)
            return@withContext CityWithWeatherResponse(
                city = city,
                weather = freshData.first,
                forecast = freshData.second
            )
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Network error: ${e.message}")
            val fallback = getCachedWeather(cityId, Long.MAX_VALUE)
            return@withContext fallback?.let {
                CityWithWeatherResponse(
                    city = city,
                    weather = it.first,
                    forecast = it.second
                )
            }
        }
    }

    suspend fun refreshWeather(
        cityWithWeather: CityWithWeatherResponse,
        units: String = getCurrentUnits(),
        days: Int = 5
    ): Pair<WeatherResponse, List<DailyForecast>> = withContext(Dispatchers.IO) {
        val city = cityWithWeather.city
        val weather = getWeatherByCoordinates(city.lat, city.lon, units)
        val forecast = getForecastByCoordinates(city.lat, city.lon, days, units)

        val weatherJson = moshi.adapter(WeatherResponse::class.java).toJson(weather)
        val listType = Types.newParameterizedType(List::class.java, DailyForecast::class.java)
        val forecastJson = moshi.adapter<List<DailyForecast>>(listType).toJson(forecast)

        val entry = WeatherCacheEntry(
            cityId = getCityId(cityWithWeather),
            timestamp = System.currentTimeMillis(),
            weatherJson = weatherJson,
            forecastJson = forecastJson
        )

        cacheDao.insert(entry)
        saveLastSelectedCity(cityWithWeather)
        weather to forecast
    }

    private fun getCityId(cityWithWeather: CityWithWeatherResponse): String {
        return "${cityWithWeather.city.lat}_${cityWithWeather.city.lon}"
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

    fun saveLastSelectedCity(cityWithWeather: CityWithWeatherResponse) {
        sharedPreferences.edit()
            .putString(LAST_CITY_LAT_KEY, cityWithWeather.city.lat.toString())
            .putString(LAST_CITY_LON_KEY, cityWithWeather.city.lon.toString())
            .apply()
    }

    suspend fun getLastSelectedCity(): CityWithWeatherResponse? {
        val latStr = sharedPreferences.getString(LAST_CITY_LAT_KEY, null)
        val lonStr = sharedPreferences.getString(LAST_CITY_LON_KEY, null)
        if (latStr == null || lonStr == null) return null

        val lat = latStr.toDoubleOrNull() ?: return null
        val lon = lonStr.toDoubleOrNull() ?: return null

        val forecast = withContext(Dispatchers.IO) {
            getForecastByCoordinates(lat, lon, 5, getCurrentUnits())
        }

        return CityWithWeatherResponse(
            city = CitySearchItem(
                name = "Ostatnia lokalizacja",
                country = "",
                state = "",
                lat = lat,
                lon = lon
            ),
            weather = WeatherResponse(
                name = "Ostatnia lokalizacja",
                coord = WeatherResponse.Coord(lon, lat),
                main = WeatherResponse.Main(0.0, 0, 0),
                wind = WeatherResponse.Wind(0.0, 0),
                weather = listOf(WeatherResponse.Weather("Brak danych", "", "")),
                sys = WeatherResponse.Sys("", 0, 0),
                clouds = WeatherResponse.Clouds(0),
                visibility = 0
            ),
            forecast = forecast
        )
    }
    fun setUnits(units: String) {
        sharedPreferences.edit().putString(UNITS_KEY, units).apply()
    }

    fun getCurrentUnits(): String {
        return sharedPreferences.getString(UNITS_KEY, DEFAULT_UNITS) ?: DEFAULT_UNITS
    }

    fun getRefreshInterval(): Int = sharedPreferences.getInt(REFRESH_INTERVAL_KEY, DEFAULT_REFRESH_INTERVAL)
    fun setRefreshInterval(value: Int) {
        refreshIntervalMinutes = value
        sharedPreferences.edit().putInt(REFRESH_INTERVAL_KEY, value).apply()
    }
}
