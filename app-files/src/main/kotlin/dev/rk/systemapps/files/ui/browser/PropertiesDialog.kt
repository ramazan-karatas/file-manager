package dev.rk.systemapps.files.ui.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rk.systemapps.core.common.format.formatBytes
import dev.rk.systemapps.core.common.format.formatDate
import dev.rk.systemapps.core.common.format.formatDuration
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.domain.model.DirectoryStats
import dev.rk.systemapps.files.domain.model.FileDetails
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.LocalFileNode
import dev.rk.systemapps.files.domain.model.MediaInfo

/**
 * Özellikler diyaloğu (docs/files/SPEC.md §3.6).
 * Klasör boyutu ayrı hesaplandığı için [stats] null iken spinner gösterilir.
 */
@Composable
fun PropertiesDialog(
    details: FileDetails,
    stats: DirectoryStats?,
    onDismiss: () -> Unit,
) {
    val node = details.node

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(node.name, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PropertyRow(stringResource(R.string.properties_path), node.id)

                if (node.isDirectory) {
                    PropertyRowWithSpinner(
                        label = stringResource(R.string.properties_size),
                        value = stats?.let { formatBytes(it.totalBytes) },
                    )
                    PropertyRowWithSpinner(
                        label = stringResource(R.string.properties_items),
                        value = stats?.let {
                            pluralStringResource(
                                R.plurals.properties_item_count,
                                it.itemCount,
                                it.itemCount,
                            )
                        },
                    )
                } else {
                    PropertyRow(stringResource(R.string.properties_size), formatBytes(node.size))
                    PropertyRow(
                        label = stringResource(R.string.properties_type),
                        value = node.mimeType ?: stringResource(R.string.properties_type_unknown),
                    )
                }

                PropertyRow(
                    label = stringResource(R.string.properties_modified),
                    value = formatDate(node.lastModified),
                )
                PropertyRow(
                    label = stringResource(R.string.properties_permissions),
                    value = details.permissionString,
                )

                details.media?.let { media ->
                    if (media.width != null && media.height != null) {
                        PropertyRow(
                            label = stringResource(R.string.properties_resolution),
                            value = "${media.width} × ${media.height}",
                        )
                    }
                    media.durationMs?.let { duration ->
                        PropertyRow(
                            label = stringResource(R.string.properties_duration),
                            value = formatDuration(duration),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PropertyRowWithSpinner(label: String, value: String?) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (value == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.properties_calculating),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        } else {
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(name = "Özellikler — dosya")
@Preview(name = "Özellikler — dosya, koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PropertiesDialogPreview() {
    SystemAppsTheme {
        PropertiesDialog(
            details = FileDetails(
                node = LocalFileNode(
                    id = "/storage/emulated/0/DCIM/Camera/IMG_20260214.jpg",
                    name = "IMG_20260214.jpg",
                    isDirectory = false,
                    size = 3_456_789,
                    lastModified = 1_770_000_000_000,
                    mimeType = "image/jpeg",
                    isHidden = false,
                ),
                canRead = true,
                canWrite = true,
                canExecute = false,
                media = MediaInfo(width = 4000, height = 3000),
            ),
            stats = null,
            onDismiss = {},
        )
    }
}

@Preview(name = "Özellikler — klasör (hesaplanıyor)")
@Composable
private fun PropertiesDialogFolderPreview() {
    SystemAppsTheme {
        PropertiesDialog(
            details = FileDetails(
                node = LocalFileNode(
                    id = "/storage/emulated/0/Belgeler",
                    name = "Belgeler",
                    isDirectory = true,
                    size = FileNode.SIZE_UNKNOWN,
                    lastModified = 1_770_000_000_000,
                    mimeType = null,
                    isHidden = false,
                ),
                canRead = true,
                canWrite = true,
                canExecute = true,
            ),
            stats = null,
            onDismiss = {},
        )
    }
}
