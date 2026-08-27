package io.github.sinatoe.slideclick.domain

sealed interface ClickerStatus {
    data object Disconnected : ClickerStatus

    data object MissingPermission : ClickerStatus

    data class Connected(val deviceName: String) : ClickerStatus

    data object Unsupported : ClickerStatus
}
