package com.example.weatherapplication.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CityWithWeatherResponse(
    val city: CitySearchItem,
    val weather: WeatherResponse?,
    val forecast: List<DailyForecast>? = null
)