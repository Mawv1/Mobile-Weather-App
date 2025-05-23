package com.example.weatherapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.weatherapplication.ui.composables.WeatherDisplay
import com.example.weatherapplication.viewmodel.WeatherViewModel
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

    // Request permission on first composition if not granted
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Load weather once permission is granted
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

    // Collect state from ViewModel
    val weatherState by viewModel.weatherState.collectAsState()
    val favoriteCities by viewModel.favorites.collectAsState(emptyList())
    val forecast by viewModel.forecast.collectAsState()
    val units by viewModel.units.collectAsState()

    // Refresh weather when units change
    LaunchedEffect(units) {
        weatherState?.let { weather ->
            viewModel.getWeatherByCoordinates(weather.coord.lat, weather.coord.lon)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ustawienia",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        weatherState?.let { weather ->
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
                }
            )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favoriteCities) { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("current/${city.lat}/${city.lon}") {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Miasto",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${city.name}, ${city.country}",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.navigate("search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Wyszukaj miasto", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
