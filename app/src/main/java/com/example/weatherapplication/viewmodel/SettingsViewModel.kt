package com.example.weatherapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.weatherapplication.data.local.NetworkMonitor

class SettingsViewModel(
    private val weatherViewModel: WeatherViewModel,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _units = MutableStateFlow(weatherViewModel.getUnits())
    val units: StateFlow<String> = _units

    private val _refreshInterval = MutableStateFlow(weatherViewModel.getRefreshInterval())
    val refreshInterval: StateFlow<Int> = _refreshInterval

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun setUnits(newUnits: String) {
        if (_units.value != newUnits) {
            if (!networkMonitor.isOnline.value) {
                _errorMessage.value = "Brak połączenia – nie można zmienić jednostek"
                return
            }
            weatherViewModel.setUnits(newUnits)
            _units.value = newUnits
            viewModelScope.launch {
                weatherViewModel.refreshWeather()
            }
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
        if (!networkMonitor.isOnline.value) {
            _errorMessage.value = "Brak połączenia – nie można odświeżyć pogody"
            return
        }
        weatherViewModel.refreshAllCitiesWeather()
    }
}
