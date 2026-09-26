package dev.rk.systemapps.core.design.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.rk.systemapps.core.design.theme.SystemAppsTheme

/**
 * Çoklu seçim modundaki üst çubuk. İşlem ikonlarını çağıran ekran [actions] ile verir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    closeContentDescription: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = selectedCount.toString()) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = closeContentDescription,
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}

@Preview(name = "Seçim çubuğu")
@Preview(name = "Seçim çubuğu — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectionTopBarPreview() {
    SystemAppsTheme {
        SelectionTopBar(
            selectedCount = 3,
            onClose = {},
            closeContentDescription = "Seçimi kapat",
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Filled.SelectAll, contentDescription = null) }
                IconButton(onClick = {}) { Icon(Icons.Filled.Delete, contentDescription = null) }
            },
        )
    }
}
