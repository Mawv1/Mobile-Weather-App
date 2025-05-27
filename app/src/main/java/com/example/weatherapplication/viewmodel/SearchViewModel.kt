package com.example.weatherapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.weatherapplication.data.local.NetworkMonitor

class SearchViewModel(
    private val weatherRepository: WeatherRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<CityWithWeatherResponse>>(emptyList())
    val searchResults: StateFlow<List<CityWithWeatherResponse>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun searchCitiesWithWeather() {
        val query = _searchQuery.value
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        if (!networkMonitor.isOnline.value) {
            _errorMessage.value = "Brak połączenia z internetem"
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val cityList = weatherRepository.searchCitiesByName(query)
                val result = cityList.map { city ->
                    try {
                        val weather = weatherRepository.getWeatherByCoordinates(
                            city.city.lat, city.city.lon, weatherRepository.getCurrentUnits()
                        )
                        CityWithWeatherResponse(city.city, weather)
                    } catch (e: Exception) {
                        Log.w("SearchViewModel", "Weather fetch failed for ${city.city.name}: ${e.message}")
                        CityWithWeatherResponse(city.city, null)
                    }
                }
                _searchResults.value = result
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Search failed: $query", e)
                _errorMessage.value = "Nie udało się wyszukać miast"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _errorMessage.value = null
    }
}
