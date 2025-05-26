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
import androidx.compose.runtime.livedata.observeAsState
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchScreen(
    viewModel: WeatherViewModel,
    onCitySelected: (CitySearchItem) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.citySearchResults.observeAsState(emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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

        LazyColumn {
            items(results) { cityItem ->
                Text(
                    text = listOfNotNull(cityItem.name, cityItem.state, cityItem.country).joinToString(", "),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onCitySelected(cityItem) }
                )
            }
        }
    }
}
