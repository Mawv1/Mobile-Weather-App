package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchScreen(
    viewModel: SearchViewModel,
    onCitySelected: (CityWithWeatherResponse) -> Unit,
    modifier: Modifier = Modifier,
    favoritesContent: @Composable (() -> Unit)? = null
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = { viewModel.onQueryChanged(it) },
                label = { Text("Wpisz nazwę miasta") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            IconButton(onClick = { viewModel.searchCitiesWithWeather() }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Szukaj miasta"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wyniki wyszukiwania
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!errorMessage.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (results.isNotEmpty()) {
            Text("Wyniki wyszukiwania", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp) // ograniczenie wysokości
            ) {
                items(results) { cityWithWeather ->
                    val city = cityWithWeather.city
                    val weather = cityWithWeather.weather
                    val temp = weather?.main?.temp?.let { "${it.toInt()}°" } ?: "--"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCitySelected(cityWithWeather)
                                viewModel.clearSearch()
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listOfNotNull(
                                city.name,
                                city.state?.takeIf { it.isNotBlank() },
                                city.country
                            ).joinToString(", "),
                            modifier = Modifier.weight(1f)
                        )
                        Text(text = temp, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Ulubione miasta – ZAWSZE WIDOCZNE
        if (favoritesContent != null) {
            favoritesContent()
        }
    }
}
