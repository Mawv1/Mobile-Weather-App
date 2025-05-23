package com.example.weatherapplication.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.weatherapplication.data.model.CitySearchItem
import kotlinx.coroutines.flow.map

// rozszerzenie Context, tworzy DataStore o nazwie "favorites_prefs"
private val Context.favoritesDataStore by preferencesDataStore("favorites_prefs")

object FavoritesStore {
    private val KEY_FAVORITES = stringPreferencesKey("key_favorite_cities")

    // Zwraca Flow<String?> gdzie string to JSON listy CitySearchItem
    fun getRawFavoritesFlow(context: Context) =
        context.favoritesDataStore.data
            .map { prefs ->
                prefs[KEY_FAVORITES]
            }

    suspend fun saveRawFavorites(context: Context, json: String) {
        context.favoritesDataStore.edit { prefs ->
            prefs[KEY_FAVORITES] = json
        }
    }

    suspend fun clearFavorites(context: Context) {
        context.favoritesDataStore.edit { prefs ->
            prefs.remove(KEY_FAVORITES)
        }
    }
}
