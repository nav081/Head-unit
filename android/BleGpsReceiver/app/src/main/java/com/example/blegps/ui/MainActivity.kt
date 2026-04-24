package com.example.blegps.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.blegps.ble.BleReceiverService
import com.example.blegps.screens.ReceiverApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, BleReceiverService::class.java))
        setContent { ReceiverApp() }
    }
}
