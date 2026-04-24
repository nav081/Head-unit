package com.example.blegps.ble

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.min

class BleReceiverService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var gatt: BluetoothGatt? = null
    private var reconnectDelayMs = 1000L
    private var lastPacketAtMs = 0L

    private val serviceUuid = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb")
    private val characteristicUuid = UUID.fromString("0000beef-0000-1000-8000-00805f9b34fb")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1001, buildNotification("Waiting for iOS stream"))
        connectWithRetry()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { gatt?.close() }
        scope.cancel()
        super.onDestroy()
    }

    private fun connectWithRetry() {
        scope.launch {
            val manager = getSystemService(BluetoothManager::class.java)
            val adapter = manager?.adapter ?: return@launch
            val device = findDevice(adapter) ?: return@launch
            gatt = device.connectGatt(this@BleReceiverService, false, callback)
        }
    }

    private fun findDevice(adapter: BluetoothAdapter) =
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        ) adapter.bondedDevices.firstOrNull() else null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val connected = newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED
            BleCoordinateBus.publishState(BleCoordinateBus.streamState.value.copy(connected = connected))
            if (connected) {
                reconnectDelayMs = 1000L
                if (ActivityCompat.checkSelfPermission(
                        this@BleReceiverService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.discoverServices()
                }
            } else {
                scope.launch {
                    delay(reconnectDelayMs)
                    reconnectDelayMs = min(reconnectDelayMs * 2, 32000L)
                    connectWithRetry()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val c = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid) ?: return
            if (ActivityCompat.checkSelfPermission(
                    this@BleReceiverService,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gatt.setCharacteristicNotification(c, true)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val payload = characteristic.value?.toString(Charsets.UTF_8) ?: return
            val parsed = PayloadParser.parse(payload) ?: return
            val now = System.currentTimeMillis()
            val deltaMs = if (lastPacketAtMs == 0L) 0L else now - lastPacketAtMs
            lastPacketAtMs = now
            val rateHz = if (deltaMs > 0) 1000.0 / deltaMs.toDouble() else 0.0
            BleCoordinateBus.publishCoordinate(parsed)
            BleCoordinateBus.publishState(
                BleCoordinateBus.streamState.value.copy(
                    connected = true,
                    lastMessage = payload.take(180),
                    streamRateHz = rateHz,
                    updatedAtMs = now
                )
            )
            if (LocationBridge.selectedSource == CoordinateSource.BLE_STREAM) {
                startForeground(1001, buildNotification("BLE driving app location"))
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("blegps", "BLE GPS Receiver", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, "blegps")
            .setContentTitle("Headunit Navigation")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
}
