package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.weatherapplication.viewmodel.SettingsViewModel
import com.example.weatherapplication.viewmodel.WeatherViewModelFactory

@Composable
fun WeatherAppScaffold(viewModelFactory: WeatherViewModelFactory) {
    val navController = rememberNavController()

    val screens = listOf(Screen.Home, Screen.Search, Screen.Settings)

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, screens = screens)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeWeatherScreen(
                    navController = navController,
                    viewModel = viewModel(factory = viewModelFactory)
                )
            }
            composable(Screen.Search.route) {
                CitySearchScreen(
                    viewModel = viewModel(factory = viewModelFactory),
                    onCitySelected = { city ->
                        navController.navigate("current/${city.lat}/${city.lon}")
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsViewModel = viewModel(factory = viewModelFactory),
                    onRefreshWeatherClick = {
                        // logika odświeżania
                    },
                    selectedCity = null
                )
            }
            composable("current/{lat}/{lon}") { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
                val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    CurrentWeatherScreen(
                        lat = lat,
                        lon = lon,
                        navController = navController
                    )
                }
            }
        }
    }
}



@Composable
fun BottomNavigationBar(navController: NavController, screens: List<Screen>) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        screens.forEach { screen ->
            val selected = currentRoute == screen.route ||
                    (currentRoute?.startsWith(screen.route) == true)

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) }
            )
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Search : Screen("search", "Szukaj", Icons.Filled.Search)
    object Settings : Screen("settings", "Ustawienia", Icons.Filled.Settings)
}
