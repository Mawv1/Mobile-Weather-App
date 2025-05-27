package com.example.weatherapplication.data.model

import com.squareup.moshi.JsonClass
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    val list: List<ForecastItem>,
    val city: City
) {
    fun toDailyForecastList(): List<DailyForecast> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val zone = ZoneId.systemDefault()

        // Grupujemy prognozy po dacie (yyyy-MM-dd)
        return list.groupBy { forecastItem ->
            val instant = Instant.ofEpochSecond(forecastItem.dt)
            val localDate = instant.atZone(zone).toLocalDate()
            localDate.format(formatter)
        }.map { (date, items) ->
            // Obliczamy średnią temperaturę, min i max temperaturę oraz ikonę pogody dla danego dnia
            val avgTemp = items.map { it.main.temp }.average()
            val minTemp = items.minOf { it.main.temp_min }
            val maxTemp = items.maxOf { it.main.temp_max }
            val weatherIcon = items.firstOrNull()?.weather?.firstOrNull()?.icon ?: ""

            DailyForecast(
                date = date,
                temperature = avgTemp,
                minTemperature = minTemp,
                maxTemperature = maxTemp,
                weatherIconCode = weatherIcon
            )
        }.sortedBy { it.date } // sortowanie wg daty rosnąco
    }
}

@JsonClass(generateAdapter = true)
data class ForecastItem(
    val dt: Long,  // timestamp w sekundach
    val main: MainData,
    val weather: List<WeatherDescription>
)

@JsonClass(generateAdapter = true)
data class MainData(
    val temp: Double,
    val temp_min: Double,
    val temp_max: Double
)

@JsonClass(generateAdapter = true)
data class WeatherDescription(
    val icon: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
    val date: String,          // np. "2025-05-22"
    val temperature: Double,   // średnia temperatura dnia lub temp w konkretnym czasie
    val minTemperature: Double, // minimalna temperatura dnia
    val maxTemperature: Double, // maksymalna temperatura dnia
    val weatherIconCode: String
)

@JsonClass(generateAdapter = true)
data class City(
    val name: String,
    val country: String
)
