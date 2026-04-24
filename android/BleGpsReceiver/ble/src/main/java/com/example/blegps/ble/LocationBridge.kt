package com.example.blegps.ble

enum class CoordinateSource {
    BLE_STREAM,
    SYSTEM_GPS
}

object LocationBridge {
    @Volatile
    var selectedSource: CoordinateSource = CoordinateSource.BLE_STREAM

    fun selectSource(source: CoordinateSource) {
        selectedSource = source
    }
}
