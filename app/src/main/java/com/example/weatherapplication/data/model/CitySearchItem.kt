package com.example.weatherapplication.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CitySearchItem(
    val name: String,
    val country: String,
    val state: String?,
    val lat: Double,
    val lon: Double
)