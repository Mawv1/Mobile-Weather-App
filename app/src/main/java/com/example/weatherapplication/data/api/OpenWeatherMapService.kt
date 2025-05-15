package com.example.weatherapplication.data.api

import com.example.weatherapplication.BuildConfig
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherMapService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pl"
    ): WeatherResponse

    @GET("geo/1.0/direct")
    suspend fun searchCities(
        @Query("q") cityName: String,
        @Query("limit") limit: Int,
        @Query("appid") apiKey: String
    ): List<CitySearchItem>


}

