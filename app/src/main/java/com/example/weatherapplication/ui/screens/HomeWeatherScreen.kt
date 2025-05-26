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
    ) { granted ->
        permissionGranted = granted
    }

    var selectedCity by remember { mutableStateOf<CitySearchItem?>(null) }
    var locationCity by remember { mutableStateOf<CitySearchItem?>(null) }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Ustaw lokalizację tylko jeśli selectedCity jest null (czyli nie wybrano miasta z ulubionych/wyszukiwania)
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
                    locationCity = city
                    selectedCity = city
                    viewModel.setSelectedCity(city)
                    viewModel.getWeatherByCoordinates(city.lat, city.lon)
                }
            }
        }
    }

    // Kiedy selectedCity się zmienia, ładuj pogodę dla tego miasta
    LaunchedEffect(selectedCity) {
        selectedCity?.let { city ->
            viewModel.setSelectedCity(city)
            viewModel.getWeatherByCoordinates(city.lat, city.lon)
        }
    }

    val weatherState by viewModel.weatherState.collectAsState()
    val favoriteCities by viewModel.favorites.collectAsState(emptyList())
    val forecast by viewModel.forecast.collectAsState()
    val units by viewModel.units.collectAsState()

    Column(
        modifier = modifier
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
                text = "Aktualne miasto:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            selectedCity?.let { city ->
                Text(
                    text = "${city.name}, ${city.country}",
                    modifier = Modifier.clickable {
                        onCityClear()
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                },
                isFavorite = favoriteCities.any { fav ->
                    fav.lat == weather.coord.lat && fav.lon == weather.coord.lon
                },
                showOfflineWarning = false
            )
        } ?: Text("Ładuję aktualną pogodę…")

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
                        selectedCity = city
                    }
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
