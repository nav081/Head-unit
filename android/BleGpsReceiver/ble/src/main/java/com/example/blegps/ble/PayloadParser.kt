package com.example.blegps.ble

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
            speedMps = obj.optDouble("speed", 0.0),
            headingDeg = obj.optDouble("heading", 0.0).toFloat(),
            timestampSec = obj.optLong("timestamp", System.currentTimeMillis() / 1000)
        )
    }.getOrNull()?.takeIf { it.isValid() }

    private fun parseNmea(nmea: String): Coordinate? {
        if (!nmea.startsWith("\$GPRMC")) return null
        val parts = nmea.split(",")
        if (parts.size < 9 || parts[2] != "A") return null
        val lat = parseCoord(parts[3], parts[4], isLat = true) ?: return null
        val lon = parseCoord(parts[5], parts[6], isLat = false) ?: return null
        val speedMps = (parts[7].toDoubleOrNull() ?: 0.0) * 0.514444
        val heading = parts[8].toFloatOrNull() ?: 0f
        return Coordinate(lat = lat, lon = lon, speedMps = speedMps, headingDeg = heading)
            .takeIf { it.isValid() }
    }

    private fun parseCoord(raw: String, hemi: String, isLat: Boolean): Double? {
        val degreeDigits = if (isLat) 2 else 3
        if (raw.length <= degreeDigits) return null
        val deg = raw.take(degreeDigits).toDoubleOrNull() ?: return null
        val mins = raw.drop(degreeDigits).toDoubleOrNull() ?: return null
        val value = deg + mins / 60.0
        return when (hemi) {
            "N", "E" -> value
            "S", "W" -> -value
            else -> null
        }
    }
}
