package com.example.weatherapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.weatherapplication.data.local.AppDatabase
import com.example.weatherapplication.data.local.NetworkMonitor
import com.example.weatherapplication.data.repository.FavoritesRepository
import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.ui.theme.WeatherApplicationTheme
import com.example.weatherapplication.ui.WeatherApp
import com.example.weatherapplication.viewmodel.WeatherViewModelFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var viewModelFactory: WeatherViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicjalizacja NetworkMonitor
        networkMonitor = NetworkMonitor(this)
        networkMonitor.start()

        // Inicjalizacja Moshi i bazy danych potrzebnej w repozytoriach
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val db = AppDatabase.getInstance(applicationContext)
        val cacheDao = db.weatherCacheDao()

        // Inicjalizacja repozytoriów
        val favoritesRepository = FavoritesRepository(applicationContext, moshi)
        val sharedPreferences = getSharedPreferences("weather_prefs", MODE_PRIVATE)

        val weatherRepository = WeatherRepository(
            apiKey = BuildConfig.WEATHER_API_KEY,
            cacheDao = cacheDao,
            moshi = moshi,
            sharedPreferences = sharedPreferences
        )

        // Tworzymy factory ViewModel z wszystkimi zależnościami
        viewModelFactory = WeatherViewModelFactory(
            repo = weatherRepository,
            favoritesRepo = favoritesRepository,
            networkMonitor = networkMonitor,
            sharedPreferences = sharedPreferences
        )

        enableEdgeToEdge()

        setContent {
            WeatherApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherApp(viewModelFactory)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stop()
    }
}

