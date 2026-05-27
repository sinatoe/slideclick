package io.github.sinatoe.slideclick.clicker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.sinatoe.slideclick.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("MissingPermission")
class ClickerService : Service() {
    inner class LocalBinder : Binder() {
        fun getService(): ClickerService = this@ClickerService
    }

    private val binder = LocalBinder()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val clickerCommand = MutableSharedFlow<ClickerCommand>(extraBufferCapacity = 1)

    val status = flow { emit(bluetoothAdapter) }
        .filterNotNull()
        .flatMapLatest { adapter ->
            callbackFlow {
                trySend(null)

                var currentProxy: BluetoothHidDevice? = null

                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            currentProxy = (proxy as BluetoothHidDevice).also { trySend(it) }
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HID_DEVICE) {
                            currentProxy = null
                            trySend(null)
                        }
                    }
                }

                adapter.getProfileProxy(applicationContext, listener, BluetoothProfile.HID_DEVICE)

                awaitClose {
                    adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, currentProxy)
                }
            }
        }
        .flatMapLatest { proxy ->
            if (proxy != null) {
                callbackFlow {
                    val callback = object : BluetoothHidDevice.Callback() {
                        override fun onAppStatusChanged(
                            pluggedDevice: BluetoothDevice?,
                            registered: Boolean,
                        ) {
                            if (registered && pluggedDevice != null) {
                                val connectionState = proxy.getConnectionState(pluggedDevice)

                                if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                                    proxy.connect(pluggedDevice)
                                }
                            }
                        }

                        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                            when (state) {
                                BluetoothProfile.STATE_CONNECTED -> trySend(device)
                                BluetoothProfile.STATE_DISCONNECTED -> trySend(null)
                            }
                        }
                    }

                    val sdp = BluetoothHidDeviceAppSdpSettings(
                        "Slideclick",
                        "Virtual keyboard",
                        "Android",
                        BluetoothHidDevice.SUBCLASS1_KEYBOARD,
                        KEYBOARD_DESCRIPTOR
                            .map { it.toByte() }
                            .toByteArray(),
                    )

                    val executor = Executors.newSingleThreadExecutor()

                    proxy.registerApp(sdp, null, null, executor, callback)

                    awaitClose {
                        proxy.unregisterApp()
                        executor.shutdown()
                    }
                }
                    .transformLatest { device ->
                        emit(device)

                        if (device != null) {
                            clickerCommand.collect { command ->
                                val keyCode = when (command) {
                                    ClickerCommand.BACK -> KEY_PAGE_UP
                                    ClickerCommand.FORWARD -> KEY_PAGE_DOWN
                                }

                                proxy.sendReport(
                                    device,
                                    REPORT_ID_KEYBOARD,
                                    ByteArray(8).apply { set(2, keyCode.toByte()) },
                                )

                                proxy.sendReport(
                                    device,
                                    REPORT_ID_KEYBOARD,
                                    ByteArray(8),
                                )
                            }
                        }
                    }
                    .map { device ->
                        if (device != null) {
                            ClickerStatus.Connected(device.name)
                        } else {
                            ClickerStatus.Idle
                        }
                    }
            } else {
                flowOf(ClickerStatus.Idle)
            }
        }
        .stateIn(
            scope = serviceScope,
            started = SharingStarted.Lazily,
            initialValue = ClickerStatus.Unsupported,
        )

    fun sendCommand(command: ClickerCommand) {
        clickerCommand.tryEmit(command)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            CONNECTION_NOTIFICATION_CHANNEL_ID,
            "Active connection",
            NotificationManager.IMPORTANCE_LOW,
        )

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (bluetoothAdapter == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CONNECTION_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Maintaining Bluetooth connection")
            .setSmallIcon(R.drawable.ic_notification_small)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                CONNECTION_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(
                CONNECTION_NOTIFICATION_ID,
                notification,
            )
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val CONNECTION_NOTIFICATION_CHANNEL_ID = "active_connection"
        private const val CONNECTION_NOTIFICATION_ID = 1001

        private const val REPORT_ID_KEYBOARD = 1

        private const val KEY_PAGE_UP = 0x4B
        private const val KEY_PAGE_DOWN = 0x4E

        private val KEYBOARD_DESCRIPTOR = intArrayOf(
            0x05, 0x01,
            0x09, 0x06,
            0xA1, 0x01,
            0x85, 0x01,
            0x05, 0x07,
            0x19, 0xE0,
            0x29, 0xE7,
            0x15, 0x00,
            0x25, 0x01,
            0x75, 0x01,
            0x95, 0x08,
            0x81, 0x02,
            0x95, 0x01,
            0x75, 0x08,
            0x81, 0x03,
            0x95, 0x05,
            0x75, 0x01,
            0x05, 0x08,
            0x19, 0x01,
            0x29, 0x05,
            0x91, 0x02,
            0x95, 0x01,
            0x75, 0x03,
            0x91, 0x03,
            0x95, 0x06,
            0x75, 0x08,
            0x15, 0x00,
            0x25, 0x65,
            0x05, 0x07,
            0x19, 0x00,
            0x29, 0x65,
            0x81, 0x00,
            0xC0,
        )
    }
}
