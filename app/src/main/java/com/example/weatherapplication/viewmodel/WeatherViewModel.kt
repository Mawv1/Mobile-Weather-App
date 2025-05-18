package com.example.weatherapplication.viewmodel
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.repository.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository("6997b1033eb7cff386299e6f12777297")
) : ViewModel() {

    // Aktualna pogoda
    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState

    // Wyniki wyszukiwania miasta
    private val _citySearchResults = MutableLiveData<List<CitySearchItem>>()
    val citySearchResults: LiveData<List<CitySearchItem>> = _citySearchResults

    // Wybrane miasto z wyszukiwarki
    private val _selectedCity = MutableLiveData<CitySearchItem?>()
    val selectedCity: LiveData<CitySearchItem?> = _selectedCity

    // Prognoza na kolejne dni
    private val _forecast = MutableStateFlow<List<DailyForecast>>(emptyList())
    val forecast: StateFlow<List<DailyForecast>> = _forecast

    fun getWeatherForCity(city: CitySearchItem) {
        viewModelScope.launch {
            try {
                val response = repository.getWeather(city)
                _weatherState.value = response
                getForecastForCoordinates(city.lat, city.lon) // automatycznie ładuj też prognozę
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Błąd ładowania pogody", e)
            }
        }
    }

    fun searchCity(cityName: String) {
        viewModelScope.launch {
            try {
                Log.d("WeatherViewModel", "Wyszukiwanie miasta: $cityName")
                val results = repository.searchCitiesByName(cityName)
                _citySearchResults.value = results
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "City search failed", e)
            }
        }
    }

    fun selectCity(city: CitySearchItem) {
        _selectedCity.value = city
    }

    fun getWeatherByCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = repository.getWeatherByCoordinates(lat, lon)
                _weatherState.value = response
                getForecastForCoordinates(lat, lon) // automatycznie ładuj też prognozę
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Błąd podczas pobierania pogody po lokalizacji", e)
            }
        }
    }

    fun getForecastForCoordinates(lat: Double, lon: Double, days: Int = 5) {
        viewModelScope.launch {
            try {
                val result = repository.getForecast(lat, lon, days)
                _forecast.value = result
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Błąd pobierania prognozy", e)
            }
        }
    }
}

