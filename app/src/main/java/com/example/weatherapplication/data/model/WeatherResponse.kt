package com.example.weatherapplication.data.model

import androidx.lifecycle.GeneratedAdapter
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val name: String,
    val coord: Coord,
    val weather: List<Weather>,
    val main: Main,
    val wind: Wind
)

@JsonClass(generateAdapter = true)
data class Coord(val lon: Double, val lat: Double)

@JsonClass(generateAdapter = true)
data class Weather(
    val description: String,
    val icon: String,
    val main: String
)

@JsonClass(generateAdapter = true)
data class Main(val temp: Double)

@JsonClass(generateAdapter = true)
data class Wind(val speed: Double, val deg: Int)