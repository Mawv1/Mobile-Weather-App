//package com.example.weatherapplication.viewmodel
//
//import android.app.Application
//import androidx.compose.runtime.mutableStateOf
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.weatherapplication.data.local.AppDatabase
//import com.example.weatherapplication.data.local.FavoriteCityWithWeather
//import com.example.weatherapplication.data.model.CitySearchItem
//import com.example.weatherapplication.data.repository.WeatherRepository
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.flow.first
//
//class FavoritesViewModel(
//    application: Application,
//    private val weatherRepository: WeatherRepository, // Dodaj repozytorium pogody
//    private val weatherViewModel: WeatherViewModel
//) : AndroidViewModel(application) {
//    private val database = AppDatabase.getInstance(application)
//    private val favoriteCityDao = database.favoriteCityDao()
//
//    val favoriteCities: Flow<List<FavoriteCityWithWeather>> = favoriteCityDao.getAllFavoriteCities()
//
//    private val _units = mutableStateOf(weatherViewModel.getUnits())
//
//    fun addFavoriteCity(citySearchItem: CitySearchItem) {
//        viewModelScope.launch {
//            val weather = weatherRepository.getWeatherByCoordinates(citySearchItem.lat, citySearchItem.lon, _units.value)
//            val favoriteCity = FavoriteCityWithWeather(
//                id = "${citySearchItem.lat},${citySearchItem.lon}",
//                name = citySearchItem.name,
//                country = citySearchItem.country,
//                lat = citySearchItem.lat,
//                lon = citySearchItem.lon,
//                temperature = weather.temperature,
//                //                weatherDescription = weather.description
//            )
//            favoriteCityDao.insertFavoriteCity(favoriteCity)
//        }
//    }
//
//    fun removeFavoriteCity(city: FavoriteCityWithWeather) {
//        viewModelScope.launch {
//            favoriteCityDao.deleteFavoriteCity(city)
//        }
//    }
//
//    fun updateWeatherForCity(city: FavoriteCityWithWeather) {
//        viewModelScope.launch {
//            val weather = weatherRepository.getCurrentWeather(city.lat, city.lon)
//            val updatedCity = city.copy(
//                temperature = weather.temperature,
//                weatherDescription = weather.description
//            )
//            favoriteCityDao.insertFavoriteCity(updatedCity)
//        }
//    }
//
//    // Jednorazowe odświeżenie pogody dla wszystkich ulubionych miast
//    fun refreshFavoritesWeather() {
//        viewModelScope.launch {
//            val cities = favoriteCityDao.getAllFavoriteCities().first() // pobierz aktualną listę
//            cities.forEach { city ->
//                val weather = weatherRepository.getCurrentWeather(city.lat, city.lon)
//                val updatedCity = city.copy(
//                    temperature = weather.temperature,
//                    weatherDescription = weather.description
//                )
//                favoriteCityDao.insertFavoriteCity(updatedCity)
//            }
//        }
//    }
//}
