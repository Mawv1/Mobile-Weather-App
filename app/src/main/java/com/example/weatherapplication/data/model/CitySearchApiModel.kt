package com.example.weatherapplication.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CitySearchApiModel(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
)