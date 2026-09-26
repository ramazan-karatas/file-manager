package dev.rk.systemapps.core.design.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.rk.systemapps.core.design.theme.SystemAppsTheme

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

@Preview(name = "Onay diyaloğu")
@Composable
private fun ConfirmDialogPreview() {
    SystemAppsTheme {
        ConfirmDialog(
            title = "3 öğe silinsin mi?",
            message = "Seçilen öğeler çöp kutusuna taşınacak.",
            confirmLabel = "Sil",
            dismissLabel = "Vazgeç",
            onConfirm = {},
            onDismiss = {},
            destructive = true,
        )
    }
}
