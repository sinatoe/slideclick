package io.github.sinatoe.slideclick.ui

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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClickerViewModel(
    connectionFlow: Flow<ClickerConnection?>,
) : ViewModel() {
    private val isPermissionGranted = MutableStateFlow(false)

    private val connection = connectionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2.minutes),
            initialValue = null,
        )

    val status = isPermissionGranted
        .filter { it }
        .flatMapLatest { connection }
        .flatMapLatest { it?.status ?: flowOf(ClickerStatus.Disconnected) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ClickerStatus.MissingPermission,
        )

    fun notifyPermissionGranted() {
        isPermissionGranted.value = true
    }

    fun sendCommand(command: ClickerCommand) {
        connection.value?.sendCommand(command)
    }
}
