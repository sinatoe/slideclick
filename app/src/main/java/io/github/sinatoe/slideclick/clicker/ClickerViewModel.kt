package io.github.sinatoe.slideclick.clicker

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClickerViewModel(application: Application) : AndroidViewModel(application) {
    private val service = callbackFlow {
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                trySend((binder as ClickerService.LocalBinder).getService())
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                trySend(null)
            }
        }

        val intent = Intent(application, ClickerService::class.java)

        ContextCompat.startForegroundService(application, intent)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        awaitClose {
            application.unbindService(serviceConnection)
            application.stopService(intent)
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.minutes),
            initialValue = null,
        )

    val status = service
        .flatMapLatest { it?.status ?: flowOf(ClickerStatus.Idle) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ClickerStatus.Idle,
        )

    fun sendCommand(command: ClickerCommand) {
        service.value?.sendCommand(command)
    }
}
