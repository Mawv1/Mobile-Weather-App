package com.example.weatherapplication.data.api

import com.example.weatherapplication.data.model.CitySearchApiModel
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.ForecastResponse
import com.example.weatherapplication.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherMapService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pl",
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): WeatherResponse

    @GET("geo/1.0/direct")
    suspend fun getCities(
        @Query("q") cityName: String,
        @Query("limit") limit: Int,
        @Query("appid") apiKey: String
    ): List<CitySearchApiModel>

    @GET("data/2.5/weather")
    suspend fun getWeatherByCoordinates(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

//    @GET("data/3.0/onecall")
//    suspend fun getOneCallForecast(
//        @Query("lat") lat: Double,
//        @Query("lon") lon: Double,
//        @Query("exclude") exclude: String = "current,minutely,hourly,alerts",
//        @Query("appid") apiKey: String,
//        @Query("units") units: String = "metric",
//        @Query("lang") lang: String = "pl"
//    ): OneCallResponse

    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pl",
        @Query("exclude") exclude: String = "minutely,hourly,alerts,current"
    ): ForecastResponse
}

