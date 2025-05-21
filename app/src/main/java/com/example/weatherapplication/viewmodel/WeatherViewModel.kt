package com.example.weatherapplication.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.BuildConfig
import com.example.weatherapplication.data.local.AppDatabase
import com.example.weatherapplication.data.local.NetworkMonitor
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.repository.FavoritesRepository
import com.example.weatherapplication.data.repository.WeatherRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val favoritesRepo: FavoritesRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    init {
        networkMonitor.start()
    }

    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState

    private val _citySearchResults = MutableLiveData<List<CitySearchItem>>()
    val citySearchResults: LiveData<List<CitySearchItem>> = _citySearchResults

    private val _selectedCity = MutableLiveData<CitySearchItem?>(null)
    val selectedCity: LiveData<CitySearchItem?> = _selectedCity

    private val _forecast = MutableStateFlow<List<DailyForecast>>(emptyList())
    val forecast: StateFlow<List<DailyForecast>> = _forecast

    val favorites: StateFlow<List<CitySearchItem>> =
        favoritesRepo.favoritesFlow.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _showOfflineWarning = MutableStateFlow(false)
    val showOfflineWarning: StateFlow<Boolean> = _showOfflineWarning

    fun getWeatherForCity(city: CitySearchItem) {
        viewModelScope.launch {
            val weather = repository.getWeatherForCity(city)
            val forecastData = repository.getForecastByCoordinates(city.lat, city.lon, 5)
            _weatherState.value = weather
            _forecast.value = forecastData
        }
    }

    fun getWeatherByCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            val weather = repository.getWeatherByCoordinates(lat, lon)
            val forecastData = repository.getForecastByCoordinates(lat, lon, 5)
            _weatherState.value = weather
            _forecast.value = forecastData
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
        loadWeather(city)
    }

    fun loadWeather(city: CitySearchItem) {
        viewModelScope.launch {
            if (isOnline.value) {
                try {
                    val (weather, forecastList) = repository.refreshWeather(city)
                    _weatherState.value = weather
                    _forecast.value = forecastList
                    _showOfflineWarning.value = false
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Błąd odświeżania pogody z API", e)
                }
            } else {
                try {
                    repository.getCachedWeather(city)?.let { (weather, forecastList) ->
                        _weatherState.value = weather
                        _forecast.value = forecastList
                        _showOfflineWarning.value = true
                    } ?: Log.w("WeatherViewModel", "Brak danych w cache dla ${city.name}")
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Błąd wczytywania pogody z cache", e)
                }
            }
        }
    }

    fun addToFavorites(city: CitySearchItem) = viewModelScope.launch {
        favoritesRepo.addFavorite(city)
    }

    fun removeFromFavorites(city: CitySearchItem) = viewModelScope.launch {
        favoritesRepo.removeFavorite(city)
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.stop()
    }
}