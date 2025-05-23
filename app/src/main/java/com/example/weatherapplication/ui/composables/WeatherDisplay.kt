package com.example.weatherapplication.ui.composables

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.ui.ForecastDayCard
import com.example.weatherapplication.ui.screens.WeatherInfoCard

@Composable
fun WeatherDisplay(
    weather: WeatherResponse,
    forecast: List<DailyForecast>,
    units: String,
    onFavoriteClick: () -> Unit,
    showOfflineWarning: Boolean = false
) {
    Column {
        if (showOfflineWarning) {
            Text(
                "Brak połączenia z internetem. Dane mogą być nieaktualne.",
                color = Color.Red,
                modifier = Modifier.padding(8.dp)
            )
        }
        Log.e("WeatherDisplay", "Units: $units")
//        Log.e("WeatherDisplay", "Current units: ${units}, ViewModel instance: ${weatherViewModel.hashCode()}")
        WeatherInfoCard(weather = weather, units = units, onFavoriteClick = onFavoriteClick)

        Spacer(Modifier.height(24.dp))

        Text("Prognoza 5-dniowa", style = MaterialTheme.typography.headlineSmall)
        if (forecast.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(forecast) { dayForecast ->
                    ForecastDayCard(forecast = dayForecast, units = units)
                }
            }
        } else {
            Text(
                "Brak dostępnych danych prognozy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}