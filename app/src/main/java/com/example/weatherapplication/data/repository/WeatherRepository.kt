package com.example.weatherapplication.data.repository

import com.example.weatherapplication.data.api.OpenWeatherMapService
import com.example.weatherapplication.data.model.WeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WeatherRepository(    private val api: OpenWeatherMapService = Retrofit.Builder()
    .baseUrl("https://api.openweathermap.org/")
    .addConverterFactory(MoshiConverterFactory.create())
    .build()
    .create(OpenWeatherMapService::class.java)) {

    suspend fun fetchCurrentWeather(city: String): WeatherResponse {
        return api.getCurrentWeather(city)
    }

    suspend fun getWeather(city: String): WeatherResponse {
        return api.getCurrentWeather(city)
    }
}