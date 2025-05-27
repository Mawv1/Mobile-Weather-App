package com.example.weatherapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoriteCity: FavoriteCityEntity)

    @Query("SELECT * FROM favorite_cities")
    suspend fun getAllFavorites(): List<FavoriteCityEntity>

    @Query("DELETE FROM favorite_cities WHERE cityId = :cityId")
    suspend fun deleteById(cityId: String)
}