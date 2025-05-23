package com.example.weatherapplication.data.model

data class CityWeatherData(
    val city: CitySearchItem,
    val temperature: Double,
    val pressure: Int,
    val localTime: String,
    val weatherDescription: String,
    val weatherIconCode: String
)
