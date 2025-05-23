package com.example.weatherapplication.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.example.weatherapplication.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val weatherViewModel: WeatherViewModel
) : ViewModel() {

    private val _units = mutableStateOf(weatherViewModel.getUnits())
    val units: State<String> get() = _units

    private val _refreshInterval = mutableStateOf(30)
    val refreshInterval: State<Int> get() = _refreshInterval

    fun setUnits(newUnits: String) {
        weatherViewModel.setUnits(newUnits) // zapis do SharedPreferences i aktualizacja w WeatherVM
        _units.value = newUnits
        weatherViewModel.refreshWeather()  // odświeżenie pogody po zmianie jednostek
    }

    fun setRefreshInterval(newInterval: Int) {
        _refreshInterval.value = newInterval
        // Tutaj można dodać logikę do WeatherViewModel, jeśli chcesz
    }
}
