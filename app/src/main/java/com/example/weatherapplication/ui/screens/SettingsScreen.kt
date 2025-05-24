package com.example.weatherapplication.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onRefreshWeatherClick: () -> Unit,
    selectedCity: CitySearchItem?
) {
    val selectedUnits by settingsViewModel.units
    val refreshInterval by settingsViewModel.refreshInterval

    val unitOptions = listOf("metric", "imperial", "standard")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Jednostki miary", style = MaterialTheme.typography.titleMedium)
            unitOptions.forEach { unit ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            settingsViewModel.setUnits(unit)
                            onRefreshWeatherClick()
                        }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = unit == selectedUnits,
                        onClick = {
                            settingsViewModel.setUnits(unit)
                            onRefreshWeatherClick()
                        }
                    )
                    Text(text = if (unit == "metric") "Celsjusza (°C)" else if (unit=="imperial") "Fahrenheita (°F)" else "Kelwina (K)")
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Interwał odświeżania (minuty)", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = refreshInterval.toFloat(),
                    onValueChange = { settingsViewModel.setRefreshInterval(it.toInt()) },
                    valueRange = 1f..60f,
                    steps = 59
                )
                Text("Wybrany interwał: $refreshInterval minut")
            }

            Button(
                onClick = onRefreshWeatherClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Odśwież pogodę")
            }
        }
    }
}
