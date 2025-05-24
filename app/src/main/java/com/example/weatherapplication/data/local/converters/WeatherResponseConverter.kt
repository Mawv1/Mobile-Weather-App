package com.example.weatherapplication.data.local.converters

import androidx.room.TypeConverter
import com.example.weatherapplication.data.model.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class WeatherResponseConverter {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(WeatherResponse::class.java)

    @TypeConverter
    fun fromWeatherResponse(weatherResponse: WeatherResponse?): String? {
        return weatherResponse?.let { adapter.toJson(it) }
    }

    @TypeConverter
    fun toWeatherResponse(json: String?): WeatherResponse? {
        return json?.let { adapter.fromJson(it) }
    }
}
