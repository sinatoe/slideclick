package io.github.sinatoe.slideclick.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sinatoe.slideclick.domain.ClickerCommand
import io.github.sinatoe.slideclick.domain.ClickerConnection
import io.github.sinatoe.slideclick.domain.ClickerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClickerViewModel(
    connectionFlow: Flow<ClickerConnection?>,
) : ViewModel() {
    private val permissionGranted = MutableSharedFlow<Unit>(replay = 1)

    private val connection = permissionGranted
        .take(1)
        .flatMapLatest { connectionFlow }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val status = permissionGranted
        .take(1)
        .flatMapLatest { connection }
        .flatMapLatest { it?.status ?: flowOf(ClickerStatus.Disconnected) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ClickerStatus.MissingPermission,
        )

    fun notifyPermissionGranted() {
        permissionGranted.tryEmit(Unit)
    }

    fun sendCommand(command: ClickerCommand) {
        connection.value?.sendCommand(command)
    }
}
