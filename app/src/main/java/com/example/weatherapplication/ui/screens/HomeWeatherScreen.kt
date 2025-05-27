package com.example.weatherapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.ui.composables.WeatherDisplay
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices

@Composable
fun HomeWeatherScreen(
    navController: NavController?,
    viewModel: WeatherViewModel,
    onCityClear: () -> Unit,
    modifier: Modifier = Modifier
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
    ) { granted -> permissionGranted = granted }

    val selectedCity by viewModel.selectedCity.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val favoriteCities by viewModel.favorites.collectAsState(emptyList())
    val forecast by viewModel.forecast.collectAsState()
    val units by viewModel.units.collectAsState()

    // Request location permission
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Get location if granted and no city selected
    LaunchedEffect(permissionGranted) {
        if (permissionGranted && selectedCity == null) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val city = CitySearchItem(
                        name = "Moja lokalizacja",
                        country = "",
                        state = null,
                        lat = it.latitude,
                        lon = it.longitude
                    )
                    val cityWithWeather = CityWithWeatherResponse(
                        city = city,
                        weather = null,
                        forecast = emptyList()
                    )
                    viewModel.setSelectedCity(cityWithWeather, context)
                }
            }
        }
    }

    // Fetch weather on city or unit change
    LaunchedEffect(selectedCity?.city?.lat, selectedCity?.city?.lon, units) {
        selectedCity?.let { city ->
            viewModel.getWeatherByCoordinates(city.city.lat, city.city.lon)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Selected city
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktualne miasto:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            selectedCity?.let { city ->
                Text(
                    text = "${city.city.name}${if (city.city.country.isNotEmpty()) ", ${city.city.country}" else ""}",
                    modifier = Modifier.clickable { onCityClear() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weather display
        if (weatherState != null) {
            WeatherDisplay(
                weather = weatherState!!,
                forecast = forecast,
                units = units,
                onFavoriteClick = {
                    val city = weatherState!!.toCitySearchItem()
                    val cityWithWeather = CityWithWeatherResponse(
                        city = city,
                        weather = weatherState,
                        forecast = forecast
                    )
                    if (favoriteCities.any { fav -> fav.city.lat == city.lat && fav.city.lon == city.lon }) {
                        viewModel.removeFromFavorites(cityWithWeather)
                    } else {
                        viewModel.addToFavorites(cityWithWeather)
                    }
                },
                isFavorite = favoriteCities.any {
                    it.city.lat == weatherState!!.coord.lat && it.city.lon == weatherState!!.coord.lon
                },
                showOfflineWarning = false
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ładuję aktualną pogodę…",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Favorites
        Text(text = "Ulubione miasta", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (favoriteCities.isEmpty()) {
            Text("Brak ulubionych miast.")
        } else {
            favoriteCities.forEach { favorite ->
                FavoriteCityItem(
                    city = favorite.city,
                    onClick = { viewModel.setSelectedCity(favorite, context) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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