package com.example.weatherapplication.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.ui.screens.BaseScreen
import com.example.weatherapplication.ui.screens.WeatherInfoCard
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWeatherScreen(
    navController: NavController,
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val weatherState by viewModel.weatherState.collectAsState()
    val searchResults by viewModel.citySearchResults.observeAsState(emptyList())
    var query by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Pobierz lokalizację
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                location?.let {
                    viewModel.getWeatherByCoordinates(it.latitude, it.longitude)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        when {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Już masz zgodę, pobierz lokalizację
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    location?.let {
                        viewModel.getWeatherByCoordinates(it.latitude, it.longitude)
                    }
                }
            }

            else -> {
                // Poproś użytkownika o zgodę
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    BaseScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Twoja lokalizacja:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            weatherState?.let { weather ->
                WeatherInfoCard(weather)
            } ?: Text("Pobieranie pogody...")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // Pole wyszukiwania
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Wpisz nazwę miasta") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            IconButton(
                onClick = {
                    viewModel.searchCity(query)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Szukaj miasta"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista wyników wyszukiwania
        LazyColumn {
            items(searchResults) { cityItem ->
                Text(
                    text = "${cityItem.name}, ${cityItem.state}, ${cityItem.country}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate("current/${cityItem.lat}/${cityItem.lon}")
                        }
                )
            }
        }
    }
}

