package io.github.sinatoe.slideclick.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sinatoe.slideclick.domain.ClickerCommand
import io.github.sinatoe.slideclick.domain.ClickerConnection
import io.github.sinatoe.slideclick.domain.ClickerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClickerViewModel(
    context: Context,
    connectionFlow: Flow<ClickerConnection?>,
) : ViewModel() {
    private val isPermissionGranted = MutableStateFlow(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        },
    )

    private val connection = connectionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2.minutes),
            initialValue = null,
        )

    val status = isPermissionGranted
        .flatMapLatest { granted ->
            if (granted) {
                connection.flatMapLatest { it?.status ?: flowOf(ClickerStatus.Disconnected) }
            } else {
                flowOf(ClickerStatus.MissingPermission)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ClickerStatus.Disconnected,
        )

    fun notifyPermissionGranted() {
        isPermissionGranted.value = true
    }

    fun sendCommand(command: ClickerCommand) {
        connection.value?.sendCommand(command)
    }
}
