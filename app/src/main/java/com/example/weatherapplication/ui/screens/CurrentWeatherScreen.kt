package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.data.model.WeatherResponse
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherapplication.R
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
            WeatherInfoCard(
                weather = weather,
                onFavoriteClick = {
                    viewModel.addToFavorites(
                        CitySearchItem(
                            name = weather.name,
                            country = weather.sys.country,
                            state = null, // jeśli nie masz danych
                            lat = weather.coord.lat,
                            lon = weather.coord.lon
                        )
                    )
                }
            )

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
fun WeatherInfoCard(
    weather: WeatherResponse,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = getWeatherIcon(weather.weather.firstOrNull()?.icon)),
                    contentDescription = "Ikona pogody",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Miasto: ${weather.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text("Temperatura: ${weather.main.temp}°C", color = MaterialTheme.colorScheme.onBackground)
            Text("Opis: ${weather.weather.firstOrNull()?.description ?: "Brak"}", color = MaterialTheme.colorScheme.onBackground)
            Text("Wiatr: ${weather.wind.speed} m/s", color = MaterialTheme.colorScheme.onBackground)

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Zapisz jako ulubione",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun getWeatherIcon(iconCode: String?): Int {
    return when (iconCode) {
        "01d" -> R.drawable.ic_01d // Słonecznie
        "01n" -> R.drawable.ic_01n
        "02d" -> R.drawable.ic_02d // Częściowe zachmurzenie
        "02n" -> R.drawable.ic_02n
        "03d" -> R.drawable.ic_03d // Zachmurzenie
        "03n" -> R.drawable.ic_03n
        "04d" -> R.drawable.ic_04d // Zachmurzenie mocniejsze
        "04n" -> R.drawable.ic_04n
        "09d" -> R.drawable.ic_09d // Deszcz
        "09n" -> R.drawable.ic_09n
        "10d" -> R.drawable.ic_10d // Deszcz
        "10n" -> R.drawable.ic_10n
        "11d" -> R.drawable.ic_11d // Burza
        "11n" -> R.drawable.ic_11n
        "13d" -> R.drawable.ic_13d // Śnieg
        "13n" -> R.drawable.ic_13n
        "50d" -> R.drawable.ic_50d // Mgła
        "50n" -> R.drawable.ic_50n
        // jakas standardowa ikona domyslna, nie mam takiej w drawable, uzyj czegos gotowego
        else -> R.drawable.ic_03d
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
            .padding(start = 16.dp, top = 64.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pogoda dla lokalizacji: $lat, $lon", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        weatherState?.let { weather ->
            WeatherInfoCard(
                weather = weather,
                onFavoriteClick = {
                    viewModel.addToFavorites(
                        CitySearchItem(
                            name = weather.name,
                            country = weather.sys.country,
                            state = null,
                            lat = weather.coord.lat,
                            lon = weather.coord.lon
                        )
                    )
                }
            )
        } ?: Text("Brak danych pogodowych.")

        Spacer(modifier = Modifier.height(24.dp))

        Text("Prognoza 5-dniowa", style = MaterialTheme.typography.headlineSmall)
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(forecast) { dayForecast ->
                ForecastDayCard(forecast = dayForecast)
            }
        }
    }
}

