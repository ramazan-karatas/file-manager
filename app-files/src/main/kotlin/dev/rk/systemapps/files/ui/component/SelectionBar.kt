package dev.rk.systemapps.files.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.rk.systemapps.core.design.component.SelectionTopBar
import dev.rk.systemapps.files.R

/**
 * Seçim modundaki üst çubuk. Üç ekranda da aynı olsun diye burada.
 *
 * Çubuğa yalnızca sık kullanılan dört işlem sığdırılıyor; fazlası sayacı ve
 * kapatma düğmesini dar ekranda dışarı taşıyordu.
 */
@Composable
fun FileSelectionTopBar(
    selectedCount: Int,
    singleSelection: Boolean,
    onAction: (SelectionAction) -> Unit,
    onShare: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    SelectionTopBar(
        selectedCount = selectedCount,
        onClose = { onAction(SelectionAction.Clear) },
        closeContentDescription = stringResource(R.string.action_close_selection),
        actions = {
            IconButton(onClick = { onAction(SelectionAction.Copy) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy),
                )
            }
            IconButton(onClick = { onAction(SelectionAction.Cut) }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCut,
                    contentDescription = stringResource(R.string.action_cut),
                )
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.action_share),
                )
            }
            OverflowMenu(singleSelection = singleSelection, onAction = onAction)
        },
    )
}

@Composable
private fun OverflowMenu(singleSelection: Boolean, onAction: (SelectionAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.action_more),
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        // Yeniden adlandırma ve özellikler yalnızca tek öğede anlamlı.
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_rename)) },
            enabled = singleSelection,
            onClick = {
                expanded = false
                onAction(SelectionAction.ShowRename)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_properties)) },
            enabled = singleSelection,
            onClick = {
                expanded = false
                onAction(SelectionAction.ShowProperties)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_select_all)) },
            onClick = {
                expanded = false
                onAction(SelectionAction.SelectAll)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_invert_selection)) },
            onClick = {
                expanded = false
                onAction(SelectionAction.Invert)
            },
        )
    }
}
