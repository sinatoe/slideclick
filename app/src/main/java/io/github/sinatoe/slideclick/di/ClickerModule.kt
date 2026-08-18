package io.github.sinatoe.slideclick.di

import io.github.sinatoe.slideclick.data.ClickerService
import io.github.sinatoe.slideclick.ui.ClickerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val clickerModule = module {
    factory { ClickerService.connectionFlow(androidContext()) }
    viewModelOf(::ClickerViewModel)
}
