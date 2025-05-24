//package com.example.weatherapplication.ui.composables
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.LocationOn
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.example.weatherapplication.data.local.FavoriteCityWithWeather
//
//@Composable
//fun FavoritesList(
//    favoriteCities: List<FavoriteCityWithWeather>,
//    navController: NavController,
//    modifier: Modifier = Modifier
//) {
//    if (favoriteCities.isEmpty()) {
//        Text("Brak ulubionych miast.")
//    } else {
//        LazyColumn(
//            modifier = Modifier.fillMaxWidth(),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            items(items = favoriteCities) { city ->
//                FavoriteCityRow(
//                    city = city,
//                    onClick = {
//                        navController.navigate("current/${city.lat}/${city.lon}") {
//                            popUpTo("home") { inclusive = false }
//                            launchSingleTop = true
//                        }
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun FavoriteCityRow(
//    city: FavoriteCityWithWeather,
//    onClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick)
//            .padding(vertical = 6.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Icon(
//            imageVector = Icons.Default.LocationOn,
//            contentDescription = "Miasto",
//            tint = MaterialTheme.colorScheme.primary
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        Column {
//            Text(
//                text = "${city.name}, ${city.country}",
//                color = MaterialTheme.colorScheme.onBackground
//            )
//            Text(
//                text = "Lat: ${city.lat}, Lon: ${city.lon}",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
//            )
//            Text(
//                text = "Temp: ${city.temperature}, ${city.weatherDescription}",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
//            )
//        }
//    }
//}