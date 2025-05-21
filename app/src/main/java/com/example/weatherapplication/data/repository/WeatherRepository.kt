package com.example.weatherapplication.data.repository

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

class WeatherRepository(
    private val apiKey: String = BuildConfig.WEATHER_API_KEY,
    private val cacheDao: WeatherCacheDao,
    private val moshi: Moshi,
) {
    // HTTP logging
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // Retrofit z Moshi
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    private val api: OpenWeatherMapService = retrofit.create(OpenWeatherMapService::class.java)

    /**
     * Pobiera aktualną pogodę po nazwie miasta
     */
    suspend fun getWeatherForCity(city: CitySearchItem): WeatherResponse =
        api.getCurrentWeather(
            cityName = city.name,
            apiKey = apiKey,
            units = "metric",
            lang = "pl",
            lat = city.lat,
            lon = city.lon
        )

    /**
     * Wyszukiwanie miast po nazwie
     */
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

    /**
     * Pobiera aktualną pogodę po współrzędnych
     */
    suspend fun getWeatherByCoordinates(lat: Double, lon: Double): WeatherResponse =
        api.getWeatherByCoordinates(latitude = lat, longitude = lon, apiKey = apiKey)

    /**
     * Pobiera prognozę dniową za pomocą One Call API
     */
    suspend fun getForecastByCoordinates(lat: Double, lon: Double, days: Int): List<DailyForecast> {
        Log.d("WeatherRepo", ">>> getForecast(lat=$lat, lon=$lon, days=$days)")
        val response = api.getOneCallForecast(lat = lat, lon = lon, apiKey = apiKey)
        Log.d("WeatherRepo", "<<< daily.size = ${'$'}{response.daily.size}")
        return response.daily.take(days)
    }

    /**
     * Odświeża dane: pobiera z API i zapisuje do cache'u
     */
    suspend fun refreshWeather(
        city: CitySearchItem,
        days: Int = 5
    ): Pair<WeatherResponse, List<DailyForecast>> = withContext(Dispatchers.IO) {
        val weather = getWeatherByCoordinates(city.lat, city.lon)
        val forecast = getForecastByCoordinates(city.lat, city.lon, days)

        // Serializacja JSON
        val weatherJson = moshi.adapter(WeatherResponse::class.java).toJson(weather)
        val listType = Types.newParameterizedType(List::class.java, DailyForecast::class.java)
        val forecastJson = moshi.adapter<List<DailyForecast>>(listType).toJson(forecast)

        // Zapis do cache
        val entry = WeatherCacheEntry(
            cityId = "${'$'}{city.lat}_${'$'}{city.lon}",
            timestamp = System.currentTimeMillis(),
            weatherJson = weatherJson,
            forecastJson = forecastJson
        )
        cacheDao.insert(entry)
        weather to forecast
    }

    /**
     * Pobiera dane z cache'u jeśli istnieją i nie wygasły
     */
    suspend fun getCachedWeather(
        city: CitySearchItem,
        ttlMillis: Long = 60 * 60 * 1000L
    ): Pair<WeatherResponse, List<DailyForecast>>? = withContext(Dispatchers.IO) {
        val cityId = "${'$'}{city.lat}_${'$'}{city.lon}"
        val entry = cacheDao.get(cityId) ?: return@withContext null
        if (System.currentTimeMillis() - entry.timestamp > ttlMillis) return@withContext null

        // Deserializacja JSON
        val weather = moshi.adapter(WeatherResponse::class.java).fromJson(entry.weatherJson)
            ?: return@withContext null
        val listType = Types.newParameterizedType(List::class.java, DailyForecast::class.java)
        val forecast = moshi.adapter<List<DailyForecast>>(listType).fromJson(entry.forecastJson)
            ?: return@withContext null

        weather to forecast
    }
}
