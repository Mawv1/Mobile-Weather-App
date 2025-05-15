package com.example.weatherapplication.data.model

data class CitySearchItem(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)