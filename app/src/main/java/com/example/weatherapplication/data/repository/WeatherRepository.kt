package com.example.weatherapplication.data.repository

import android.util.Log
import com.example.weatherapplication.BuildConfig
import com.example.weatherapplication.data.api.OpenWeatherMapService
import com.example.weatherapplication.data.model.CitySearchApiModel
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant
import java.time.ZoneId

class WeatherRepository(
    private val apiKey: String = BuildConfig.WEATHER_API_KEY
) {
    // 1. HTTP‑logging
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // 2. Moshi z KotlinJsonAdapterFactory
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // 3. Retrofit z MoshiConverterFactory i klientem
    private val api: OpenWeatherMapService = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenWeatherMapService::class.java)

    suspend fun getWeather(city: CitySearchItem): WeatherResponse =
        api.getCurrentWeather(city.name, apiKey, "metric", "pl", city.lat, city.lon)

    suspend fun searchCitiesByName(cityName: String): List<CitySearchItem> {
        val response = api.getCities(cityName, limit = 5, apiKey = apiKey)
        return response.map {
            CitySearchItem(it.name, it.country, it.state ?: "", it.lat, it.lon)
        }
    }

    suspend fun getWeatherByCoordinates(lat: Double, lon: Double): WeatherResponse =
        api.getWeatherByCoordinates(lat, lon, apiKey)

    suspend fun getForecast(lat: Double, lon: Double, days: Int): List<DailyForecast> {
        Log.d("WeatherRepo", ">>> getForecast(lat=$lat, lon=$lon, days=$days)")
        val response = api.getOneCallForecast(
            lat     = lat,
            lon     = lon,
            apiKey  = apiKey
        )
        Log.d("WeatherRepo", "<<< daily.size = ${response.daily.size}")
        return response.daily.take(days)
    }
}

