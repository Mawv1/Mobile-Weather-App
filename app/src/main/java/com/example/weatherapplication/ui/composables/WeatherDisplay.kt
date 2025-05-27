package com.example.weatherapplication.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
//import com.example.weatherapplication.ui.screens.AdditionalCityWeatherDetails
import com.example.weatherapplication.ui.screens.WeatherInfoCard

@Composable
fun WeatherDisplay(
    weather: WeatherResponse,
    forecast: List<DailyForecast>,
    units: String,
    onFavoriteClick: () -> Unit,
    showOfflineWarning: Boolean,
    isFavorite: Boolean
) {
    Column {
        WeatherInfoCard(weather, units, onFavoriteClick, isFavorite)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Prognoza:", style = MaterialTheme.typography.titleMedium)

        LazyRow {
            items(forecast) { item ->
                ForecastDayCard(forecast = item, units = units)
            }
        }
    }
}
