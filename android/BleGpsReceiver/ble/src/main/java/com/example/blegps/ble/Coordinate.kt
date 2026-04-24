package com.example.blegps.ble

data class Coordinate(
    val lat: Double,
    val lon: Double,
    val alt: Double = 0.0,
    val speedMps: Double = 0.0,
    val headingDeg: Float = 0f,
    val timestampSec: Long = System.currentTimeMillis() / 1000
) {
    fun isValid(): Boolean = lat in -90.0..90.0 && lon in -180.0..180.0
}
