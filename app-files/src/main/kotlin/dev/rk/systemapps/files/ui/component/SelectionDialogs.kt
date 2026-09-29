package dev.rk.systemapps.files.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.rk.systemapps.core.design.component.ConfirmDialog
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.ui.browser.NameInputDialog
import dev.rk.systemapps.files.ui.browser.PropertiesDialog

/**
 * Seçim modunun diyalogları: silme onayı, yeniden adlandırma ve özellikler.
 * Üç ekranda da aynı davranış olsun diye tek yerde.
 */
@Composable
fun SelectionDialogs(
    state: SelectionState,
    deleteRequested: Boolean,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onAction: (SelectionAction) -> Unit,
) {
    if (deleteRequested) {
        ConfirmDialog(
            title = pluralStringResource(
                R.plurals.delete_confirm_title,
                state.selectedIds.size,
                state.selectedIds.size,
            ),
            message = stringResource(R.string.delete_confirm_message),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss,
            destructive = true,
        )
    }

    when (val dialog = state.dialog) {
        is SelectionDialog.Rename -> NameInputDialog(
            title = stringResource(R.string.dialog_rename_title),
            initialName = dialog.node.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { name -> onAction(SelectionAction.ConfirmName(name)) },
            onDismiss = { onAction(SelectionAction.DismissDialog) },
            errorMessage = state.nameError?.let { stringResource(it.messageRes()) },
        )

        is SelectionDialog.Properties -> PropertiesDialog(
            details = dialog.details,
            stats = state.directoryStats,
            onDismiss = { onAction(SelectionAction.DismissDialog) },
        )

        null -> Unit
    }
}

private fun SelectionNameError.messageRes(): Int = when (this) {
    SelectionNameError.ALREADY_EXISTS -> R.string.name_error_exists
    SelectionNameError.INVALID -> R.string.name_error_invalid
    SelectionNameError.FAILED -> R.string.name_error_failed
}
