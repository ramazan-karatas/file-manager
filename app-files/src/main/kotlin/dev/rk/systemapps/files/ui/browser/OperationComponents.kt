package dev.rk.systemapps.files.ui.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rk.systemapps.core.common.format.formatBytes
import dev.rk.systemapps.core.design.component.ProgressSheet
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.domain.model.Conflict
import dev.rk.systemapps.files.domain.model.ConflictDecision
import dev.rk.systemapps.files.domain.model.ConflictResolution
import dev.rk.systemapps.files.domain.model.FileClipboard
import dev.rk.systemapps.files.domain.model.ClipboardMode
import dev.rk.systemapps.files.domain.model.OperationProgress
import dev.rk.systemapps.files.domain.model.OperationState
import java.io.File

/** Pano doluyken ekranın altında duran kalıcı çubuk (docs/files/SPEC.md §3.4). */
@Composable
fun PasteBar(
    clipboard: FileClipboard,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentPaste,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = pluralStringResource(
                    when (clipboard.mode) {
                        ClipboardMode.COPY -> R.plurals.clipboard_copy_pending
                        ClipboardMode.MOVE -> R.plurals.clipboard_move_pending
                    },
                    clipboard.paths.size,
                    clipboard.paths.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            TextButton(onClick = onPaste) { Text(stringResource(R.string.action_paste)) }
        }
    }
}

/** Yürüyen işlemin ilerlemesi; iptal buradan ve bildirimden yapılabilir. */
@Composable
fun OperationProgressBar(
    progress: OperationProgress,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProgressSheet(
        title = stringResource(
            if (progress.state == OperationState.PREPARING) {
                R.string.operation_preparing
            } else {
                R.string.operation_running
            },
        ),
        currentItem = progress.currentFile,
        counterText = stringResource(
            R.string.operation_counter,
            progress.processedItems,
            progress.totalItems,
            formatBytes(progress.processedBytes),
            formatBytes(progress.totalBytes),
        ),
        cancelLabel = stringResource(R.string.action_cancel),
        onCancel = onCancel,
        progress = progress.fraction,
        modifier = modifier,
    )
}

/**
 * Çakışma sorusu. "Kalan çakışmalara da uygula" kutusu işaretliyse motor bir daha sormaz.
 */
@Composable
fun ConflictDialog(
    conflict: Conflict,
    onDecision: (ConflictDecision) -> Unit,
) {
    var applyToAll by remember(conflict) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDecision(ConflictDecision(ConflictResolution.CANCEL)) },
        title = { Text(stringResource(R.string.conflict_title)) },
        text = {
            Column {
                Text(
                    stringResource(
                        R.string.conflict_message,
                        File(conflict.targetPath).name,
                    ),
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = applyToAll, onCheckedChange = { applyToAll = it })
                    Text(
                        text = stringResource(R.string.conflict_apply_to_all),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Top) {
                TextButton(
                    onClick = {
                        onDecision(ConflictDecision(ConflictResolution.OVERWRITE, applyToAll))
                    },
                ) {
                    Text(stringResource(R.string.action_overwrite))
                }
                TextButton(
                    onClick = {
                        onDecision(ConflictDecision(ConflictResolution.KEEP_BOTH, applyToAll))
                    },
                ) {
                    Text(stringResource(R.string.action_keep_both))
                }
                TextButton(
                    onClick = {
                        onDecision(ConflictDecision(ConflictResolution.SKIP, applyToAll))
                    },
                ) {
                    Text(stringResource(R.string.action_skip))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onDecision(ConflictDecision(ConflictResolution.CANCEL)) }) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview(name = "Yapıştırma çubuğu")
@Preview(name = "Yapıştırma çubuğu — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PasteBarPreview() {
    SystemAppsTheme {
        PasteBar(
            clipboard = FileClipboard(listOf("/a", "/b", "/c"), ClipboardMode.COPY),
            onPaste = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Çakışma diyaloğu")
@Composable
private fun ConflictDialogPreview() {
    SystemAppsTheme {
        ConflictDialog(
            conflict = Conflict(
                sourcePath = "/storage/emulated/0/DCIM/foto.jpg",
                targetPath = "/storage/emulated/0/Pictures/foto.jpg",
                sourceIsDirectory = false,
            ),
            onDecision = {},
        )
    }
}
