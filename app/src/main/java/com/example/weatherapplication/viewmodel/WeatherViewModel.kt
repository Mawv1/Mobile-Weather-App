package com.example.weatherapplication.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.repository.FavoritesRepository
import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.data.local.NetworkMonitor
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val favoritesRepo: FavoritesRepository,
    private val networkMonitor: NetworkMonitor,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    init {
        Log.d("WeatherViewModel", "Instance created: ${this.hashCode()}")
        Log.d("WeatherViewModel", "WeatherViewModel instance created: $this")

        networkMonitor.start()
        viewModelScope.launch {
            delay(100)
            startAutoRefresh()
        }
    }

    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState.asStateFlow()

    private val _citySearchResults = MutableStateFlow<List<CityWithWeatherResponse>>(emptyList())
    val citySearchResults: StateFlow<List<CityWithWeatherResponse>> =
        _citySearchResults.asStateFlow()

    private val _selectedCity = MutableStateFlow<CityWithWeatherResponse?>(null)
    val selectedCity: StateFlow<CityWithWeatherResponse?> = _selectedCity.asStateFlow()

    private val _forecast = MutableStateFlow<List<DailyForecast>>(emptyList())
    val forecast: StateFlow<List<DailyForecast>> = _forecast.asStateFlow()

    private val _units =
        MutableStateFlow(sharedPreferences.getString("units", "metric") ?: "metric")
    val units: StateFlow<String> = _units.asStateFlow()

    private val _refreshInterval = MutableStateFlow(
        sharedPreferences.getInt("refresh_interval", 30).takeIf { it > 0 } ?: 30
    )
    val refreshInterval: StateFlow<Int> = _refreshInterval.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showOfflineWarning = MutableStateFlow(false)
    val showOfflineWarning: StateFlow<Boolean> = _showOfflineWarning.asStateFlow()

    private var autoRefreshJob: Job? = null

    val favorites: StateFlow<List<CityWithWeatherResponse>> =
        favoritesRepo.favoritesFlow
            .map { list -> list.map { CityWithWeatherResponse(it.city, null) } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList<CityWithWeatherResponse>())

    private val moshi = Moshi.Builder().build()
    private val weatherAdapter = moshi.adapter(WeatherResponse::class.java)
    private val forecastListType =
        Types.newParameterizedType(List::class.java, DailyForecast::class.java)
    private val forecastAdapter = moshi.adapter<List<DailyForecast>>(forecastListType)

    fun setUnits(newUnits: String) {
        if (_units.value == newUnits) return

        // Zapis do SharedPreferences!
        sharedPreferences.edit().putString("units", newUnits).apply()

        _units.value = newUnits  // Aktualizacja flow
        Log.d("WeatherViewModel", "Units changed to $newUnits")

        refreshAllCitiesWeather() // Odświeżenie pogody po zmianie jednostek
    }

    fun setRefreshInterval(newInterval: Int) {
        if (_refreshInterval.value == newInterval) return
        sharedPreferences.edit().putInt("refresh_interval", newInterval).apply()
        _refreshInterval.value = newInterval
        repository.setRefreshInterval(newInterval)
        stopAutoRefresh()
        startAutoRefresh()
    }

    fun setSelectedCity(city: CityWithWeatherResponse, context: Context) {
        if (_selectedCity.value?.city == city.city) return
        _selectedCity.value = city
        loadWeather(city, context)
    }

    fun searchCity(cityName: String) {
        viewModelScope.launch {
            try {
                val results = repository.searchCitiesByName(cityName)
                _citySearchResults.value = results
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "City search failed", e)
                _citySearchResults.value = emptyList()
            }
        }
    }

    fun addToFavorites(city: CityWithWeatherResponse) {
        viewModelScope.launch {
            favoritesRepo.addFavorite(city)
        }
    }

    fun removeFromFavorites(city: CityWithWeatherResponse) {
        viewModelScope.launch {
            favoritesRepo.removeFavorite(city)
        }
    }

    fun loadWeather(city: CityWithWeatherResponse, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUnits = _units.value
            val cached = loadWeatherCache(context, city.city.name)
            if (cached != null) {
                _weatherState.value = cached.first
                _forecast.value = cached.second
                _showOfflineWarning.value = !networkMonitor.isOnline.value
            }

            try {
                if (networkMonitor.isOnline.value) {
                    val (weather, forecastList) = repository.refreshWeather(city, currentUnits)
                    _weatherState.value = weather
                    _forecast.value = forecastList
                    _showOfflineWarning.value = false
                    saveWeatherCache(context, weather, forecastList, city.city.name)
                } else if (cached == null) {
                    _weatherState.value = null
                    _forecast.value = emptyList()
                    _showOfflineWarning.value = true
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error in loadWeather", e)
                if (cached == null) {
                    _weatherState.value = null
                    _forecast.value = emptyList()
                }
                _showOfflineWarning.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAllCitiesWeather() {
        viewModelScope.launch {
            val unitsNow = _units.value
            Log.d("WeatherViewModel", "refreshAllCitiesWeather called with units: $unitsNow")
            val selected = selectedCity.value
            val updatedFavorites = mutableListOf<CityWithWeatherResponse>()

            if (selected != null) {
                try {
                    val (weather, forecastList) = repository.refreshWeather(selected, unitsNow)
                    Log.d(
                        "WeatherViewModel",
                        "Updated weather for selected city with units: $unitsNow"
                    )
                    _weatherState.value = weather
                    _forecast.value = forecastList
                    _showOfflineWarning.value = false
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Error refreshing selected city weather", e)
                    _showOfflineWarning.value = true
                }
            }

            // Odśwież pogodę dla ulubionych (pomijając jeśli to samo co wybrane)
            favorites.value.forEach { city ->
                if (selected != null && city.city == selected.city) {
                    updatedFavorites.add(city) // już odświeżone wybrane miasto
                } else {
                    try {
                        val (weather, forecast) = repository.refreshWeather(city, unitsNow)
                        updatedFavorites.add(CityWithWeatherResponse(city.city, weather, forecast))
                    } catch (e: Exception) {
                        Log.e(
                            "WeatherViewModel",
                            "Error refreshing favorite city weather: ${city.city.name}",
                            e
                        )
                        updatedFavorites.add(city)
                    }
                }
            }

            favoritesRepo.updateFavorites(updatedFavorites)
        }
    }

    private suspend fun saveWeatherCache(
        context: Context,
        weather: WeatherResponse,
        forecastList: List<DailyForecast>,
        cityName: String
    ) = withContext(Dispatchers.IO) {
        try {
            val weatherFile = File(context.filesDir, "${cityName}_weather.json")
            val forecastFile = File(context.filesDir, "${cityName}_forecast.json")
            weatherFile.writeText(weatherAdapter.toJson(weather))
            forecastFile.writeText(forecastAdapter.toJson(forecastList))
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error saving cache", e)
        }
    }

    private suspend fun loadWeatherCache(
        context: Context,
        cityName: String
    ): Pair<WeatherResponse, List<DailyForecast>>? = withContext(Dispatchers.IO) {
        try {
            val weatherFile = File(context.filesDir, "${cityName}_weather.json")
            val forecastFile = File(context.filesDir, "${cityName}_forecast.json")
            if (!weatherFile.exists() || !forecastFile.exists()) return@withContext null
            val weather = weatherAdapter.fromJson(weatherFile.readText())
            val forecastList = forecastAdapter.fromJson(forecastFile.readText())
            if (weather != null && forecastList != null) {
                Pair(weather, forecastList)
            } else null
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error loading cache", e)
            null
        }
    }

    fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            refreshInterval.collectLatest { interval ->
                Log.d("WeatherViewModel", "Auto-refresh interval changed to $interval minutes")
                while (isActive) {
                    delay(interval * 60 * 1000L)
                    refreshWeather()
                }
            }
        }
    }

    suspend fun refreshWeather() {
        Log.d("WeatherViewModel", "refreshWeather called")
        val cityWithWeather = _selectedCity.value ?: return
        try {
            val (weather, forecastList) = repository.refreshWeather(cityWithWeather, _units.value)
            _weatherState.value = weather
            _forecast.value = forecastList
            _showOfflineWarning.value = false
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error refreshing weather", e)
            _showOfflineWarning.value = true
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.stop()
    }

    fun getWeatherByCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUnits = _units.value
                val weather = repository.getWeatherByCoordinates(lat, lon, currentUnits)
                _weatherState.value = weather
                _showOfflineWarning.value = false
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error getting weather by coordinates", e)
                _weatherState.value = null
                _showOfflineWarning.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getUnits(): String = _units.value
    fun getRefreshInterval(): Int {
        return _refreshInterval.value
    }
}