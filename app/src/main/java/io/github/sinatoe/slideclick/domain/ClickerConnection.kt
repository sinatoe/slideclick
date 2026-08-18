package io.github.sinatoe.slideclick.domain

import kotlinx.coroutines.flow.Flow

interface ClickerConnection {
    val status: Flow<ClickerStatus>

    fun sendCommand(command: ClickerCommand)
}
