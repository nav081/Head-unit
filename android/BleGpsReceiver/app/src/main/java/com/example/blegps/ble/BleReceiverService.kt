package com.example.blegps.ble

import android.Manifest
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
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.blegps.debug.DebugRuntimeLogger
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
        // #region agent log
        DebugRuntimeLogger.log(
            runId = "pre-fix",
            hypothesisId = "H2",
            location = "BleReceiverService.kt:onCreate",
            message = "Service onCreate entered",
            data = """{"sdkInt":${android.os.Build.VERSION.SDK_INT}}"""
        )
        // #endregion
        runCatching {
            injector = LocationInjector(this)
            // #region agent log
            DebugRuntimeLogger.log(
                runId = "pre-fix",
                hypothesisId = "H3",
                location = "BleReceiverService.kt:onCreate",
                message = "LocationInjector initialized",
                data = """{}"""
            )
            // #endregion
            createNotificationChannel()
            
            val hasLocation = ActivityCompat.checkSelfPermission(
                this@BleReceiverService,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this@BleReceiverService,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val hasBluetoothConnect = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ActivityCompat.checkSelfPermission(
                    this@BleReceiverService,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else true

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fgTypes = buildList {
                    if (hasLocation) add(android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                    if (hasBluetoothConnect) add(android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
                }.fold(0) { acc, type -> acc or type }

                val finalTypes = if (fgTypes != 0) fgTypes else android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                startForeground(1001, buildNotification("Receiver active"), finalTypes)
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val fgType = if (hasLocation) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
                if (fgType != 0) {
                    startForeground(1001, buildNotification("Receiver active"), fgType)
                } else {
                    startForeground(1001, buildNotification("Receiver active"))
                }
            } else {
                startForeground(1001, buildNotification("Receiver active"))
            }
            // #region agent log
            DebugRuntimeLogger.log(
                runId = "pre-fix",
                hypothesisId = "H2",
                location = "BleReceiverService.kt:onCreate",
                message = "startForeground succeeded",
                data = """{}"""
            )
            // #endregion
            connectWithRetry()
        }.onFailure { err ->
            // #region agent log
            DebugRuntimeLogger.log(
                runId = "pre-fix",
                hypothesisId = "H2",
                location = "BleReceiverService.kt:onCreate",
                message = "Service initialization failed",
                data = """{"type":"${err.javaClass.name}","message":"${err.message ?: ""}"}"""
            )
            // #endregion
            throw err
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gatt?.close()
            }
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectWithRetry() {
        scope.launch {
            val manager = getSystemService(BluetoothManager::class.java)
            val adapter = manager?.adapter ?: return@launch
            val device = findDevice(adapter) ?: return@launch
            withContext(Dispatchers.Main) {
                if (ActivityCompat.checkSelfPermission(
                        this@BleReceiverService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@withContext
                }
                gatt = device.connectGatt(this@BleReceiverService, false, callback)
            }
        }
    }

    private fun findDevice(adapter: BluetoothAdapter): BluetoothDevice? {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null
        }
        return adapter.bondedDevices.firstOrNull()
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                reconnectDelayMs = 1000L
                if (ActivityCompat.checkSelfPermission(
                        this@BleReceiverService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
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
            if (ActivityCompat.checkSelfPermission(
                    this@BleReceiverService,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
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
