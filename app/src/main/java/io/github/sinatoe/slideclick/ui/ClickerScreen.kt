package io.github.sinatoe.slideclick.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sinatoe.slideclick.R
import io.github.sinatoe.slideclick.domain.ClickerCommand
import io.github.sinatoe.slideclick.domain.ClickerStatus
import org.koin.androidx.compose.koinViewModel

@Composable
fun ClickerScreen(viewModel: ClickerViewModel = koinViewModel()) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = LocalActivity.current

    var showPermissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return@rememberLauncherForActivityResult
        }

        val isGranted = results[Manifest.permission.BLUETOOTH_CONNECT] == true

        if (isGranted) {
            viewModel.notifyPermissionGranted()
            return@rememberLauncherForActivityResult
        }

        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } ?: true

        if (!shouldShowRationale) {
            showPermissionDenied = true
        }
    }

    if (status == ClickerStatus.MissingPermission) {
        LifecycleResumeEffect(Unit) {
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.notifyPermissionGranted()
            }

            onPauseOrDispose { }
        }

        if (showPermissionDenied) {
            PermissionDeniedDialog(
                onDismiss = { showPermissionDenied = false },
                onOpenSettings = {
                    showPermissionDenied = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                },
            )
        }
    }

    ClickerScreen(
        status = status,
        onRequestPermission = {
            permissionLauncher.launch(
                buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add(Manifest.permission.BLUETOOTH_CONNECT)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray(),
            )
        },
        onSendCommand = { viewModel.sendCommand(it) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClickerScreen(
    status: ClickerStatus,
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
                    ClickerStatus.Disconnected -> {
                        Pair(
                            painterResource(R.drawable.ic_devices_outlined),
                            stringResource(R.string.clicker_status_disconnected),
                        )
                    }

                    ClickerStatus.MissingPermission -> {
                        Pair(
                            painterResource(R.drawable.ic_security),
                            stringResource(R.string.clicker_status_permission_needed),
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
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer,
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
                        .clip(MaterialShapes.Sunny.toShape())
                        .background(iconBackground)
                        .padding(24.dp)
                        .size(32.dp),
                    tint = iconTint,
                )

                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )

                if (status == ClickerStatus.MissingPermission) {
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
