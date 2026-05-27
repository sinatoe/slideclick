package io.github.sinatoe.slideclick.clicker

sealed interface ClickerStatus {
    data object Idle : ClickerStatus

    data class Connected(val deviceName: String) : ClickerStatus

    data object Unsupported : ClickerStatus
}
