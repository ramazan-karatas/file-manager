package dev.rk.systemapps.files.ui.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.R

/**
 * Yeniden adlandırma, yeni klasör ve yeni dosya için ortak ad giriş diyaloğu.
 *
 * Yeniden adlandırmada uzantı seçimin dışında bırakılır: kullanıcı genellikle yalnızca
 * adı değiştirmek ister, uzantıyı yanlışlıkla silmesi kolay olmamalı.
 */
@Composable
fun NameInputDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
) {
    var value by remember {
        val baseLength = initialName.substringBeforeLast('.', initialName).length
        mutableStateOf(
            TextFieldValue(text = initialName, selection = TextRange(0, baseLength)),
        )
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val trimmed = value.text.trim()
    val isValid = trimmed.isNotEmpty() && !trimmed.contains('/') &&
        trimmed != "." && trimmed != ".."

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (isValid) onConfirm(trimmed) },
                    ),
                    modifier = Modifier.focusRequester(focusRequester),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed) }, enabled = isValid) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Preview(name = "Ad giriş diyaloğu")
@Composable
private fun NameInputDialogPreview() {
    SystemAppsTheme {
        NameInputDialog(
            title = "Yeniden adlandır",
            initialName = "rapor_şubat.pdf",
            confirmLabel = "Kaydet",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
