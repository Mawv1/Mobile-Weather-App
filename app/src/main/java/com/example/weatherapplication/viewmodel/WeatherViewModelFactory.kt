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

    private val weatherViewModel: WeatherViewModel by lazy { // by lazy zapewnia, że ViewModel zostanie utworzony tylko raz
        WeatherViewModel(repo, favoritesRepo, networkMonitor, sharedPreferences)
    }

    private val searchViewModel: SearchViewModel by lazy {
        SearchViewModel(repo, networkMonitor)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WeatherViewModel::class.java) -> weatherViewModel as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(weatherViewModel, networkMonitor) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> searchViewModel as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

