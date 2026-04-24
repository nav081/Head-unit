package com.example.blegps.model

data class Coordinate(
    val lat: Double,
    val lon: Double,
    val alt: Double,
    val speed: Double,
    val timestamp: Long
) {
    fun isValid(): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }
}
