package com.example.weatherapplication

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.weatherapplication.viewmodel.WeatherViewModel

class AppLifecycleObserver(
    private val weatherViewModel: WeatherViewModel
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        weatherViewModel.setAppInForeground(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        weatherViewModel.setAppInForeground(false)
    }
}
