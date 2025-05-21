package com.example.weatherapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeatherCacheEntry)

    @Query("SELECT * FROM weather_cache WHERE cityId = :cityId")
    suspend fun get(cityId: String): WeatherCacheEntry?

    @Query("DELETE FROM weather_cache WHERE cityId = :cityId")
    suspend fun delete(cityId: String)
}

