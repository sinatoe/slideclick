package io.github.sinatoe.slideclick.clicker.di

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import io.github.sinatoe.slideclick.clicker.data.ClickerService
import io.github.sinatoe.slideclick.clicker.domain.ClickerConnection
import io.github.sinatoe.slideclick.clicker.ui.ClickerViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val clickerModule = module {
    factory<Flow<ClickerConnection?>> {
        val context = androidContext()
        callbackFlow {
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    trySend((binder as ClickerService.LocalBinder).getService())
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    trySend(null)
                }
            }

            val intent = Intent(context, ClickerService::class.java)

            ContextCompat.startForegroundService(context, intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            awaitClose {
                context.unbindService(serviceConnection)
                context.stopService(intent)
            }
        }
    }
    viewModelOf(::ClickerViewModel)
}
