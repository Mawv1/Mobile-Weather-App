package com.example.weatherapplication.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.example.weatherapplication.data.repository.FavoritesRepository
import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.data.local.NetworkMonitor
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        startAutoRefresh()
    }

    // --- Stany ---
    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState

    private val _citySearchResults = MutableLiveData<List<CitySearchItem>>()
    val citySearchResults: LiveData<List<CitySearchItem>> = _citySearchResults

    private val _selectedCity = MutableLiveData<CitySearchItem?>()
    val selectedCity: LiveData<CitySearchItem?> = _selectedCity

    private val _forecast = MutableStateFlow<List<DailyForecast>>(emptyList())
    val forecast: StateFlow<List<DailyForecast>> = _forecast

    // --- Jednostki temperatury (metric/imperial) ---
    private val _units = MutableStateFlow(sharedPreferences.getString("units", "metric") ?: "metric")
    val units: StateFlow<String> = _units

    fun getUnits(): String = _units.value

    fun setUnits(newUnits: String) {
        Log.d("WeatherViewModel", "setUnits called with: $newUnits")
        sharedPreferences.edit().putString("units", newUnits).apply()
        _units.value = newUnits
        repository.setUnits(newUnits)
        refreshWeather()
    }

    // --- Odświeżanie co X minut (np. dla automatycznego odświeżania danych) ---
    private val _refreshInterval = MutableStateFlow(
        try {
            sharedPreferences.getInt("refresh_interval", 30).takeIf { it > 0 } ?: 30
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Failed to load refresh interval from prefs", e)
            30
        }
    )
//    val refreshInterval: StateFlow<Int> = _refreshInterval
    private var autoRefreshJob: Job? = null

    fun setRefreshInterval(newInterval: Int) {
        Log.d("WeatherViewModel", "setRefreshInterval called with: $newInterval")
        sharedPreferences.edit().putInt("refresh_interval", newInterval).apply()
        _refreshInterval.value = newInterval
        repository.setRefreshInterval(newInterval)
    }

    // --- Ulubione miasta ---
    val favorites: StateFlow<List<CitySearchItem>> =
        favoritesRepo.favoritesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedFavoriteWeather = MutableStateFlow<WeatherResponse?>(null)
    val selectedFavoriteWeather: StateFlow<WeatherResponse?> = _selectedFavoriteWeather

    // --- Sieć ---
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    private val _showOfflineWarning = MutableStateFlow(false)
    val showOfflineWarning: StateFlow<Boolean> = _showOfflineWarning

    // --- Moshi do cache ---
    private val moshi = Moshi.Builder().build()
    private val weatherAdapter = moshi.adapter(WeatherResponse::class.java)
    private val forecastListType = Types.newParameterizedType(List::class.java, DailyForecast::class.java)
    private val forecastAdapter = moshi.adapter<List<DailyForecast>>(forecastListType)

    // --- Zarządzanie miastem i pogodą ---
    fun setSelectedCity(city: CitySearchItem) {
        Log.d("WeatherViewModel", "setSelectedCity: ${city.name} (${city.lat}, ${city.lon})")
        _selectedCity.postValue(city)
    }

    fun getWeatherByCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            val currentUnits = _units.value
            val cachedWeather = _weatherState.value
            if (cachedWeather != null && cachedWeather.coord.lat == lat && cachedWeather.coord.lon == lon) {
                Log.d("WeatherViewModel", "Dane aktualne, pomijam zapytanie")
                return@launch
            }
            try {
                val weather = repository.getWeatherByCoordinates(lat, lon, currentUnits)
                val forecastData = repository.getForecastByCoordinates(lat, lon, 5, currentUnits)
                _weatherState.value = weather
                _forecast.value = forecastData
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Błąd pobierania pogody wg koordynatów", e)
            }
        }
    }

    fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return

        autoRefreshJob = viewModelScope.launch {
            while (true) {
                try {
                    val intervalMinutes = _refreshInterval.value.takeIf { it > 0 } ?: 30
                    Log.d("WeatherViewModel", "Auto-refresh every $intervalMinutes minutes")
                    delay(intervalMinutes * 60 * 1000L)
                    refreshWeather()
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Error in auto-refresh loop", e)
                    delay(30 * 60 * 1000L) // fallback delay
                }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun getWeatherForSelectedFavorite(city: CitySearchItem) {
        viewModelScope.launch {
            val response = repository.getWeatherByCoordinates(city.lat, city.lon, _units.value)
            _selectedFavoriteWeather.value = response
        }
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

    fun selectCity(city: CitySearchItem, context: android.content.Context) {
        _selectedCity.value = city
        loadWeather(city, context)
    }

    fun addToFavorites(city: CitySearchItem) {
        viewModelScope.launch {
            favoritesRepo.addFavorite(city)
//            loadFavorites()
        }
    }

    fun removeFromFavorites(city: CitySearchItem) {
        viewModelScope.launch {
            favoritesRepo.removeFavorite(city)
//            loadFavorites()
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.stop()
    }

    // --- Cache ---
    private suspend fun saveWeatherCache(context: android.content.Context, weather: WeatherResponse, forecastList: List<DailyForecast>, cityName: String) = withContext(Dispatchers.IO) {
        try {
            val weatherFile = File(context.filesDir, "${cityName}_weather.json")
            val forecastFile = File(context.filesDir, "${cityName}_forecast.json")
            weatherFile.writeText(weatherAdapter.toJson(weather))
            forecastFile.writeText(forecastAdapter.toJson(forecastList))
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Błąd zapisu cache", e)
        }
    }

    private suspend fun loadWeatherCache(context: android.content.Context, cityName: String): Pair<WeatherResponse, List<DailyForecast>>? = withContext(Dispatchers.IO) {
        try {
            val weatherFile = File(context.filesDir, "${cityName}_weather.json")
            val forecastFile = File(context.filesDir, "${cityName}_forecast.json")

            if (!weatherFile.exists() || !forecastFile.exists()) return@withContext null

            val weather = weatherAdapter.fromJson(weatherFile.readText())
            val forecastList = forecastAdapter.fromJson(forecastFile.readText())

            if (weather != null && forecastList != null) {
                return@withContext Pair(weather, forecastList)
            }
            null
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Błąd wczytywania cache", e)
            null
        }
    }

    fun loadWeather(city: CitySearchItem, context: android.content.Context) {
        Log.d("WeatherViewModel", "loadWeather called for city: ${city.name}, isOnline = ${isOnline.value}")
        viewModelScope.launch {
            try {
                if (isOnline.value) {
                    val currentUnits = _units.value
                    val (weather, forecastList) = repository.refreshWeather(city, currentUnits)
                    _weatherState.value = weather
                    _forecast.value = forecastList
                    _showOfflineWarning.value = false
                    saveWeatherCache(context, weather, forecastList, city.name)
                } else {
                    val cached = loadWeatherCache(context, city.name)
                    if (cached != null) {
                        _weatherState.value = cached.first
                        _forecast.value = cached.second
                        _showOfflineWarning.value = true
                    } else {
                        _weatherState.value = null
                        _forecast.value = emptyList()
                        _showOfflineWarning.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error in loadWeather", e)
                _weatherState.value = null
                _forecast.value = emptyList()
                _showOfflineWarning.value = true
            }
        }
    }

    fun refreshWeather() {
        val city = selectedCity.value
        if (city == null) {
            Log.w("WeatherViewModel", "No city selected for refreshing weather")
            return
        }
        viewModelScope.launch {
            try {
                val (weather, forecastList) = repository.refreshWeather(city, _units.value)
                _weatherState.value = weather
                _forecast.value = forecastList
                _showOfflineWarning.value = false
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error refreshing weather", e)
                _showOfflineWarning.value = true
            }
        }
    }
}
