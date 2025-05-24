package com.example.weatherapplication.data.model

data class FavoriteCityWithWeatherResponse(
    val city: CitySearchItem,
    val weather: WeatherResponse? // cała pogoda (może być null)
)