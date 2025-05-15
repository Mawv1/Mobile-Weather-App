package com.example.weatherapplication.data.repository

import com.example.weatherapplication.data.api.OpenWeatherMapService
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.WeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WeatherRepository(
    private val apiKey: String,
    private val api: OpenWeatherMapService = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(OpenWeatherMapService::class.java)
) {

    suspend fun getWeather(city: String): WeatherResponse {
        return api.getCurrentWeather(city, apiKey)
    }

    suspend fun searchCitiesByName(cityName: String): List<CitySearchItem> {
        return api.searchCities(cityName, 5, apiKey)
    }
}
