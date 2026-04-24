package com.example.blegps.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.blegps.ble.BleReceiverService
import com.example.blegps.debug.DebugRuntimeLogger
import com.example.blegps.screens.ReceiverApp

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Start the service regardless of individual results.
        // The service itself will check which permissions are granted
        // and adjust its foreground type accordingly.
        startReceiverService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // #region agent log
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            DebugRuntimeLogger.log(
                runId = "pre-fix",
                hypothesisId = "H5",
                location = "MainActivity.kt:onCreate",
                message = "Uncaught exception",
                data = """{"type":"${throwable.javaClass.name}","message":"${throwable.message ?: ""}"}"""
            )
        }
        // #endregion
        // #region agent log
        DebugRuntimeLogger.log(
            runId = "pre-fix",
            hypothesisId = "H1",
            location = "MainActivity.kt:onCreate",
            message = "MainActivity onCreate entered",
            data = """{"savedInstanceStateNull":${savedInstanceState == null}}"""
        )
        // #endregion

        checkAndRequestPermissions()

        // #region agent log
        setContent {
            DebugRuntimeLogger.log(
                runId = "pre-fix",
                hypothesisId = "H5",
                location = "MainActivity.kt:setContent",
                message = "Compose content initialized",
                data = """{}"""
            )
            ReceiverApp()
        }
        // #endregion
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startReceiverService()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startReceiverService() {
        runCatching {
            startForegroundService(Intent(this, BleReceiverService::class.java))
            DebugRuntimeLogger.log(
                runId = "pre-fix",
                hypothesisId = "H1",
                location = "MainActivity.kt:startReceiverService",
                message = "startForegroundService succeeded",
                data = """{}"""
            )
        }.onFailure { err ->
            DebugRuntimeLogger.log(
                runId = "pre-fix",
                hypothesisId = "H1",
                location = "MainActivity.kt:startReceiverService",
                message = "startForegroundService failed",
                data = """{"type":"${err.javaClass.name}","message":"${err.message ?: ""}"}"""
            )
        }
    }
}
