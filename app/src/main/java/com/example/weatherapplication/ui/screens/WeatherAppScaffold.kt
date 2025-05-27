package com.example.weatherapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.weatherapplication.data.model.CityWithWeatherResponse
import com.example.weatherapplication.viewmodel.SearchViewModel
import com.example.weatherapplication.viewmodel.SettingsViewModel
import com.example.weatherapplication.viewmodel.WeatherViewModel
import com.example.weatherapplication.viewmodel.WeatherViewModelFactory

@Composable
fun WeatherAppScaffold(viewModelFactory: WeatherViewModelFactory) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600

    val weatherViewModel = viewModel<WeatherViewModel>(factory = viewModelFactory)
    val settingsViewModel = viewModel<SettingsViewModel>(factory = viewModelFactory)
    val searchViewModel = viewModel<SearchViewModel>(factory = viewModelFactory)

    if (isTablet) {
        TabletLayout(weatherViewModel, settingsViewModel, searchViewModel)
    } else {
        PhoneLayout(weatherViewModel, settingsViewModel, searchViewModel)
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

@Composable
fun PhoneLayout(
    weatherViewModel: WeatherViewModel,
    settingsViewModel: SettingsViewModel,
    searchViewmodel: SearchViewModel
) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Search, Screen.Settings)

    var selectedCity by remember { mutableStateOf<CityWithWeatherResponse?>(null) }
    val context = LocalContext.current

    // Synchronizacja z ViewModel po zmianie miasta
    LaunchedEffect(selectedCity) {
        selectedCity?.let { city ->
            weatherViewModel.setSelectedCity(city, context)
            weatherViewModel.getWeatherByCoordinates(city.city.lat, city.city.lon)
            // Po ustawieniu miasta przejdź do ekranu CurrentWeatherScreen
            navController.navigate("current/${city.city.lat}/${city.city.lon}") {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

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
                    viewModel = weatherViewModel,
                    onCityClear = {
                        selectedCity = null
                    }
                )
            }
            composable(Screen.Search.route) {
                CitySearchScreen(
                    viewModel = searchViewmodel,
                    onCitySelected = { city: CityWithWeatherResponse ->
                        selectedCity = city
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onRefreshWeatherClick = {},
                    selectedCity = selectedCity
                )
            }
            composable("current/{lat}/{lon}") { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
                val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    CurrentWeatherScreen(
                        lat = lat,
                        lon = lon,
                        navController = navController,
                        viewModel = weatherViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun TabletLayout(
    weatherViewModel: WeatherViewModel,
    settingsViewModel: SettingsViewModel,
    searchViewModel: SearchViewModel
) {
    var selectedCity by remember { mutableStateOf<CityWithWeatherResponse?>(null) }
    val context = LocalContext.current

    LaunchedEffect(selectedCity) {
        selectedCity?.let { city ->
            weatherViewModel.setSelectedCity(city, context)
            weatherViewModel.getWeatherByCoordinates(city.city.lat, city.city.lon)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            Text("Home", style = MaterialTheme.typography.titleMedium)
            HomeWeatherScreen(
                navController = null,
                viewModel = weatherViewModel,
                modifier = Modifier.fillMaxSize(),
                onCityClear = {
                    selectedCity = null
                }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            Text("Szukaj", style = MaterialTheme.typography.titleMedium)
            CitySearchScreen(
                viewModel = searchViewModel,
                onCitySelected = { city: CityWithWeatherResponse ->
                    selectedCity = city
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            Text("Ustawienia", style = MaterialTheme.typography.titleMedium)
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onRefreshWeatherClick = {},
                selectedCity = selectedCity,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}