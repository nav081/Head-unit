package com.example.blegps.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.blegps.location.LocationInjector
import kotlinx.coroutines.*
import java.util.UUID
import kotlin.math.min

class BleReceiverService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var injector: LocationInjector
    private var gatt: BluetoothGatt? = null
    private var reconnectDelayMs = 1000L

    private val serviceUuid = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb")
    private val characteristicUuid = UUID.fromString("0000beef-0000-1000-8000-00805f9b34fb")

    override fun onCreate() {
        super.onCreate()
        injector = LocationInjector(this)
        createNotificationChannel()
        startForeground(1001, buildNotification("Receiver active"))
        connectWithRetry()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        gatt?.close()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectWithRetry() {
        scope.launch {
            val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            val device = findDevice(adapter) ?: return@launch
            withContext(Dispatchers.Main) {
                gatt = device.connectGatt(this@BleReceiverService, false, callback)
            }
        }
    }

    private fun findDevice(adapter: BluetoothAdapter): BluetoothDevice? {
        return adapter.bondedDevices.firstOrNull()
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                reconnectDelayMs = 1000L
                gatt.discoverServices()
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
            gatt.setCharacteristicNotification(c, true)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val payload = characteristic.value?.toString(Charsets.UTF_8) ?: return
            val parsed = PayloadParser.parse(payload) ?: return
            injector.inject(parsed)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("blegps", "BLE GPS Receiver", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "blegps")
            .setContentTitle("BLE GPS Receiver")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }
}
