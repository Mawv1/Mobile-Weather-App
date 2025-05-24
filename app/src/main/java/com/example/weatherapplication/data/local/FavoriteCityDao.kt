//package com.example.weatherapplication.data.local
//
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface FavoriteCityDao {
//    @Query("SELECT * FROM favorite_cities")
//    fun getAllFavoriteCities(): Flow<List<FavoriteCityWithWeather>>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertFavoriteCity(city: FavoriteCityWithWeather)
//
//    @Delete
//    suspend fun deleteFavoriteCity(city: FavoriteCityWithWeather)
//}