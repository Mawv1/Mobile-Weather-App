package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.viewmodel.WeatherViewModel

@Composable
fun FavoritesElement(viewModel: WeatherViewModel, onCitySelected: (CityWithWeatherResponse) -> Unit) {
    val favoriteCities by viewModel.favorites.collectAsState()
    val units by viewModel.units.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Ulubione miasta",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (favoriteCities.isEmpty()) {
            Text("Brak ulubionych miast.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favoriteCities) { favorite ->
                    FavoriteCityItem(
                        city = favorite,
                        units = units,
                        onClick = { onCitySelected(favorite) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteCityItem(
    city: CityWithWeatherResponse,
    units: String,
    onClick: () -> Unit
) {
    val temperature = city.weather?.main?.temp
    val temperatureText = temperature?.let {
        val unitSymbol = when (units) {
            "metric" -> "°C"
            "imperial" -> "°F"
            else -> "K"
        }
        "${it.toInt()}$unitSymbol"
    } ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Miasto",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listOfNotNull(city.city.name, city.city.country)
                    .joinToString(", "),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Lat: ${city.city.lat}, Lon: ${city.city.lon}, Temp: $temperatureText",
                style = MaterialTheme.typography.bodySmall
            )
        }

        city.weather?.weather?.firstOrNull()?.icon?.let { iconCode ->
            val iconRes = getWeatherIcon(iconCode)
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "Ikona pogody",
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}
