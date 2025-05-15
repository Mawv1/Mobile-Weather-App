package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Ustawienia", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Tutaj będą ustawienia użytkownika.")
    }
}
