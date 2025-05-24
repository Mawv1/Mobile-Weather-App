package com.example.weatherapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.data.model.WeatherResponse
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherapplication.R
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.ui.ForecastDayCard
import com.example.weatherapplication.ui.composables.WeatherDisplay

@Composable
fun WeatherInfoCard(
    weather: WeatherResponse,
    units: String,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean
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
            Text("Temperatura: ${weather.main.temp} ${if (units == "metric") "°C" else if (units=="imperial") "°F" else "K"}")
            Text("Opis: ${weather.weather.firstOrNull()?.description ?: "Brak"}")
            Text("Wiatr: ${weather.wind.speed} ${if (units == "metric") "m/s" else "mph"}")

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ DODATKOWE INFORMACJE
            Text("Dodatkowe informacje:", style = MaterialTheme.typography.titleMedium)
            Text("Ciśnienie: ${weather.main.pressure} hPa")
            Text("Wilgotność: ${weather.main.humidity}%")
            Text("Zachmurzenie: ${weather.clouds.all}%")
            Text("Widoczność: ${weather.visibility / 1000.0} km")
            Text("Wschód słońca: ${formatUnixTime(weather.sys.sunrise)}")
            Text("Zachód słońca: ${formatUnixTime(weather.sys.sunset)}")

            Spacer(modifier = Modifier.height(8.dp))
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
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
    navController: NavController,
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = lat, key2 = lon) {
        Log.d("CurrentWeatherScreen", "Wywołanie getWeatherByCoordinates dla $lat, $lon")
        viewModel.getWeatherByCoordinates(lat, lon)
    }

    val weatherState by viewModel.weatherState.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val units by viewModel.units.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val showOfflineWarning by viewModel.showOfflineWarning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 64.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showOfflineWarning) {
            Text(
                text = "Brak połączenia z internetem. Dane mogą być nieaktualne.",
                color = Color.Red,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            weatherState?.let { weather ->
                val cityItem = CitySearchItem(
                    name = weather.name,
                    country = weather.sys.country,
                    state = null,
                    lat = weather.coord.lat,
                    lon = weather.coord.lon
                )

                val isFavorite = favorites.any { fav ->
                    fav.name == cityItem.name &&
                            fav.lat == cityItem.lat &&
                            fav.lon == cityItem.lon
                }

                WeatherDisplay(
                    weather = weather,
                    forecast = forecast,
                    units = units,
                    onFavoriteClick = {
                        if (isFavorite) {
                            viewModel.removeFromFavorites(cityItem)
                        } else {
                            viewModel.addToFavorites(cityItem)
                        }
                    },
                    showOfflineWarning = showOfflineWarning,
                    isFavorite = isFavorite
                )

                Spacer(modifier = Modifier.height(16.dp))
            } ?: Text("Brak danych pogodowych.")
        }
    }
}


// Pomocnicza funkcja do formatowania czasu UNIX
fun formatUnixTime(unixTime: Long): String {
    val date = java.util.Date(unixTime * 1000)
    val format = java.text.SimpleDateFormat("HH:mm")
    format.timeZone = java.util.TimeZone.getDefault()
    return format.format(date)
}
