package com.example.weatherapplication.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.repository.FavoritesRepository
import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.data.local.NetworkMonitor
import com.example.weatherapplication.data.model.ForecastResponse
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
        networkMonitor.start()
        viewModelScope.launch {
            delay(100)
            startAutoRefresh()
        }
    }

    // --- StateFlows for UI ---
    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState.asStateFlow()

    private val _citySearchResults = MutableStateFlow<List<CityWithWeatherResponse>>(emptyList())
    val citySearchResults: StateFlow<List<CityWithWeatherResponse>> = _citySearchResults.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _selectedCity = MutableStateFlow<CityWithWeatherResponse?>(null)
    val selectedCity: StateFlow<CityWithWeatherResponse?> = _selectedCity.asStateFlow()

    private val _forecast = MutableStateFlow<List<DailyForecast>>(emptyList())
    val forecast: StateFlow<List<DailyForecast>> = _forecast.asStateFlow()

    private val _units = MutableStateFlow(sharedPreferences.getString("units", "metric") ?: "metric")
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

    private val _isAppInForeground = MutableStateFlow(true)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    val favorites: StateFlow<List<CityWithWeatherResponse>> =
        favoritesRepo.favoritesFlow
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Moshi adapters ---
    private val moshi = Moshi.Builder().build()
    private val weatherAdapter = moshi.adapter(WeatherResponse::class.java)
    private val forecastListType = Types.newParameterizedType(List::class.java, DailyForecast::class.java)
    private val forecastAdapter = moshi.adapter<List<DailyForecast>>(forecastListType)

    // --- Methods for updating settings ---
    fun setUnits(newUnits: String) {
        if (_units.value == newUnits) return

        sharedPreferences.edit().putString("units", newUnits).apply()
        _units.value = newUnits
//        Log.d("WeatherViewModel", "Units changed to $newUnits")

        refreshAllCitiesWeather()
    }

    fun setRefreshInterval(newInterval: Int) {
        if (_refreshInterval.value == newInterval) return

        sharedPreferences.edit().putInt("refresh_interval", newInterval).apply()
        _refreshInterval.value = newInterval

        stopAutoRefresh()
        startAutoRefresh()
    }

    // --- City selection ---
    fun setSelectedCity(city: CityWithWeatherResponse) {
        if (_selectedCity.value?.city == city.city) return

        _selectedCity.value = city
        loadWeather(city)
    }

    fun setAppInForeground(isForeground: Boolean) {
        _isAppInForeground.value = isForeground
    }

    // --- Search ---
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

    // --- Favorites management ---
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

    // --- Load weather for a city ---
    fun loadWeather(city: CityWithWeatherResponse) {
        viewModelScope.launch {
            // Najpierw cache bez ładowania
            val cached = favoritesRepo.getFavorite(city)
            if (cached != null) {
                _weatherState.value = cached.weather
                _forecast.value = cached.forecast ?: emptyList()
                _showOfflineWarning.value = !networkMonitor.isOnline.value
            } else {
                _weatherState.value = null
                _forecast.value = emptyList()
                _showOfflineWarning.value = !networkMonitor.isOnline.value
            }

            // Jeśli mamy internet, to dopiero wtedy ustawiamy loading i odświeżamy
            if (networkMonitor.isOnline.value && _isAppInForeground.value) {
                _isLoading.value = true
                try {
                    val (weather, forecastList) = repository.refreshWeather(city, _units.value)
                    _weatherState.value = weather
                    _forecast.value = forecastList
                    _showOfflineWarning.value = false
                    favoritesRepo.updateFavoriteWithWeather(city.city, weather, forecastList)
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Error in loadWeather network refresh", e)
                    _showOfflineWarning.value = true
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }


    // --- Refresh weather for selected city and favorites ---
    fun refreshAllCitiesWeather() {
        viewModelScope.launch {
            val unitsNow = _units.value
            val selected = selectedCity.value
            val updatedFavorites = mutableListOf<CityWithWeatherResponse>()

            if (selected != null) {
                try {
                    val (weather, forecastList) = repository.refreshWeather(selected, unitsNow)
                    _weatherState.value = weather
                    _forecast.value = forecastList
                    _showOfflineWarning.value = false
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Error refreshing selected city weather", e)
                    _showOfflineWarning.value = true
                }
            }

            favorites.value.forEach { city ->
                if (selected != null && city.city == selected.city) {
                    updatedFavorites.add(city)
                } else {
                    try {
                        val (weather, forecast) = repository.refreshWeather(city, unitsNow)
                        updatedFavorites.add(CityWithWeatherResponse(city.city, weather, forecast))
                    } catch (e: Exception) {
                        Log.e("WeatherViewModel", "Error refreshing favorite city weather: ${city.city.name}", e)
                        updatedFavorites.add(city)
                    }
                }
            }

            favoritesRepo.updateFavorites(updatedFavorites)
        }
    }

    // --- Auto-refresh management ---
    fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            refreshInterval.collectLatest { interval ->
                Log.d("WeatherViewModel", "Auto-refresh interval changed to $interval minutes")
                while (isActive) {
                    delay(interval * 60 * 1000L)
                    if (_isAppInForeground.value && networkMonitor.isOnline.value) {
                        refreshWeather()
                    } else {
                        Log.d("WeatherViewModel", "Skipping refresh: App in foreground: ${_isAppInForeground.value}, Online: ${networkMonitor.isOnline.value}")
                    }
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

    // --- Weather by coordinates ---
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

    private fun checkInternetConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    // --- Helpers for external use ---
    fun getUnits(): String = _units.value
    fun getRefreshInterval(): Int = _refreshInterval.value
}
