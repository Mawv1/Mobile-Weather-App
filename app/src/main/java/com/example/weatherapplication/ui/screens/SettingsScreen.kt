package com.example.weatherapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onRefreshWeatherClick: () -> Unit,
    selectedCity: CityWithWeatherResponse?,
    modifier: Modifier = Modifier
) {
    val selectedUnits by settingsViewModel.units.collectAsState()
    val refreshInterval by settingsViewModel.refreshInterval.collectAsState()

    val unitOptions = listOf("metric", "imperial", "standard")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Jednostki miary", style = MaterialTheme.typography.titleMedium)

        unitOptions.forEach { unit ->
            UnitOptionRow(
                unit = unit,
                isSelected = unit == selectedUnits,
                onUnitSelected = {
                    settingsViewModel.setUnits(it)
                    onRefreshWeatherClick()
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Interwał odświeżania (minuty)", style = MaterialTheme.typography.titleMedium)

        Slider(
            value = refreshInterval.toFloat(),
            onValueChange = { settingsViewModel.setRefreshInterval(it.toInt()) },
            valueRange = 1f..60f,
            steps = 59
        )
        Text("Wybrany interwał: $refreshInterval minut")

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            settingsViewModel.refreshWeather()
            onRefreshWeatherClick()
        }) {
            Text("Odśwież pogodę")
        }
    }
}

@Composable
fun UnitOptionRow(
    unit: String,
    isSelected: Boolean,
    onUnitSelected: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Log.d("SettingsScreen", "Unit selected: $unit")
                onUnitSelected(unit)
            }
            .padding(vertical = 8.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = {
                Log.d("SettingsScreen", "RadioButton clicked: $unit")
                onUnitSelected(unit)
            },
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text = getUnitLabel(unit))
    }
}

private fun getUnitLabel(unit: String): String {
    return when (unit) {
        "metric" -> "Celsjusza (°C)"
        "imperial" -> "Fahrenheita (°F)"
        "standard" -> "Kelwina (K)"
        else -> unit
    }
}