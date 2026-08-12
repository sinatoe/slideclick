package io.github.sinatoe.slideclick.clicker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sinatoe.slideclick.clicker.domain.ClickerCommand
import io.github.sinatoe.slideclick.clicker.domain.ClickerConnection
import io.github.sinatoe.slideclick.clicker.domain.ClickerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ClickerViewModel(clickerConnection: Flow<ClickerConnection?>) : ViewModel() {
    private val connection = clickerConnection
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.minutes),
            initialValue = null,
        )

    val status = connection
        .flatMapLatest { it?.status ?: flowOf(ClickerStatus.Disconnected) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ClickerStatus.Disconnected,
        )

    fun sendCommand(command: ClickerCommand) {
        connection.value?.sendCommand(command)
    }
}
