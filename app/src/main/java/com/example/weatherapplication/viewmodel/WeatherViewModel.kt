package com.example.weatherapplication.viewmodel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository() // lub użyj DI
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState

    fun getWeatherForCity(city: String) {
        viewModelScope.launch {
            try {
                val response = repository.getWeather(city)
                _weatherState.value = response
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Błąd ładowania pogody", e)
            }
        }
    }
}

