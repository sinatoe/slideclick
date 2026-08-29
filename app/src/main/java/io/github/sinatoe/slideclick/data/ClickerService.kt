package io.github.sinatoe.slideclick.data

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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.sinatoe.slideclick.R
import io.github.sinatoe.slideclick.domain.ClickerCommand
import io.github.sinatoe.slideclick.domain.ClickerConnection
import io.github.sinatoe.slideclick.domain.ClickerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

private const val CONNECTION_NOTIFICATION_CHANNEL_ID = "connection_service"
private const val CONNECTION_NOTIFICATION_ID = 1001

private const val SDP_NAME = "Slideclick"
private const val SDP_DESCRIPTION = "Virtual keyboard"
private const val SDP_PROVIDER = "Android"

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

@OptIn(ExperimentalCoroutinesApi::class)
@SuppressLint("MissingPermission")
class ClickerService : Service(), ClickerConnection {
    inner class LocalBinder : Binder() {
        fun getService(): ClickerService = this@ClickerService
    }

    private val binder = LocalBinder()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        getSystemService<BluetoothManager>()?.adapter
    }

    private val clickerCommand = MutableSharedFlow<ClickerCommand>(extraBufferCapacity = 1)

    override val status = flow { emit(bluetoothAdapter) }
        .filterNotNull()
        .flatMapLatest { adapter -> adapter.hidDeviceProxyFlow(applicationContext) }
        .flatMapLatest { proxy ->
            if (proxy == null) {
                return@flatMapLatest flowOf<ClickerStatus>(ClickerStatus.Disconnected)
            }

            proxy.connectedDeviceFlow().transformLatest { device ->
                if (device == null) {
                    return@transformLatest emit(ClickerStatus.Disconnected)
                }

                emit(ClickerStatus.Connected(device.name))

                clickerCommand.collectAsHidReports(proxy, device)
            }
        }
        .stateIn(
            scope = serviceScope,
            started = SharingStarted.Lazily,
            initialValue = ClickerStatus.Unsupported,
        )

    override fun sendCommand(command: ClickerCommand) {
        clickerCommand.tryEmit(command)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            CONNECTION_NOTIFICATION_CHANNEL_ID,
            getString(R.string.service_notification_channel_name),
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
            .setContentTitle(getString(R.string.service_notification_title))
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

        return START_NOT_STICKY
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
        fun connectionFlow(context: Context): Flow<ClickerConnection?> = callbackFlow {
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    trySend((binder as LocalBinder).getService())
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    trySend(null)
                }
            }

            val intent = Intent(context, ClickerService::class.java)

            ContextCompat.startForegroundService(context, intent)
            context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)

            awaitClose {
                context.unbindService(serviceConnection)
                context.stopService(intent)
            }
        }
    }
}

private fun BluetoothAdapter.hidDeviceProxyFlow(context: Context): Flow<BluetoothHidDevice?> =
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

        getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)

        awaitClose {
            closeProfileProxy(BluetoothProfile.HID_DEVICE, currentProxy)
        }
    }

@SuppressLint("MissingPermission")
private fun BluetoothHidDevice.connectedDeviceFlow(): Flow<BluetoothDevice?> = callbackFlow {
    val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(
            pluggedDevice: BluetoothDevice?,
            registered: Boolean,
        ) {
            if (registered && pluggedDevice != null) {
                val connectionState = getConnectionState(pluggedDevice)

                if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                    connect(pluggedDevice)
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
        SDP_NAME,
        SDP_DESCRIPTION,
        SDP_PROVIDER,
        BluetoothHidDevice.SUBCLASS1_KEYBOARD,
        KEYBOARD_DESCRIPTOR
            .map { it.toByte() }
            .toByteArray(),
    )

    registerApp(sdp, null, null, Dispatchers.Default.limitedParallelism(1).asExecutor(), callback)

    awaitClose {
        unregisterApp()
    }
}

@SuppressLint("MissingPermission")
private suspend fun Flow<ClickerCommand>.collectAsHidReports(
    proxy: BluetoothHidDevice,
    device: BluetoothDevice,
) {
    collect { command ->
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
