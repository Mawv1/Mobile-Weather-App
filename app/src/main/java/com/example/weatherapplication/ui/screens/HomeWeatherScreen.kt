package com.example.weatherapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.weatherapplication.ui.ForecastDayCard
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.ui.screens.BaseScreen
import com.example.weatherapplication.ui.screens.WeatherInfoCard
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


@Composable
fun HomeWeatherScreen(
    navController: NavController,
    viewModel: WeatherViewModel
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            fetchLocation(fusedLocationClient, viewModel)
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fetchLocation(fusedLocationClient, viewModel)
        }
    }

    val weatherState by viewModel.weatherState.collectAsState()
    val favoriteCities by viewModel.favorites.collectAsState(emptyList())
    val forecast by viewModel.forecast.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Nagłówek z przyciskiem ustawień
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Aktualna lokalizacja:",
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

        Spacer(Modifier.height(8.dp))

        // Aktualna pogoda
        weatherState?.let {
            WeatherInfoCard(
                weather = it,
                onFavoriteClick = {
                    val city = it.toCitySearchItem()
                    if (favoriteCities.any { fav -> fav.lat == city.lat && fav.lon == city.lon }) {
                        viewModel.removeFromFavorites(city)
                    } else {
                        viewModel.addToFavorites(city)
                    }
                }
            )
        } ?: Text("Ładuję aktualną pogodę…", color = MaterialTheme.colorScheme.onBackground)

        Spacer(Modifier.height(24.dp))

        // Prognoza 5-dniowa
        Text("Prognoza 5-dniowa", style = MaterialTheme.typography.headlineSmall)
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(forecast) { dayForecast ->
                ForecastDayCard(forecast = dayForecast)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Ulubione miasta
        Text("Ulubione miasta", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .verticalScroll(rememberScrollState())
                .padding(4.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column {
                if (favoriteCities.isEmpty()) {
                    Text("Brak ulubionych miast.")
                } else {
                    favoriteCities.forEach { city ->
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
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${city.name}, ${city.country}",
                                modifier = Modifier.wrapContentWidth(),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Przycisk wyszukiwania
        Button(
            onClick = { navController.navigate("search") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Wyszukaj miasto", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

// Wyciągnięta funkcja, żeby kod composable był czytelniejszy
private fun fetchLocation(
    client: FusedLocationProviderClient,
    viewModel: WeatherViewModel
) {
    try {
        client.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    viewModel.getWeatherByCoordinates(it.latitude, it.longitude)
                }
            }
    } catch (e: SecurityException) {
        Log.e("HomeWeatherScreen", "Brak uprawnienia do lokalizacji", e)
    }
}
