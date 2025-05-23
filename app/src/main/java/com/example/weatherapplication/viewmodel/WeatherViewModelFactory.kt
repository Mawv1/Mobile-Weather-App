package com.example.weatherapplication.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapplication.data.local.NetworkMonitor
import com.example.weatherapplication.data.repository.FavoritesRepository
import com.example.weatherapplication.data.repository.WeatherRepository

class WeatherViewModelFactory(
    private val repo: WeatherRepository,
    private val favoritesRepo: FavoritesRepository,
    private val networkMonitor: NetworkMonitor,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val weatherViewModel = WeatherViewModel(repo, favoritesRepo, networkMonitor, sharedPreferences)

        return when {
            modelClass.isAssignableFrom(WeatherViewModel::class.java) -> weatherViewModel as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(weatherViewModel) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

