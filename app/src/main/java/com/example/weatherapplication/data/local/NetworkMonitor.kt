package com.example.weatherapplication.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkMonitor(context: Context) {

    // _isOnline trzyma aktualny stan połączenia
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> get() = _isOnline

    // Uzyskujemy ConnectivityManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Callback reagujący na zmiany sieci
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Sprawdzamy, czy sieć ma Internet
            val nc = connectivityManager.getNetworkCapabilities(network)
            val hasInternet = nc?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isOnline.value = hasInternet
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
        }
    }

    // Rejestruje się do monitorowania sieci
    fun start() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        // Ustaw początkowy stan
        val activeNetwork = connectivityManager.activeNetwork
        val nc = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOnline.value = nc?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // Wyrejestrowuje callback
    fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}