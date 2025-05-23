package com.example.weatherapplication.data.repository

import android.content.Context
import com.example.weatherapplication.data.local.FavoritesStore
import com.example.weatherapplication.data.model.CitySearchItem
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


class FavoritesRepository(
    private val context: Context,
    private val moshi: Moshi
) {
    private val listType = Types.newParameterizedType(List::class.java, CitySearchItem::class.java)
    private val adapter: JsonAdapter<List<CitySearchItem>> = moshi.adapter(listType)

    // Flow<List<CitySearchItem>>
    val favoritesFlow: Flow<List<CitySearchItem>> =
        FavoritesStore.getRawFavoritesFlow(context)
            .map { raw ->
                raw?.let { adapter.fromJson(it) } ?: emptyList()
            }

    // Add/remove favorites
    suspend fun addFavorite(city: CitySearchItem) {
        val current = favoritesFlow.first()
        val updated = (current + city).distinctBy { it.lat to it.lon }
        FavoritesStore.saveRawFavorites(context, adapter.toJson(updated))
    }

    suspend fun removeFavorite(city: CitySearchItem) {
        val current = favoritesFlow.first()
        val updated = current.filterNot { it.lat == city.lat && it.lon == city.lon }
        FavoritesStore.saveRawFavorites(context, adapter.toJson(updated))
    }

    suspend fun clearFavorites() {
        FavoritesStore.clearFavorites(context)
    }
}
