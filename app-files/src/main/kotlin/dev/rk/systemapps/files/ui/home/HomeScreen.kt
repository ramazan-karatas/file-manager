package dev.rk.systemapps.files.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.rk.systemapps.core.design.component.EmptyState
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.R

@Composable
fun HomeRoute() {
    HomeScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.home_title)) })
        },
    ) { innerPadding ->
        // F-1.10'da depolama kartları, kategoriler ve son değişenler buraya gelecek.
        EmptyState(
            icon = Icons.Outlined.Folder,
            title = stringResource(R.string.scaffold_placeholder_title),
            description = stringResource(R.string.scaffold_placeholder_description),
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Preview(name = "Ana ekran")
@Preview(name = "Ana ekran — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    SystemAppsTheme {
        HomeScreen()
    }
}
