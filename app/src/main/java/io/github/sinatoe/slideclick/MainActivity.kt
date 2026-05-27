package io.github.sinatoe.slideclick

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.github.sinatoe.slideclick.clicker.ClickerScreen
import io.github.sinatoe.slideclick.ui.BluetoothPermissionRequestScreen
import io.github.sinatoe.slideclick.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                RequireBluetoothPermission {
                    ClickerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequireBluetoothPermission(
    content: @Composable () -> Unit,
) {
    val permissionsState = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else emptyList(),
    )

    if (!permissionsState.allPermissionsGranted) {
        BluetoothPermissionRequestScreen(
            onRequest = { permissionsState.launchMultiplePermissionRequest() },
        )
    } else {
        content()
    }
}
