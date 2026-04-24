package com.example.blegps.ui

import android.location.Geocoder
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blegps.ble.Coordinate
import com.example.blegps.ble.CoordinateSource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverApp(vm: ReceiverViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val effectiveCarLocation = state.coordinate ?: state.manualSource
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Map", "Route", "Connection", "Settings")
    if (isLandscape) {
        LandscapeInfotainmentLayout(
            state = state,
            effectiveCarLocation = effectiveCarLocation,
            onSourceSelected = vm::setManualSource,
            onDestinationSelected = vm::setDestination,
            onRoutePreview = vm::previewRoute,
            onSourceChanged = vm::setSource,
            onSafetyChanged = vm::setSafetyMode,
            onStopNavigation = vm::stopNavigation
        )
        return
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Headunit Navigator") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { idx, name ->
                    Tab(selected = tab == idx, onClick = { tab = idx }, text = { Text(name) })
                }
            }
            when (tab) {
                0 -> MapScreen(
                    coordinate = effectiveCarLocation,
                    destination = state.selectedDestination,
                    showRoute = effectiveCarLocation != null && state.selectedDestination != null,
                    route = state.routePreview,
                    routePolyline = state.routePolyline,
                    isNavigating = state.isNavigating
                )
                1 -> RouteScreen(
                    state = state,
                    onSourceSelected = vm::setManualSource,
                    onDestinationSelected = vm::setDestination,
                    onRoutePreview = vm::previewRoute,
                    onNavigationStarted = { tab = 0 }
                )
                2 -> ConnectionScreen(state)
                else -> SettingsScreen(state, onSourceChanged = vm::setSource, onSafetyChanged = vm::setSafetyMode)
            }
        }
    }
}

@Composable
private fun LandscapeInfotainmentLayout(
    state: ReceiverUiState,
    effectiveCarLocation: Coordinate?,
    onSourceSelected: (String, Coordinate) -> Unit,
    onDestinationSelected: (String, Coordinate) -> Unit,
    onRoutePreview: (Coordinate) -> Unit,
    onSourceChanged: (CoordinateSource) -> Unit,
    onSafetyChanged: (Boolean) -> Unit,
    onStopNavigation: () -> Unit
) {
    var panelTab by remember { mutableIntStateOf(0) }
    val panelTabs = listOf("Route", "Connection", "Settings")
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.30f)
                .background(Color(0xFFF7F7F7))
                .padding(12.dp)
        ) {
            Text("Navigation", style = MaterialTheme.typography.titleLarge)
            TabRow(selectedTabIndex = panelTab) {
                panelTabs.forEachIndexed { idx, label ->
                    Tab(selected = panelTab == idx, onClick = { panelTab = idx }, text = { Text(label) })
                }
            }
            when (panelTab) {
                0 -> RouteScreen(
                    state = state,
                    onSourceSelected = onSourceSelected,
                    onDestinationSelected = onDestinationSelected,
                    onRoutePreview = onRoutePreview,
                    onNavigationStarted = {}
                )
                1 -> ConnectionScreen(state)
                else -> SettingsScreen(
                    state = state,
                    onSourceChanged = onSourceChanged,
                    onSafetyChanged = onSafetyChanged
                )
            }
            if (state.isNavigating) {
                OutlinedButton(onClick = onStopNavigation, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Stop Navigation")
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.70f)
        ) {
            MapScreen(
                coordinate = effectiveCarLocation,
                destination = state.selectedDestination,
                showRoute = effectiveCarLocation != null && state.selectedDestination != null,
                route = state.routePreview,
                routePolyline = state.routePolyline,
                isNavigating = state.isNavigating
            )
        }
    }
}

@Composable
private fun MapScreen(
    coordinate: Coordinate?,
    destination: Coordinate?,
    showRoute: Boolean,
    route: com.example.blegps.navigation.RoutePreview?,
    routePolyline: List<Coordinate>,
    isNavigating: Boolean
) {
    val fallback = LatLng(37.7749, -122.4194)
    val current = coordinate?.let { LatLng(it.lat, it.lon) } ?: fallback
    val destinationLatLng = destination?.let { LatLng(it.lat, it.lon) }
    val speedMps = coordinate?.speedMps ?: 0.0
    val dynamicZoom = calculateZoomFromSpeed(speedMps)
    
    // Calculate bearing from route polyline (stays on road), fallback to heading if no route
    val routeBearing = if (isNavigating && routePolyline.isNotEmpty() && coordinate != null) {
        calculateBearingFromRoute(coordinate, routePolyline).toFloat()
    } else {
        coordinate?.headingDeg ?: 0f
    }
    
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.builder()
            .target(current)
            .zoom(dynamicZoom)
            .bearing(routeBearing)
            .build()
    }
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(current, routeBearing, destinationLatLng, isNavigating, dynamicZoom) {
        if (isNavigating && destinationLatLng != null) {
            cameraState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder()
                        .target(current)
                        .zoom(dynamicZoom)
                        .bearing(routeBearing)
                        .build()
                ),
                durationMs = 650
            )
        } else if (destinationLatLng != null) {
            val bounds = LatLngBounds.builder()
                .include(current)
                .include(destinationLatLng)
                .build()
            cameraState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 120),
                durationMs = 800
            )
        } else {
            cameraState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder()
                        .target(current)
                        .zoom(dynamicZoom)
                        .bearing(routeBearing)
                        .build()
                ),
                durationMs = 500
            )
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(mapType = MapType.NORMAL),
            onMapLoaded = { mapLoaded = true }
        ) {
            Marker(
                state = MarkerState(position = current),
                title = "Vehicle",
                snippet = "BLE/system/manual position"
            )
            destinationLatLng?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Destination"
                )
            }
            if (showRoute && destinationLatLng != null) {
                Polyline(
                    points = routePolyline
                        .map { LatLng(it.lat, it.lon) }
                        .ifEmpty { listOf(current, destinationLatLng) },
                    color = Color(0xFF1E88E5),
                    width = 10f
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .background(Color(0xAA000000))
                .padding(10.dp)
        ) {
            Text("Map: ${if (mapLoaded) "loaded" else "loading/unavailable"}", color = Color.White)
            Text("Car: ${"%.5f".format(current.latitude)}, ${"%.5f".format(current.longitude)}", color = Color.White)
            Text(
                "Dest: ${
                    destinationLatLng?.let { "${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}" } ?: "not set"
                }",
                color = Color.White
            )
            Text("Speed: ${"%.1f".format(speedMps * 3.6)} km/h | Heading: ${"%.0f".format(routeBearing)}°", color = Color(0xFF81C784))
            Text("Zoom: ${dynamicZoom.toInt()} (auto from speed)", color = Color(0xFF81C784))
            if (!mapLoaded) {
                Text("Verify Maps API key/billing/internet.", color = Color(0xFFFFCC80))
            }
        }

        route?.let {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .fillMaxWidth()
                    .background(Color(0xCC101010))
                    .padding(12.dp)
            ) {
                Text("Route ${it.distanceMeters} m | ETA ${it.etaMinutes} min", color = Color.White)
                it.turnSteps.take(3).forEach { step -> Text("- $step", color = Color(0xFFB3E5FC)) }
            }
        }
    }
}

@Composable
private fun RouteScreen(
    state: ReceiverUiState,
    onSourceSelected: (String, Coordinate) -> Unit,
    onDestinationSelected: (String, Coordinate) -> Unit,
    onRoutePreview: (Coordinate) -> Unit,
    onNavigationStarted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourceQuery by remember { mutableStateOf(state.manualSourceLabel) }
    val sourceSuggestions = remember { mutableStateListOf<DestinationSuggestion>() }
    var destinationQuery by remember { mutableStateOf(state.destinationLabel) }
    val suggestions = remember { mutableStateListOf<DestinationSuggestion>() }
    var routeMessage by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
        if (state.coordinate == null) {
            OutlinedTextField(
                value = sourceQuery,
                onValueChange = { query ->
                    sourceQuery = query
                    routeMessage = ""
                    scope.launch {
                        sourceSuggestions.clear()
                        if (query.length >= 3) {
                            sourceSuggestions.addAll(searchDestinationSuggestions(query, context = context))
                        }
                    }
                },
                label = { Text("Source (car location)") },
                modifier = Modifier.fillMaxWidth()
            )
            sourceSuggestions.take(5).forEach { suggestion ->
                ListItem(
                    headlineContent = { Text(suggestion.title) },
                    supportingContent = { Text("${suggestion.coordinate.lat}, ${suggestion.coordinate.lon}") },
                    modifier = Modifier.clickable {
                        sourceQuery = suggestion.title
                        routeMessage = "Source selected as car location"
                        onSourceSelected(suggestion.title, suggestion.coordinate)
                        sourceSuggestions.clear()
                    }
                )
            }
        }

        OutlinedTextField(
            value = destinationQuery,
            onValueChange = { query ->
                destinationQuery = query
                routeMessage = ""
                scope.launch {
                    suggestions.clear()
                    if (query.length >= 3) {
                        suggestions.addAll(searchDestinationSuggestions(query, context = context))
                    }
                }
            },
            label = { Text("Search destination") },
            modifier = Modifier.fillMaxWidth()
        )
        suggestions.take(5).forEach { suggestion ->
            ListItem(
                headlineContent = { Text(suggestion.title) },
                supportingContent = { Text("${suggestion.coordinate.lat}, ${suggestion.coordinate.lon}") },
                modifier = Modifier.clickable {
                    destinationQuery = suggestion.title
                    routeMessage = "Destination selected"
                    onDestinationSelected(suggestion.title, suggestion.coordinate)
                    suggestions.clear()
                }
            )
        }
        if (routeMessage.isNotEmpty()) {
            Text(routeMessage, color = Color(0xFF2E7D32))
        }
        Button(
            onClick = {
                scope.launch {
                    val source = state.coordinate
                        ?: state.manualSource
                        ?: searchDestinationSuggestions(sourceQuery, context).firstOrNull()?.coordinate
                    if (source == null) {
                        routeMessage = "Set Source first when BLE location is unavailable."
                        return@launch
                    }
                    if (state.coordinate == null && state.manualSource == null) {
                        onSourceSelected(sourceQuery, source)
                    }

                    val destination = state.selectedDestination
                        ?: searchDestinationSuggestions(destinationQuery, context).firstOrNull()?.coordinate
                    if (destination == null) {
                        routeMessage = "No destination found. Try a more specific place."
                        return@launch
                    }
                    onDestinationSelected(destinationQuery, destination)
                    routeMessage = "Route generated"
                    onRoutePreview(destination)
                    onNavigationStarted()
                }
            }
        ) {
            Text("Start Navigation")
        }
        Text("Turn list and ETA", style = MaterialTheme.typography.titleMedium)
        state.routePreview?.let { route ->
            Text("Distance: ${route.distanceMeters}m")
            Text("ETA: ${route.etaMinutes} min")
            Text(route.turnSteps.joinToString("\n"))
        }
    }
}

private data class DestinationSuggestion(
    val title: String,
    val coordinate: Coordinate
)

private suspend fun searchDestinationSuggestions(
    query: String,
    context: android.content.Context
): List<DestinationSuggestion> = withContext(Dispatchers.IO) {
    val geocoder = Geocoder(context, Locale.getDefault())
    @Suppress("DEPRECATION")
    val addresses = geocoder.getFromLocationName(query, 5).orEmpty()
    addresses.mapNotNull { address ->
        val lat = address.latitude
        val lon = address.longitude
        if (lat.isNaN() || lon.isNaN()) return@mapNotNull null
        val fullLine = address.getAddressLine(0)?.takeIf { it.isNotBlank() }
        val fallback = listOfNotNull(
            address.featureName,
            address.subLocality,
            address.locality,
            address.adminArea,
            address.countryName
        ).distinct().joinToString(", ")
        val label = (fullLine ?: fallback).ifBlank { query }
        DestinationSuggestion(label, Coordinate(lat = lat, lon = lon))
    }
}

@Composable
private fun ConnectionScreen(state: ReceiverUiState) {
    val lastCoordinate = state.coordinate ?: state.manualSource
    val nowMs by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val updatedAgoSec = if (state.bleState.updatedAtMs > 0L) {
        max(0L, (nowMs - state.bleState.updatedAtMs) / 1000L)
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 16.dp)) {
        Text("BLE status: ${if (state.bleState.connected) "Connected" else "Disconnected"}")
        Text("Last message: ${state.bleState.lastMessage}")
        Text("Rate: ${"%.2f".format(state.bleState.streamRateHz)} Hz")
        Text("Latitude: ${lastCoordinate?.lat ?: "N/A"}")
        Text("Longitude: ${lastCoordinate?.lon ?: "N/A"}")
        Text("Updated: ${updatedAgoSec?.let { "$it sec ago" } ?: "No BLE updates yet"}")
        Button(onClick = {}) { Text("Reconnect") }
    }
}

@Composable
private fun SettingsScreen(
    state: ReceiverUiState,
    onSourceChanged: (CoordinateSource) -> Unit,
    onSafetyChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        ListItem(
            headlineContent = { Text("Use BLE as coordinate source") },
            supportingContent = { Text("Fallback to system GPS on BLE disconnect") },
            trailingContent = {
                Switch(
                    checked = state.source == CoordinateSource.BLE_STREAM,
                    onCheckedChange = {
                        onSourceChanged(if (it) CoordinateSource.BLE_STREAM else CoordinateSource.SYSTEM_GPS)
                    }
                )
            }
        )
        ListItem(
            headlineContent = { Text("Safety mode") },
            supportingContent = { Text("Lower update frequency and network intensity") },
            trailingContent = { Switch(checked = state.safetyMode, onCheckedChange = onSafetyChanged) }
        )
        Text("Map provider: Google (Mapbox stub ready)")
        Text("Offline tile download: planned flow with progress UI")
    }
}

private fun calculateZoomFromSpeed(speedMps: Double): Float {
    // Convert m/s to km/h for reference: speedMps * 3.6 = km/h
    // Speed-based zoom scaling:
    // 0-5 m/s (0-18 km/h, urban) = zoom 16 (closest)
    // 5-10 m/s (18-36 km/h, suburban) = zoom 14
    // 10-20 m/s (36-72 km/h, highway) = zoom 12
    // 20+ m/s (72+ km/h, fast highway) = zoom 10
    return when {
        speedMps < 5.0 -> 16f
        speedMps < 10.0 -> 14f
        speedMps < 20.0 -> 12f
        else -> 10f
    }
}

private fun calculateBearingFromRoute(current: Coordinate, polyline: List<Coordinate>): Double {
    if (polyline.isEmpty()) return 0.0
    
    // Find the closest waypoint on the route ahead of the vehicle
    val nextWaypoint = findNextWaypointAhead(current, polyline) ?: polyline.lastOrNull() ?: return 0.0
    
    // Calculate bearing from current position to next waypoint
    return calculateBearing(current.lat, current.lon, nextWaypoint.lat, nextWaypoint.lon)
}

private fun findNextWaypointAhead(current: Coordinate, polyline: List<Coordinate>): Coordinate? {
    if (polyline.size < 2) return polyline.firstOrNull()
    
    // Find the closest point on the polyline
    var minDistance = Double.MAX_VALUE
    var closestIndex = 0
    
    for (i in polyline.indices) {
        val dist = haversineDistance(current.lat, current.lon, polyline[i].lat, polyline[i].lon)
        if (dist < minDistance) {
            minDistance = dist
            closestIndex = i
        }
    }
    
    // Return the waypoint ahead (or last if at end)
    return polyline.getOrNull(closestIndex + 1) ?: polyline.lastOrNull()
}

private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = kotlin.math.sin(dLon) * kotlin.math.cos(Math.toRadians(lat2))
    val x = kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.sin(Math.toRadians(lat2)) -
            kotlin.math.sin(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.cos(dLon)
    var bearing = Math.toDegrees(kotlin.math.atan2(y, x))
    bearing = (bearing + 360.0) % 360.0
    return bearing
}

private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).pow(2) + 
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * 
            kotlin.math.sin(dLon / 2).pow(2)
    return 2 * r * kotlin.math.asin(kotlin.math.sqrt(a))
}

