package com.example.weatherapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_cities")
data class FavoriteCityEntity(
    @PrimaryKey val cityId: String,       // np. "lat_lon" jako unikalny klucz
    val cityJson: String,                 // JSON obiektu CitySearchItem
    val weatherJson: String,              // JSON obiektu WeatherResponse
    val timestamp: Long                   // czas zapisu (może się przydać do odświeżania)
)
