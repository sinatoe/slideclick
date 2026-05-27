package io.github.sinatoe.slideclick.clicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ClickerScreen(viewModel: ClickerViewModel = viewModel()) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val isConnected = status is ClickerStatus.Connected

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = when (val currentStatus = status) {
                    is ClickerStatus.Connected -> "Connected to ${currentStatus.deviceName}"
                    ClickerStatus.Idle -> "Waiting for connection..."
                    ClickerStatus.Unsupported -> "Device not supported"
                },
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { viewModel.sendCommand(ClickerCommand.BACK) },
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Back")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { viewModel.sendCommand(ClickerCommand.FORWARD) },
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Forward")
                }
            }
        }
    }
}
