package dev.rk.systemapps.files.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rk.systemapps.core.design.component.AppListItem
import dev.rk.systemapps.core.design.component.ItemIcon
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.launchManageAllFilesSettings
import dev.rk.systemapps.core.storage.volume.StoragePaths
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.ui.component.AccessWarningBanner

@Composable
fun HomeRoute(
    onOpenFolder: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.onAction(HomeAction.Refresh)
        onPauseOrDispose {}
    }

    HomeScreen(
        uiState = uiState,
        onGrantAccess = { context.launchManageAllFilesSettings() },
        onOpenFolder = onOpenFolder,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onGrantAccess: () -> Unit,
    onOpenFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.home_title)) })
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (uiState.showLimitedAccessBanner) {
                AccessWarningBanner(onGrantAccess = onGrantAccess)
            }
            // F-1.10'da burası depolama kartları, kategoriler ve son değişenlerle dolacak.
            // Şimdilik gezgine tek giriş noktası.
            AppListItem(
                title = stringResource(R.string.storage_internal),
                subtitle = stringResource(R.string.storage_internal_subtitle),
                leading = { ItemIcon(Icons.Outlined.Smartphone) },
                onClick = { onOpenFolder(StoragePaths.primaryExternalStorage()) },
            )
        }
    }
}

@Preview(name = "Ana ekran — tam erişim")
@Composable
private fun HomeScreenPreview() {
    SystemAppsTheme {
        HomeScreen(
            uiState = HomeUiState(accessLevel = StorageAccessLevel.FULL),
            onGrantAccess = {},
            onOpenFolder = {},
        )
    }
}

@Preview(name = "Ana ekran — sınırlı mod")
@Preview(name = "Ana ekran — sınırlı mod, koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenLimitedPreview() {
    SystemAppsTheme {
        HomeScreen(
            uiState = HomeUiState(accessLevel = StorageAccessLevel.LIMITED),
            onGrantAccess = {},
            onOpenFolder = {},
        )
    }
}
