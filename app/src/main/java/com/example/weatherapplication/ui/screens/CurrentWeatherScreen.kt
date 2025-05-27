package com.example.weatherapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.ui.composables.WeatherDisplay
import com.example.weatherapplication.R
import com.example.weatherapplication.data.model.CityWithWeatherResponse

@Composable
fun WeatherInfoCard(
    weather: WeatherResponse,
    units: String,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean,
    modifier: Modifier = Modifier
) {
    Log.d("WeatherInfoCard", "units = $units")
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = "Miasto: ${weather.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Temperatura: ${weather.main.temp} ${
                    when (units) {
                        "metric" -> "°C"
                        "imperial" -> "°F"
                        else -> "K"
                    }
                }"
            )
            Text(text = "Opis: ${weather.weather.firstOrNull()?.description ?: "Brak"}")
            Text(
                text = "Wiatr: ${weather.wind.speed} ${
                    if (units == "metric") "m/s" else "mph"
                }"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Dodatkowe informacje:", style = MaterialTheme.typography.titleMedium)
            Text(text = "Ciśnienie: ${weather.main.pressure} hPa")
            Text(text = "Wilgotność: ${weather.main.humidity}%")
            Text(text = "Zachmurzenie: ${weather.clouds.all}%")
            Text(text = "Widoczność: ${weather.visibility / 1000.0} km")
            Text(text = "Wschód słońca: ${formatUnixTime(weather.sys.sunrise.toLong())}")
            Text(text = "Zachód słońca: ${formatUnixTime(weather.sys.sunset.toLong())}")

            Spacer(modifier = Modifier.height(8.dp))
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                    tint = if (isFavorite) Color.Yellow else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun getWeatherIcon(iconCode: String?): Int {
    return when (iconCode) {
        "01d" -> R.drawable.ic_01d
        "01n" -> R.drawable.ic_01n
        "02d" -> R.drawable.ic_02d
        "02n" -> R.drawable.ic_02n
        "03d" -> R.drawable.ic_03d
        "03n" -> R.drawable.ic_03n
        "04d" -> R.drawable.ic_04d
        "04n" -> R.drawable.ic_04n
        "09d" -> R.drawable.ic_09d
        "09n" -> R.drawable.ic_09n
        "10d" -> R.drawable.ic_10d
        "10n" -> R.drawable.ic_10n
        "11d" -> R.drawable.ic_11d
        "11n" -> R.drawable.ic_11n
        "13d" -> R.drawable.ic_13d
        "13n" -> R.drawable.ic_13n
        "50d" -> R.drawable.ic_50d
        "50n" -> R.drawable.ic_50n
        else -> R.drawable.ic_04d // Domyślna ikona
    }
}

@Composable
fun CurrentWeatherScreen(
    lat: Double,
    lon: Double,
    navController: NavController? = null,
    viewModel: WeatherViewModel
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val units by viewModel.units.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val showOfflineWarning by viewModel.showOfflineWarning.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState(initial = false)

    LaunchedEffect(lat, lon, units) {
        viewModel.getWeatherByCoordinates(lat, lon)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showOfflineWarning) {
            Text(
                text = "Brak połączenia z internetem. Dane mogą być nieaktualne.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        if (isLoading) {
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator()
        } else {
            weatherState?.let { weather ->
                val cityItem = CitySearchItem(
                    name = weather.name,
                    country = weather.sys.country,
                    state = null,
                    lat = weather.coord.lat,
                    lon = weather.coord.lon
                )

                val cityWithWeather = CityWithWeatherResponse(
                    city = cityItem,
                    weather = weather
                )

                val isFavorite = favorites.any { fav ->
                    fav.city.name == cityItem.name &&
                            fav.city.lat == cityItem.lat &&
                            fav.city.lon == cityItem.lon
                }

                WeatherDisplay(
                    weather = weather,
                    forecast = forecast,
                    units = units,
                    isFavorite = isFavorite,
                    onFavoriteClick = {
                        if (isFavorite) {
                            viewModel.removeFromFavorites(cityWithWeather)
                        } else {
                            viewModel.addToFavorites(cityWithWeather)
                        }
                    },
                    showOfflineWarning = showOfflineWarning
                )
            } ?: Text("Brak danych pogodowych.")
        }
    }
}

// Pomocnicza funkcja do formatowania czasu UNIX
fun formatUnixTime(unixTime: Long): String {
    val date = java.util.Date(unixTime * 1000)
    val format = java.text.SimpleDateFormat("HH:mm").apply {
        timeZone = java.util.TimeZone.getDefault()
    }
    return format.format(date)
}
