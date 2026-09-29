package dev.rk.systemapps.files.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/**
 * Pano doluyken FAB'ın yerini alan yapıştırma düğmesi (docs/files/SPEC.md §3.4).
 *
 * Eskiden ekranın altında tam genişlikte bir çubuk vardı; hem çirkin duruyordu hem
 * de asıl eylem parmağın uzağında kalıyordu. Pano doluyken ekranın ana eylemi
 * yapıştırmaktır, o yüzden doğrudan FAB yuvasına yerleşiyor. Panoyu temizleme
 * aynı hapın içinde, ikincil ağırlıkta.
 */
@Composable
fun PasteFab(
    clipboard: FileClipboard,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(onClick = onPaste)
                    .padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = pluralStringResource(
                        when (clipboard.mode) {
                            ClipboardMode.COPY -> R.plurals.clipboard_paste_copy
                            ClipboardMode.MOVE -> R.plurals.clipboard_paste_move
                        },
                        clipboard.paths.size,
                        clipboard.paths.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            IconButton(onClick = onCancel, modifier = Modifier.padding(end = 4.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_clear_clipboard),
                    modifier = Modifier.size(20.dp),
                )
            }
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

@Preview(name = "Yapıştırma düğmesi")
@Preview(name = "Yapıştırma düğmesi — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PasteFabPreview() {
    SystemAppsTheme {
        PasteFab(
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
