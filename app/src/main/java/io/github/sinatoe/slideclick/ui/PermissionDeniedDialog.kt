package io.github.sinatoe.slideclick.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sinatoe.slideclick.R

@Composable
fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.permission_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.permission_dialog_message))
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(text = stringResource(R.string.permission_dialog_btn_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onOpenSettings,
            ) {
                Text(text = stringResource(R.string.permission_dialog_btn_settings))
            }
        },
    )
}

@Preview(showSystemUi = true)
@Composable
private fun PermissionDeniedDialog_Preview() {
    AppTheme {
        PermissionDeniedDialog(
            onDismiss = {},
            onOpenSettings = {},
        )
    }
}
