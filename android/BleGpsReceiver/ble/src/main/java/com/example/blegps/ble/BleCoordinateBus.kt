package com.example.blegps.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BleCoordinateBus {
    private val _coordinate = MutableStateFlow<Coordinate?>(null)
    val coordinate: StateFlow<Coordinate?> = _coordinate.asStateFlow()

    private val _streamState = MutableStateFlow(BleStreamState())
    val streamState: StateFlow<BleStreamState> = _streamState.asStateFlow()

    fun publishCoordinate(value: Coordinate) {
        _coordinate.value = value
    }

    fun publishState(value: BleStreamState) {
        _streamState.value = value
    }
}
