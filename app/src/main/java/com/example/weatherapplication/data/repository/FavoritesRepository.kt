package com.example.weatherapplication.data.repository

import android.content.Context
import com.example.weatherapplication.data.local.FavoritesStore
import com.example.weatherapplication.data.model.CitySearchItem
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.data.model.DailyForecast
import com.example.weatherapplication.data.model.WeatherResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class FavoritesRepository(
    private val context: Context,
    private val moshi: Moshi
) {
    private val listType = Types.newParameterizedType(List::class.java, CityWithWeatherResponse::class.java)
    private val adapter: JsonAdapter<List<CityWithWeatherResponse>> = moshi.adapter(listType)

    val favoritesFlow: Flow<List<CityWithWeatherResponse>> =
        FavoritesStore.getRawFavoritesFlow(context)
            .map { raw ->
                if (raw == null) return@map emptyList()

                try {
                    adapter.fromJson(raw) ?: emptyList()
                } catch (e: Exception) {
                    // Jeśli deserializacja jako CityWithWeatherResponse się nie powiedzie,
                    // spróbuj odczytać dane jako CitySearchItem i skonwertować je
                    val legacyAdapter: JsonAdapter<List<CitySearchItem>> =
                        moshi.adapter(Types.newParameterizedType(List::class.java, CitySearchItem::class.java))

                    val legacyList = legacyAdapter.fromJson(raw) ?: emptyList()
                    legacyList.map { cityItem ->
                        CityWithWeatherResponse(city = cityItem, weather = null)
                    }
                }
            }


    suspend fun addFavorite(city: CityWithWeatherResponse) {
        val current = favoritesFlow.first()
        val updated = (current + city).distinctBy { it.city.lat to it.city.lon }
        FavoritesStore.saveRawFavorites(context, adapter.toJson(updated))
    }

    suspend fun removeFavorite(city: CityWithWeatherResponse) {
        val current = favoritesFlow.first()
        val updated = current.filterNot {
            it.city.lat == city.city.lat && it.city.lon == city.city.lon
        }
        FavoritesStore.saveRawFavorites(context, adapter.toJson(updated))
    }

    suspend fun clearFavorites() {
        FavoritesStore.clearFavorites(context)
    }

    suspend fun updateFavorites(list: List<CityWithWeatherResponse>) {
        FavoritesStore.saveRawFavorites(context, adapter.toJson(list))
    }

    suspend fun getFavorite(city: CityWithWeatherResponse): CityWithWeatherResponse {
        return favoritesFlow.first().firstOrNull {
            it.city.lat == city.city.lat && it.city.lon == city.city.lon
        } ?: CityWithWeatherResponse(city = city.city, weather = city.weather)
    }

    suspend fun updateFavoriteWithWeather(city: CitySearchItem, weather: WeatherResponse, forecast: List<DailyForecast>) {
        val currentFavorites = favoritesFlow.first().toMutableList()
        val index = currentFavorites.indexOfFirst { it.city == city }
        if (index != -1) {
            currentFavorites[index] = CityWithWeatherResponse(city, weather, forecast)
            FavoritesStore.saveRawFavorites(context, adapter.toJson(currentFavorites))
        }
    }

    fun saveFavoritesToCache(favorites: List<CityWithWeatherResponse>) {
        val file = File(context.cacheDir, "favorites_cache.json")
        file.writeText(adapter.toJson(favorites))
    }

    fun loadFavoritesFromCache(): List<CityWithWeatherResponse> {
        val file = File(context.cacheDir, "favorites_cache.json")
        if (!file.exists()) return emptyList()
        return adapter.fromJson(file.readText()) ?: emptyList()
    }
}