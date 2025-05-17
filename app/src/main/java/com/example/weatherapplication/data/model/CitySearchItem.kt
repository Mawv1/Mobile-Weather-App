package com.example.weatherapplication.data.model

data class CitySearchItem(
    val name: String,
    val country: String,
    val state: String?,
    val lat: Double,
    val lon: Double
)