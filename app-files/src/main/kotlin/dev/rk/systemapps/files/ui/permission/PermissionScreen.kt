package dev.rk.systemapps.files.ui.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.launchManageAllFilesSettings
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.R

@Composable
fun PermissionRoute(
    onCompleted: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Kullanıcı Ayarlar'dan dönünce izin durumu değişmiş olabilir.
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(PermissionAction.Refresh)
        onPauseOrDispose {}
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onAction(PermissionAction.RuntimePermissionResult)
    }

    // Navigasyon composition sırasında değil, yan etki olarak tetiklenmeli.
    LaunchedEffect(uiState.completed) {
        if (uiState.completed) onCompleted()
    }

    PermissionScreen(
        uiState = uiState,
        onGrantFullAccess = {
            if (uiState.requiresManageExternalStorage) {
                if (!context.launchManageAllFilesSettings()) {
                    viewModel.onAction(PermissionAction.SettingsUnavailable)
                }
            } else {
                runtimePermissionLauncher.launch(uiState.runtimePermissions.toTypedArray())
            }
        },
        onContinueLimited = {
            if (uiState.accessLevel == StorageAccessLevel.NONE) {
                runtimePermissionLauncher.launch(uiState.runtimePermissions.toTypedArray())
            } else {
                viewModel.onAction(PermissionAction.ContinueWithLimitedAccess)
            }
        },
    )
}

@Composable
fun PermissionScreen(
    uiState: PermissionUiState,
    onGrantFullAccess: () -> Unit,
    onContinueLimited: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.permission_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            ReasonRow(Icons.Outlined.Folder, stringResource(R.string.permission_reason_browse))
            ReasonRow(Icons.Outlined.Search, stringResource(R.string.permission_reason_search))
            ReasonRow(Icons.Outlined.WifiOff, stringResource(R.string.permission_reason_offline))

            if (uiState.settingsUnavailable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.permission_settings_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Button(
                onClick = onGrantFullAccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            ) {
                Text(stringResource(R.string.permission_grant_full))
            }

            TextButton(
                onClick = onContinueLimited,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.permission_continue_limited))
            }
        }
    }
}

@Composable
private fun ReasonRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Preview(name = "İzin ekranı")
@Preview(name = "İzin ekranı — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PermissionScreenPreview() {
    SystemAppsTheme {
        PermissionScreen(
            uiState = PermissionUiState(),
            onGrantFullAccess = {},
            onContinueLimited = {},
        )
    }
}
