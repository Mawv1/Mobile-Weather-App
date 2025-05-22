package com.example.weatherapplication.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    val list: List<ForecastItem>,
    val city: City
)

@JsonClass(generateAdapter = true)
data class ForecastItem(
    val dt: Long,  // timestamp w sekundach
    val main: MainData,
    val weather: List<WeatherDescription>
)

@JsonClass(generateAdapter = true)
data class MainData(
    val temp: Double,
    val temp_min: Double,
    val temp_max: Double
)

@JsonClass(generateAdapter = true)
data class WeatherDescription(
    val icon: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
    val date: String,          // np. "2025-05-22"
    val temperature: Double,   // średnia temperatura dnia lub temp w konkretnym czasie
    val weatherIconCode: String
)

@JsonClass(generateAdapter = true)
data class City(
    val name: String,
    val country: String
)
