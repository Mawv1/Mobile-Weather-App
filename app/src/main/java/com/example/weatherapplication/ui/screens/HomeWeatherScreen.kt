package com.example.weatherapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.ui.composables.WeatherDisplay
import com.example.weatherapplication.viewmodel.WeatherViewModel
//import com.example.weatherapplication.utils.formatUnixTime
import com.google.android.gms.location.LocationServices


@Composable
fun HomeWeatherScreen(
    navController: NavController,
    viewModel: WeatherViewModel
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
//        viewModel.clearFavorites()
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val city = CitySearchItem(
                            name = "Moja lokalizacja",
                            country = "",
                            state = null,
                            lat = it.latitude,
                            lon = it.longitude
                        )
                        Log.d("HomeWeatherScreen", "Ustawiam selectedCity na ${city.lat}, ${city.lon}")
                        viewModel.setSelectedCity(city)
                        viewModel.getWeatherByCoordinates(city.lat, city.lon)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("HomeWeatherScreen", "Brak uprawnienia do lokalizacji", e)
            }
        }
    }

    val weatherState by viewModel.weatherState.collectAsState()
    val favoriteCities by viewModel.favorites.collectAsState(emptyList())
    val forecast by viewModel.forecast.collectAsState()
    val units by viewModel.units.collectAsState()
    val selectedFavoriteWeather by viewModel.selectedFavoriteWeather.collectAsState()

    LaunchedEffect(units) {
        weatherState?.let { weather ->
            viewModel.getWeatherByCoordinates(weather.coord.lat, weather.coord.lon)
        }
    }

    var selectedFavoriteCity by remember { mutableStateOf<CitySearchItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktualna lokalizacja:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        weatherState?.let { weather ->
            Column {
                WeatherDisplay(
                    weather = weather,
                    forecast = forecast,
                    units = units,
                    onFavoriteClick = {
                        val city = weather.toCitySearchItem()
                        if (favoriteCities.any { fav -> fav.lat == city.lat && fav.lon == city.lon }) {
                            viewModel.removeFromFavorites(city)
                        } else {
                            viewModel.addToFavorites(city)
                        }
                    },
                    isFavorite = favoriteCities.any { fav ->
                        fav.lat == weather.coord.lat && fav.lon == weather.coord.lon
                    },
                    showOfflineWarning = false // albo: !weather.isFromApi (jeśli masz taką właściwość)
                )


                Spacer(modifier = Modifier.height(8.dp))

//                Text("Dodatkowe informacje:", style = MaterialTheme.typography.titleMedium)
//                AdditionalCityWeatherDetails(weather = weather, units = units)
            }
        } ?: Text(
            text = "Ładuję aktualną pogodę…",
            color = MaterialTheme.colorScheme.onBackground
        )


        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Ulubione miasta", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (favoriteCities.isEmpty()) {
            Text("Brak ulubionych miast.")
        } else {
            favoriteCities.forEach { city ->
                FavoriteCityItem(
                    city = city,
                    onClick = {
                        selectedFavoriteCity = city
                        viewModel.getWeatherForSelectedFavorite(city)
                        navController.navigate("current/${city.lat}/${city.lon}") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

//        selectedFavoriteCity?.let { city ->
//            Text("Dane dodatkowe dla: ${city.name}", style = MaterialTheme.typography.titleMedium)
//            selectedFavoriteWeather?.let { weather ->
//                AdditionalCityWeatherDetails(weather = weather, units = units)
//            } ?: Text("Ładuję dane pogodowe…")
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))

    }
}

@Composable
fun FavoriteCityItem(
    city: CitySearchItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Miasto",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = "${city.name}, ${city.country}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Lat: ${city.lat}, Lon: ${city.lon}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AdditionalCityWeatherDetails(
    weather: WeatherResponse,
    units: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Temperatura: ${weather.main.temp} ${if (units == "metric") "°C" else "°F"}", style = MaterialTheme.typography.bodyMedium)
        Text("Ciśnienie: ${weather.main.pressure} hPa", style = MaterialTheme.typography.bodyMedium)
        Text("Wilgotność: ${weather.main.humidity}%", style = MaterialTheme.typography.bodyMedium)
        Text("Zachmurzenie: ${weather.clouds.all}%", style = MaterialTheme.typography.bodyMedium)
        Text("Widoczność: ${weather.visibility / 1000.0} km", style = MaterialTheme.typography.bodyMedium)
        Text("Wschód słońca: ${formatUnixTime(weather.sys.sunrise)}", style = MaterialTheme.typography.bodyMedium)
        Text("Zachód słońca: ${formatUnixTime(weather.sys.sunset)}", style = MaterialTheme.typography.bodyMedium)
    }
}
