package com.example.weatherapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.weatherapplication.BuildConfig
import com.example.weatherapplication.data.api.OpenWeatherMapService
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.viewmodel.WeatherViewModelFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherScreen(
    navController: NavController,
    viewModel: WeatherViewModel = viewModel()
) {
    val cityList = listOf("Łódź", "Warszawa", "Kraków", "Gdańsk", "Wrocław", "Poznań")
    val weatherState by viewModel.weatherState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Wybierz miasto:", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        cityList.forEach { city ->
            Button(
                onClick = { viewModel.getWeatherForCity(city) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(city)
            }
        }

        Spacer(Modifier.height(16.dp))

        weatherState?.let { weather ->
            WeatherInfoCard(weather)
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
