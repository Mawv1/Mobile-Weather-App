package com.example.weatherapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapplication.data.local.NetworkMonitor
import com.example.weatherapplication.data.repository.FavoritesRepository
import com.example.weatherapplication.data.repository.WeatherRepository

class WeatherViewModelFactory(
    private val repo: WeatherRepository,
    private val favoritesRepo: FavoritesRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(repo, favoritesRepo, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
