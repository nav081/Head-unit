package com.example.blegps.navigation

import com.example.blegps.ble.Coordinate
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class RoutePreview(
    val distanceMeters: Int,
    val etaMinutes: Int,
    val turnSteps: List<String>
)

interface NavigationEngine {
    suspend fun previewRoute(origin: Coordinate, destination: Coordinate): RoutePreview
}

class CachedDirectionsEngine : NavigationEngine {
    override suspend fun previewRoute(origin: Coordinate, destination: Coordinate): RoutePreview {
        val distance = haversineMeters(origin.lat, origin.lon, destination.lat, destination.lon).toInt()
        val eta = (distance / 12.5 / 60.0).coerceAtLeast(1.0).toInt()
        return RoutePreview(
            distanceMeters = distance,
            etaMinutes = eta,
            turnSteps = listOf(
                "Head toward destination",
                "Continue on suggested corridor",
                "Arrive at destination"
            )
        )
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
