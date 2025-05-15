package com.example.weatherapplication.ui.screens

// Tutaj będzie Search Bar, do którego wprowadza się nazwę miasta, zatwierdza i wyszukiwuje sie pogoda dla miasta

import androidx.compose.runtime.Composable

@Composable
fun BaseScreen(content: @Composable () -> Unit) {
    content()
}