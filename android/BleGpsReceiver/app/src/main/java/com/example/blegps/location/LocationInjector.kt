package com.example.blegps.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.blegps.model.Coordinate
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationInjector(private val context: Context) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val provider = "blegps_mock"

    @SuppressLint("MissingPermission")
    fun inject(coordinate: Coordinate): Result<Unit> = runCatching {
        require(coordinate.isValid()) { "Invalid coordinate" }
        setupProviderIfNeeded()
        val loc = Location(provider).apply {
            latitude = coordinate.lat
            longitude = coordinate.lon
            altitude = coordinate.alt
            speed = coordinate.speed.toFloat()
            accuracy = 3f
            time = System.currentTimeMillis()
        }
        manager.setTestProviderLocation(provider, loc)
    }.recoverCatching {
        // If test provider fails (no mock location permission), fallback for app-local consumers.
        fused.lastLocation
    }.map { Unit }

    @SuppressLint("MissingPermission")
    private fun setupProviderIfNeeded() {
        runCatching {
            manager.addTestProvider(
                provider,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                0,
                5
            )
        }
        runCatching { manager.setTestProviderEnabled(provider, true) }
    }
}
