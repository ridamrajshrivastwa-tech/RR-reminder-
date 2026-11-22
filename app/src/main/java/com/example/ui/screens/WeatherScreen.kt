package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.SavedCityEntity
import com.example.data.weather.CitySearchResult
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RRCard
import com.example.ui.components.RRGlassCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.WeatherUiState
import com.example.util.DateTimeUtils
import kotlin.math.roundToInt

@Composable
fun WeatherScreen(
    weatherState: WeatherUiState,
    savedCities: List<SavedCityEntity>,
    citySearchResults: List<CitySearchResult>,
    citySearchError: String?,
    useFahrenheit: Boolean,
    use24HourTime: Boolean,
    onSearchCities: (String) -> Unit,
    onSelectSearchCity: (CitySearchResult) -> Unit,
    onSelectSavedCity: (SavedCityEntity) -> Unit,
    onRemoveSavedCity: (String) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onRetry: () -> Unit,
    onToggleUnit: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var locationPermissionDenied by remember { mutableStateOf(false) }

    val weatherCities = remember(savedCities) {
        savedCities.filter { it.forWeather && it.latitude != null && it.longitude != null }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            locationPermissionDenied = false
            onUseDeviceLocation()
        } else {
            locationPermissionDenied = true
        }
    }

    fun formatTemp(tempC: Double): String {
        val value = if (useFahrenheit) (tempC * 9.0 / 5.0) + 32.0 else tempC
        val unit = if (useFahrenheit) "°F" else "°C"
        return "${value.roundToInt()}$unit"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Weather", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        text = "Live Open-Meteo Forecast",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilterChip(
                    selected = useFahrenheit,
                    onClick = { onToggleUnit(!useFahrenheit) },
                    label = { Text(if (useFahrenheit) "°F" else "°C") }
                )
                IconButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) onUseDeviceLocation()
                        else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    modifier = Modifier.testTag("weather_location_btn")
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = "Use device location")
                }
                IconButton(onClick = onRetry, modifier = Modifier.testTag("weather_refresh_btn")) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh weather")
                }
            }
        }

        if (locationPermissionDenied) {
            item {
                RRCard {
                    Text(
                        text = "Location permission was denied. You can still search and save any city manually below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // City Search
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchCities(it)
                },
                placeholder = { Text("Search city for live forecast…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weather_city_search_input")
            )
        }

        if (!citySearchError.isNullOrBlank()) {
            item {
                Text(
                    text = citySearchError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (searchQuery.trim().length >= 2 && citySearchResults.isNotEmpty()) {
            item {
                RRCard {
                    Text("Search Results", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    citySearchResults.take(5).forEach { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(result.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    listOf(result.admin1, result.country).filter { it.isNotBlank() }.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = {
                                onSelectSearchCity(result)
                                searchQuery = ""
                            }) {
                                Text("Select")
                            }
                        }
                    }
                }
            }
        }

        // Saved Cities Chips
        if (weatherCities.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(weatherCities, key = { it.id }) { city ->
                        val isSelected = weatherState is WeatherUiState.Success &&
                            weatherState.snapshot.cityName.equals(city.cityName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectSavedCity(city) },
                            label = { Text(city.cityName) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemoveSavedCity(city.id) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove city", modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        when (weatherState) {
            WeatherUiState.NeedsLocationOrCity -> {
                item {
                    EmptyStatePanel(
                        icon = Icons.Rounded.WbSunny,
                        title = "Choose a City or Use Location",
                        message = "Search for your city above or tap Use My Location to load live weather, hourly trends, and 7-day forecasts.",
                        actionButton = {
                            Button(onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) onUseDeviceLocation()
                                else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }) {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Use My Location")
                            }
                        }
                    )
                }
            }

            WeatherUiState.Loading -> {
                item {
                    RRCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("Fetching live weather forecast…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            is WeatherUiState.Error -> {
                item {
                    RRCard {
                        Text("Weather Unavailable", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(6.dp))
                        Text(weatherState.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                }
            }

            is WeatherUiState.Success -> {
                val snap = weatherState.snapshot

                // Severe / Advisory Alert Banner
                if (!snap.weatherAlert.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarningAmber.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = WarningAmber)
                                Spacer(Modifier.width(10.dp))
                                Text(snap.weatherAlert, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Hero Current Conditions
                item {
                    RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(snap.cityName, style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    text = snap.conditionLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = formatTemp(snap.temperatureC),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Feels like ${formatTemp(snap.feelsLikeC)} • Updated ${DateTimeUtils.formatDigitalTime(snap.lastUpdatedMillis, use24HourTime, showSeconds = false)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = weatherIconForCode(snap.weatherCode),
                                contentDescription = snap.conditionLabel,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }

                // Metrics Grid
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WeatherMetricCard("Rain Prob.", "${snap.rainProbabilityToday}%", Icons.Rounded.WaterDrop, Modifier.weight(1f))
                        WeatherMetricCard("Humidity", "${snap.humidityPercent}%", Icons.Rounded.WaterDrop, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WeatherMetricCard("Wind", "${snap.windSpeedKmh.roundToInt()} km/h ${snap.windDirectionLabel}", Icons.Rounded.Air, Modifier.weight(1f))
                        WeatherMetricCard("UV Index", String.format("%.1f", snap.uvIndexToday), Icons.Rounded.WbSunny, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WeatherMetricCard("Sunrise", snap.sunriseToday, Icons.Rounded.WbTwilight, Modifier.weight(1f))
                        WeatherMetricCard("Sunset", snap.sunsetToday, Icons.Rounded.WbTwilight, Modifier.weight(1f))
                    }
                }

                // Hourly Forecast
                item {
                    Text("24-Hour Forecast", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(snap.hourly) { h ->
                            RRCard(modifier = Modifier.width(88.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(h.timeLabel, style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(6.dp))
                                    Icon(weatherIconForCode(h.weatherCode), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text(formatTemp(h.tempC), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${h.rainProbability}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }

                // 7-Day Forecast
                item {
                    Text("7-Day Forecast", style = MaterialTheme.typography.titleMedium)
                }
                items(snap.daily) { d ->
                    RRCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(d.dateLabel, style = MaterialTheme.typography.titleSmall)
                                Text("${d.condition} • Rain ${d.rainProbabilityMax}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(weatherIconForCode(d.weatherCode), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "${formatTemp(d.minTempC)} / ${formatTemp(d.maxTempC)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun WeatherMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    RRCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun weatherIconForCode(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Rounded.WbSunny
    2, 3, 45, 48 -> Icons.Rounded.Cloud
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.WaterDrop
}
