package com.example.weatherapplication.data.repository

import com.example.weatherapplication.data.api.OpenWeatherMapService
import com.example.weatherapplication.data.model.CitySearchApiModel
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

    suspend fun getWeather(city: CitySearchItem): WeatherResponse {
        return api.getCurrentWeather(city.name, apiKey, "metric" ,"pl", city.lat, city.lon)
    }

    suspend fun searchCitiesByName(cityName: String): List<CitySearchItem> {
        val response = api.getCities(cityName, 5, apiKey)
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
}
