package com.example.blegps.simulation

import com.example.blegps.model.Coordinate
import kotlin.math.cos
import kotlin.math.sin

object SimulatedRouteGenerator {
    fun generateLoop(startLat: Double, startLon: Double, steps: Int): List<Coordinate> {
        if (steps <= 0) return emptyList()
        return (0 until steps).map { idx ->
            val angle = (idx.toDouble() / steps.toDouble()) * 2.0 * Math.PI
            Coordinate(
                lat = startLat + 0.002 * cos(angle),
                lon = startLon + 0.002 * sin(angle),
                alt = 20.0,
                speed = 8.0 + sin(angle),
                timestamp = System.currentTimeMillis() / 1000 + idx
            )
        }
    }
}
