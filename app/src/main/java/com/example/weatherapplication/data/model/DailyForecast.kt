package com.example.weatherapplication.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    val list: List<DailyForecast>,
    val city: City
)

@JsonClass(generateAdapter = true)
data class OneCallResponse(
    val daily: List<DailyForecast>
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
    val dt: Long,
    val temp: TempForecast,
    val weather: List<WeatherDescription>
)
@JsonClass(generateAdapter = true)
data class TempForecast(
    val day: Double,
    val min: Double,
    val max: Double
)
@JsonClass(generateAdapter = true)
data class WeatherDescription(val description: String)

@JsonClass(generateAdapter = true)
data class City(
    val name: String,
    val country: String
)

