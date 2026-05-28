package io.github.sinatoe.slideclick.clicker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import io.github.sinatoe.slideclick.R
import io.github.sinatoe.slideclick.theme.AppTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ClickerScreen(viewModel: ClickerViewModel = viewModel()) {
    val context = LocalContext.current

    val permissionsState = rememberMultiplePermissionsState(
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )

    val bluetoothPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissionsState.permissions.find { it.permission == Manifest.permission.BLUETOOTH_CONNECT }
    } else {
        null
    }

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    ClickerScreen(
        status = if (bluetoothPermissionState?.status?.isGranted != false) {
            viewModel.status.collectAsStateWithLifecycle().value
        } else {
            null
        },
        onRequestPermission = {
            if (bluetoothPermissionState?.status?.shouldShowRationale == true) {
                bluetoothPermissionState.launchPermissionRequest()
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    },
                )
            }
        },
        onSendCommand = { viewModel.sendCommand(it) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClickerScreen(
    status: ClickerStatus?,
    onRequestPermission: () -> Unit,
    onSendCommand: (ClickerCommand) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    val isConnected = status is ClickerStatus.Connected

    Scaffold(
        modifier = if (isConnected) Modifier.keepScreenOn() else Modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Slideclick")
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (status) {
                null -> {
                    Text(
                        text = "Nearby devices permission is required for the app",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Button(
                        onClick = onRequestPermission,
                    ) {
                        Text(text = "Grant permission")
                    }
                }

                ClickerStatus.Idle -> {
                    Text(
                        text = "Waiting for connection...",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                is ClickerStatus.Connected -> {
                    Text(
                        text = "Connected to ${status.deviceName}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                ClickerStatus.Unsupported -> {
                    Text(
                        text = "This device can't run this app",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                listOf(
                    Triple(ClickerCommand.BACK, R.drawable.ic_chevron_backward, "Back"),
                    Triple(ClickerCommand.FORWARD, R.drawable.ic_chevron_forward, "Forward"),
                )
                    .forEach { (command, iconResourceId, iconDescription) ->
                        FilledTonalIconButton(
                            onClick = {
                                onSendCommand(command)
                                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            },
                            enabled = isConnected,
                            modifier = Modifier.size(
                                IconButtonDefaults.extraLargeContainerSize(
                                    IconButtonDefaults.IconButtonWidthOption.Narrow,
                                ),
                            ),
                            shape = IconButtonDefaults.extraLargeRoundShape,
                        ) {
                            Icon(
                                painter = painterResource(iconResourceId),
                                contentDescription = iconDescription,
                                modifier = Modifier.size(IconButtonDefaults.extraLargeIconSize),
                            )
                        }
                    }
            }
        }
    }
}

@Preview
@Composable
private fun ClickerScreen_Preview() {
    AppTheme {
        ClickerScreen(
            status = ClickerStatus.Connected("Alice's PC"),
            onRequestPermission = {},
            onSendCommand = {},
        )
    }
}
