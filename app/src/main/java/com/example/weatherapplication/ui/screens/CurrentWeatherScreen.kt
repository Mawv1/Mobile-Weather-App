package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.data.model.WeatherResponse
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun CurrentWeatherScreen(
    navController: NavController,
    viewModel: WeatherViewModel = viewModel()
) {
    val weather by viewModel.weatherState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Aktualna pogoda", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (weather != null) {
            WeatherInfoCard(weather!!)
        } else {
            Text("Brak danych pogodowych.")
        }
    }
}



@Composable
fun WeatherInfoCard(weather: WeatherResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Miasto: ${weather.name}", style = MaterialTheme.typography.titleMedium)
            Text("Temperatura: ${weather.main.temp}°C")
            Text("Opis: ${weather.weather.firstOrNull()?.description ?: "Brak"}")
            Text("Wiatr: ${weather.wind.speed} m/s")
        }
    }
}
