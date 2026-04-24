package com.example.blegps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blegps.ble.BleCoordinateBus
import com.example.blegps.ble.BleStreamState
import com.example.blegps.ble.Coordinate
import com.example.blegps.ble.CoordinateSource
import com.example.blegps.ble.LocationBridge
import com.example.blegps.navigation.CachedDirectionsEngine
import com.example.blegps.navigation.NavigationEngine
import com.example.blegps.navigation.RoutePreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ReceiverUiState(
    val coordinate: Coordinate? = null,
    val manualSource: Coordinate? = null,
    val manualSourceLabel: String = "",
    val bleState: BleStreamState = BleStreamState(),
    val source: CoordinateSource = CoordinateSource.BLE_STREAM,
    val selectedDestination: Coordinate? = null,
    val destinationLabel: String = "",
    val routePolyline: List<Coordinate> = emptyList(),
    val routePreview: RoutePreview? = null,
    val isNavigating: Boolean = false,
    val safetyMode: Boolean = false
)

class ReceiverViewModel(
    private val navigationEngine: NavigationEngine = CachedDirectionsEngine()
) : ViewModel() {
    private val _state = MutableStateFlow(ReceiverUiState())
    val state: StateFlow<ReceiverUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(BleCoordinateBus.coordinate, BleCoordinateBus.streamState) { coord, stream ->
                coord to stream
            }.collect { (coord, stream) ->
                _state.value = _state.value.copy(coordinate = coord, bleState = stream)
            }
        }
    }

    fun setSource(source: CoordinateSource) {
        LocationBridge.selectSource(source)
        _state.value = _state.value.copy(source = source)
    }

    fun setSafetyMode(enabled: Boolean) {
        _state.value = _state.value.copy(safetyMode = enabled)
    }

    fun previewRoute(destination: Coordinate) {
        val origin = _state.value.coordinate ?: _state.value.manualSource ?: return
        viewModelScope.launch {
            val mapsApiKey = readMapsApiKey()
            val directionsRoute = mapsApiKey?.let { fetchDirectionsRoute(origin, destination, it) }
            val fallbackPreview = navigationEngine.previewRoute(origin, destination)
            _state.value = _state.value.copy(
                selectedDestination = destination,
                routePolyline = directionsRoute?.polyline ?: listOf(origin, destination),
                routePreview = directionsRoute?.preview ?: fallbackPreview,
                isNavigating = true
            )
        }
    }

    fun setDestination(label: String, destination: Coordinate) {
        _state.value = _state.value.copy(
            destinationLabel = label,
            selectedDestination = destination
        )
    }

    fun setManualSource(label: String, source: Coordinate) {
        _state.value = _state.value.copy(
            manualSourceLabel = label,
            manualSource = source
        )
    }

    fun stopNavigation() {
        _state.value = _state.value.copy(isNavigating = false)
    }

    private fun readMapsApiKey(): String? {
        val configured = try {
            Class.forName("com.example.blegps.BuildConfig")
                .getField("MAPS_API_KEY")
                .get(null) as? String
        } catch (_: Throwable) {
            null
        }
        return configured?.takeIf { it.isNotBlank() && it != "REPLACE_WITH_API_KEY" }
    }

    private suspend fun fetchDirectionsRoute(
        origin: Coordinate,
        destination: Coordinate,
        apiKey: String
    ): DirectionsRoute? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val originParam = "${origin.lat},${origin.lon}"
        val destinationParam = "${destination.lat},${destination.lon}"
        val url = URL(
            "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${URLEncoder.encode(originParam, "UTF-8")}&" +
                "destination=${URLEncoder.encode(destinationParam, "UTF-8")}&" +
                "mode=driving&key=${URLEncoder.encode(apiKey, "UTF-8")}"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        runCatching {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            parseDirections(response)
        }.getOrNull()
    }

    private fun parseDirections(raw: String): DirectionsRoute? {
        val root = JSONObject(raw)
        val routes = root.optJSONArray("routes") ?: return null
        if (routes.length() == 0) return null
        val route = routes.getJSONObject(0)
        val legs = route.optJSONArray("legs") ?: return null
        if (legs.length() == 0) return null
        val leg = legs.getJSONObject(0)
        val meters = leg.optJSONObject("distance")?.optInt("value") ?: return null
        val seconds = leg.optJSONObject("duration")?.optInt("value") ?: 0
        val polylineEncoded = route.optJSONObject("overview_polyline")?.optString("points").orEmpty()
        val points = decodePolyline(polylineEncoded).ifEmpty { return null }
        val steps = mutableListOf<String>()
        val stepArray = leg.optJSONArray("steps")
        if (stepArray != null) {
            for (i in 0 until minOf(3, stepArray.length())) {
                val html = stepArray.getJSONObject(i).optString("html_instructions")
                steps.add(html.replace(Regex("<.*?>"), "").trim())
            }
        }
        return DirectionsRoute(
            polyline = points,
            preview = RoutePreview(
                distanceMeters = meters,
                etaMinutes = (seconds / 60).coerceAtLeast(1),
                turnSteps = if (steps.isEmpty()) listOf("Follow suggested route") else steps
            )
        )
    }

    private fun decodePolyline(encoded: String): List<Coordinate> {
        val poly = mutableListOf<Coordinate>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(Coordinate(lat = lat / 1E5, lon = lng / 1E5))
        }
        return poly
    }

    private data class DirectionsRoute(
        val polyline: List<Coordinate>,
        val preview: RoutePreview
    )
}
