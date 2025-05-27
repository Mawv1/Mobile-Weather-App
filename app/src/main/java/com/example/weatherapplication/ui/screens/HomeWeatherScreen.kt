package com.example.weatherapplication.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    val selectedCity by viewModel.selectedCity.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val favoriteCities by viewModel.favorites.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val units by viewModel.units.collectAsState()

    // Informacja, czy mamy połączenie
    val hasInternet = remember { mutableStateOf(checkInternetConnection(context)) }

    // Request location permission
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(Unit) {
        hasInternet.value = checkInternetConnection(context)
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
                    viewModel.setSelectedCity(cityWithWeather)
                }
            }
        }
    }

    LaunchedEffect(selectedCity?.city?.lat, selectedCity?.city?.lon, units, hasInternet.value) {
        if (hasInternet.value) {
            selectedCity?.let { city ->
                viewModel.getWeatherByCoordinates(city.city.lat, city.city.lon)
            }
        }
    }

    // UI
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
                    text = "${city.city.name}${if (city.city.country.isNotEmpty()) ", ${city.city.country}" else ""}",
                    modifier = Modifier.clickable { onCityClear() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weather display - jeśli offline i wybrano miasto, pokaż zapisane dane bez spinnera
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
                showOfflineWarning = !hasInternet.value
            )
        } else {
            // Jeśli offline i brak danych, pokaż komunikat, nie spinner
            if (hasInternet.value) {
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
            } else {
                Text(
                    text = "Brak danych pogodowych. Sprawdź połączenie internetowe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (!hasInternet.value) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Brak połączenia z internetem. Dane mogą być nieaktualne.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Prosta funkcja sprawdzająca połączenie internetowe (możesz ją przenieść do VM)
fun checkInternetConnection(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
