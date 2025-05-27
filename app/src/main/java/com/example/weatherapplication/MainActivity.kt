package com.example.weatherapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.weatherapplication.data.local.AppDatabase
import com.example.weatherapplication.data.local.NetworkMonitor
import com.example.weatherapplication.data.repository.FavoritesRepository
import com.example.weatherapplication.data.repository.WeatherRepository
import com.example.weatherapplication.ui.screens.WeatherAppScaffold
import com.example.weatherapplication.ui.theme.WeatherApplicationTheme
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.viewmodel.WeatherViewModelFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var viewModelFactory: WeatherViewModelFactory

    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicjalizacja NetworkMonitor
        networkMonitor = NetworkMonitor(this)
        networkMonitor.start()

        // Inicjalizacja Moshi i bazy danych
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

        // ViewModel factory z zależnościami
        viewModelFactory = WeatherViewModelFactory(
            repo = weatherRepository,
            favoritesRepo = favoritesRepository,
            networkMonitor = networkMonitor,
            sharedPreferences = sharedPreferences
        )


        weatherViewModel = viewModelFactory.create(WeatherViewModel::class.java)
        appLifecycleObserver = AppLifecycleObserver(weatherViewModel)

        lifecycle.addObserver(appLifecycleObserver)

        enableEdgeToEdge()

        setContent {
            WeatherApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherAppScaffold(viewModelFactory = viewModelFactory)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stop()
    }
}
