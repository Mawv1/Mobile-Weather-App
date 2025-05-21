package com.example.weatherapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntry(
    @PrimaryKey val cityId: String,
    val timestamp: Long,
    val weatherJson: String,
    val forecastJson: String
)