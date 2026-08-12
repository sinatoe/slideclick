package io.github.sinatoe.slideclick.clicker.domain

sealed interface ClickerStatus {
    data object Disconnected : ClickerStatus

    data class Connected(val deviceName: String) : ClickerStatus

    data object Unsupported : ClickerStatus
}
