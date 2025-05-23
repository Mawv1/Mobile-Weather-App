package com.example.weatherapplication.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.weatherapplication.data.model.CityWeatherData
import com.example.weatherapplication.ui.screens.getWeatherIcon

@Composable
fun FavoriteCityCard(cityWeather: CityWeatherData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikona pogody, zamień na odpowiednie źródło ikon
            Icon(
                painter = painterResource(id = getWeatherIcon(cityWeather.weatherIconCode)),
                contentDescription = "Ikona pogody",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = cityWeather.city.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Współrzędne: ${cityWeather.city.lat}, ${cityWeather.city.lon}")
                Text(text = "Czas lokalny: ${cityWeather.localTime}")
                Text(text = "Temperatura: ${cityWeather.temperature} °C")
                Text(text = "Ciśnienie: ${cityWeather.pressure} hPa")
                Text(text = cityWeather.weatherDescription)
            }
        }
    }
}
