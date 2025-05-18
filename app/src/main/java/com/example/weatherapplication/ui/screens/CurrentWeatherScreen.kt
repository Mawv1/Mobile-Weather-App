package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.data.model.WeatherResponse
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.ui.ForecastDayCard


@Composable
fun CurrentWeatherScreen(
    city: CitySearchItem,
    navController: NavController,
    viewModel: WeatherViewModel = viewModel()
) {
    LaunchedEffect(city) {
        viewModel.getWeatherForCity(city)
    }

    val weatherState by viewModel.weatherState.collectAsState()
    val forecast by viewModel.forecast.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Aktualna pogoda dla ${city.name}, ${city.country}", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        weatherState?.let { weather ->
            WeatherInfoCard(weather)
        } ?: Text("Brak danych pogodowych.")

        Spacer(modifier = Modifier.height(24.dp))


        Text(
            text = "Prognoza na kolejne dni",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyRow {
            items(forecast) { dayForecast ->
                ForecastDayCard(forecast = dayForecast)
            }
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

@Composable
fun CurrentWeatherScreen(
    lat: Double,
    lon: Double,
    navController: NavController,
    viewModel: WeatherViewModel = viewModel()
) {
    LaunchedEffect(key1 = "$lat$lon") {
        viewModel.getWeatherByCoordinates(lat, lon)
    }

    val weatherState by viewModel.weatherState.collectAsState()
    val forecast by viewModel.forecast.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pogoda dla lokalizacji: $lat, $lon", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        weatherState?.let { weather ->
            WeatherInfoCard(weather)
        } ?: Text("Brak danych pogodowych.")

        Spacer(modifier = Modifier.height(24.dp))

        Text("Prognoza 5-dniowa", style = MaterialTheme.typography.headlineSmall)
        Text("Liczba dni w prognozie: ${forecast.size}")

        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(forecast) { dayForecast ->
                ForecastDayCard(forecast = dayForecast)
            }
        }
    }
}

