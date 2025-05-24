//package com.example.weatherapplication.data.local
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//import androidx.room.TypeConverters
//import com.example.weatherapplication.data.local.converters.WeatherResponseConverter
//import com.example.weatherapplication.data.model.CitySearchItem
//import com.example.weatherapplication.data.model.WeatherResponse
//
//@Entity(tableName = "favorite_cities")
//@TypeConverters(WeatherResponseConverter::class)
//data class FavoriteCityWithWeather(
//    @PrimaryKey val id: String, // np. "${lat},${lon}"
//    val city: CitySearchItem,
//    val weather: WeatherResponse? // cała pogoda (może być null)
//)
