package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.ui.ForecastDayCard

@Composable
fun ForecastScreen(
    forecasts: List<DailyForecast>,
    units: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Prognoza pogody", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        forecasts.forEach { forecast ->
            ForecastDayCard(forecast = forecast, units = units)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}