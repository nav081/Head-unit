package com.example.blegps.ble

import com.example.blegps.model.Coordinate
import org.json.JSONObject

object PayloadParser {
    fun parse(payload: String): Coordinate? {
        val trimmed = payload.trim()
        return if (trimmed.startsWith("{")) parseJson(trimmed) else parseNmea(trimmed)
    }

    private fun parseJson(json: String): Coordinate? = runCatching {
        val obj = JSONObject(json)
        Coordinate(
            lat = obj.getDouble("lat"),
            lon = obj.getDouble("lon"),
            alt = obj.optDouble("alt", 0.0),
            speed = obj.optDouble("speed", 0.0),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis() / 1000)
        )
    }.getOrNull()?.takeIf { it.isValid() }

    private fun parseNmea(nmea: String): Coordinate? {
        if (!nmea.startsWith("\$GPRMC")) return null
        val parts = nmea.split(",")
        if (parts.size < 8 || parts[2] != "A") return null
        val lat = parseCoord(parts[3], parts[4], true) ?: return null
        val lon = parseCoord(parts[5], parts[6], false) ?: return null
        val speedKnots = parts[7].toDoubleOrNull() ?: 0.0
        return Coordinate(
            lat = lat,
            lon = lon,
            alt = 0.0,
            speed = speedKnots * 0.514444,
            timestamp = System.currentTimeMillis() / 1000
        ).takeIf { it.isValid() }
    }

    private fun parseCoord(raw: String, hemi: String, isLat: Boolean): Double? {
        val degreeDigits = if (isLat) 2 else 3
        if (raw.length <= degreeDigits) return null
        val deg = raw.take(degreeDigits).toDoubleOrNull() ?: return null
        val mins = raw.drop(degreeDigits).toDoubleOrNull() ?: return null
        val value = deg + (mins / 60.0)
        return when (hemi) {
            "S", "W" -> -value
            "N", "E" -> value
            else -> null
        }
    }
}
