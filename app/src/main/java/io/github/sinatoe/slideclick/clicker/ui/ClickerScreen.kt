package io.github.sinatoe.slideclick.clicker.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import io.github.sinatoe.slideclick.R
import io.github.sinatoe.slideclick.clicker.domain.ClickerCommand
import io.github.sinatoe.slideclick.clicker.domain.ClickerStatus
import io.github.sinatoe.slideclick.common.ui.AppTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ClickerScreen(viewModel: ClickerViewModel = koinViewModel()) {
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
                    Text(text = stringResource(R.string.clicker_title_main))
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.padding(contentPadding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val (iconPainter, text) = when (status) {
                    null -> {
                        Pair(
                            painterResource(R.drawable.ic_security),
                            stringResource(R.string.clicker_status_permission_needed),
                        )
                    }

                    ClickerStatus.Disconnected -> {
                        Pair(
                            painterResource(R.drawable.ic_devices_outlined),
                            stringResource(R.string.clicker_status_disconnected),
                        )
                    }

                    is ClickerStatus.Connected -> {
                        Pair(
                            painterResource(R.drawable.ic_devices_filled),
                            stringResource(R.string.clicker_status_connected, status.deviceName),
                        )
                    }

                    ClickerStatus.Unsupported -> {
                        Pair(
                            painterResource(R.drawable.ic_devices_off),
                            stringResource(R.string.clicker_status_unsupported),
                        )
                    }
                }

                val (iconBackground, iconTint) = if (isConnected) {
                    Pair(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary,
                    )
                } else {
                    Pair(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(iconBackground)
                        .padding(16.dp)
                        .size(32.dp),
                    tint = iconTint,
                )

                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )

                if (status == null) {
                    Button(
                        onClick = onRequestPermission,
                    ) {
                        Text(text = stringResource(R.string.clicker_btn_grant))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(
                    Triple(
                        ClickerCommand.BACK,
                        painterResource(R.drawable.ic_chevron_backward),
                        stringResource(R.string.clicker_cd_back),
                    ),
                    Triple(
                        ClickerCommand.FORWARD,
                        painterResource(R.drawable.ic_chevron_forward),
                        stringResource(R.string.clicker_cd_forward),
                    ),
                )
                    .forEach { (command, iconPainter, iconDescription) ->
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
                            shapes = IconButtonShapes(
                                shape = IconButtonDefaults.extraLargeRoundShape,
                                pressedShape = IconButtonDefaults.extraLargePressedShape,
                            ),
                        ) {
                            Icon(
                                painter = iconPainter,
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
