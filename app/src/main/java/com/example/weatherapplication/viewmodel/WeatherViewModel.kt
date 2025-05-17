package com.example.weatherapplication.viewmodel
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.repository.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository("6997b1033eb7cff386299e6f12777297") // lub użyj DI
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState

    private val _citySearchResults = MutableLiveData<List<CitySearchItem>>()
    val citySearchResults: LiveData<List<CitySearchItem>> = _citySearchResults

    private val _selectedCity = MutableLiveData<CitySearchItem?>()
    val selectedCity: LiveData<CitySearchItem?> = _selectedCity

    fun getWeatherForCity(city: CitySearchItem) {
        viewModelScope.launch {
            try {
                val response = repository.getWeather(city)
                _weatherState.value = response
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Błąd ładowania pogody", e)
            }
        }
    }
    fun searchCity(cityName: String) {
        viewModelScope.launch {
            try {
                Log.d("WeatherViewModel", "Wyszukiwanie miasta: $cityName") // ← to
                val results = repository.searchCitiesByName(cityName)
                _citySearchResults.value = results
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "City search failed: ${e.message}")
            }
        }
    }

    fun selectCity(city: CitySearchItem) {
        _selectedCity.value = city
    }
}

