package com.example.weatherapplication.data.model

data class CityWithWeatherResponse(
    val city: CitySearchItem,
    val weather: WeatherResponse? // cała pogoda (może być null)
)