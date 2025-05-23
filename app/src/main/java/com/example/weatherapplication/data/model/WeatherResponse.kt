package com.example.weatherapplication.data.model

import androidx.lifecycle.GeneratedAdapter
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val name: String,
    val coord: Coord,
    val main: Main,
    val wind: Wind,
    val weather: List<Weather>,
    val sys: Sys,
    val clouds: Clouds,
    val visibility: Int
) {
    fun toCitySearchItem(): CitySearchItem {
        return CitySearchItem(
            name = this.name,
            country = this.sys.country,
            state = null,
            lat = this.coord.lat,
            lon = this.coord.lon
        )
    }
}

@JsonClass(generateAdapter = true)
data class Coord(val lon: Double, val lat: Double)

@JsonClass(generateAdapter = true)
data class Weather(
    val description: String,
    val icon: String,
    val main: String
)

@JsonClass(generateAdapter = true)
data class Main(
    val temp: Double,
    val pressure: Int,
    val humidity: Int
)

@JsonClass(generateAdapter = true)
data class Wind(val speed: Double, val deg: Int)

@JsonClass(generateAdapter = true)
data class Sys(
    val country: String,
    val sunrise: Long,
    val sunset: Long
)

@JsonClass(generateAdapter = true)
data class Clouds(
    val all: Int
)