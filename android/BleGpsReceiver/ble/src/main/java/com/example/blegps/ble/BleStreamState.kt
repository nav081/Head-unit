package com.example.blegps.ble

data class BleStreamState(
    val connected: Boolean = false,
    val lastMessage: String = "",
    val streamRateHz: Double = 0.0,
    val updatedAtMs: Long = System.currentTimeMillis()
)
