package com.example.weatherapplication.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.weatherapplication.viewmodel.WeatherViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ForecastScreen(city: String, navController: NavController, viewModel: WeatherViewModel = viewModel()) {
    Text(text = "Prognoza pogody")
}