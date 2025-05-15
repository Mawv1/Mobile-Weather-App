package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DetailsScreen(
    city: String,
    navController: NavController,
    viewModel: WeatherViewModel = viewModel()
) {
    Text(
        text = "Szczegóły pogody dla: $city",
        modifier = Modifier.padding(16.dp)
    )
}
