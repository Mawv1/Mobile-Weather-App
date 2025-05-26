package com.example.weatherapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class SettingsViewModel(
    private val weatherViewModel: WeatherViewModel
) : ViewModel() {

    private val _units = MutableStateFlow(weatherViewModel.getUnits())
    val units: StateFlow<String> = _units

    private val _refreshInterval = MutableStateFlow(30)
    val refreshInterval: StateFlow<Int> = _refreshInterval

    fun setUnits(newUnits: String) {
        if (_units.value != newUnits) {
            Log.d("SettingsViewModel", "Changing units to $newUnits")
            weatherViewModel.setUnits(newUnits)
            _units.value = newUnits
        }
    }

    fun setRefreshInterval(newInterval: Int) {
        if (newInterval > 0) {
            Log.d("SettingsViewModel", "Changing refresh interval to $newInterval")
            weatherViewModel.setRefreshInterval(newInterval)
            _refreshInterval.value = newInterval
        }
    }

    fun refreshWeather() {
        Log.d("SettingsViewModel", "Manual weather refresh requested")
        weatherViewModel.refreshAllCitiesWeather()
    }
}
