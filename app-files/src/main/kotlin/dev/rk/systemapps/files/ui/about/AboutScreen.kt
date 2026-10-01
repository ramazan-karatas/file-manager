package dev.rk.systemapps.files.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.BuildConfig
import dev.rk.systemapps.files.R

/**
 * Hakkında ekranı. Asıl amacı reklamsızlık ve ağa çıkmama sözünü somut hâle getirmek:
 * uygulamanın istediği izinler tek tek sayılıyor (docs/files/PLAN.md F-1.11).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutRoute(onNavigateUp: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AboutClaim(Icons.Outlined.WifiOff, stringResource(R.string.about_no_internet))
            AboutClaim(Icons.Outlined.Block, stringResource(R.string.about_no_ads))

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.about_permissions_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.about_permissions_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            AboutClaim(Icons.Outlined.Folder, stringResource(R.string.about_permission_storage))
            AboutClaim(Icons.Outlined.PhotoLibrary, stringResource(R.string.about_permission_media))
            AboutClaim(Icons.Outlined.Sync, stringResource(R.string.about_permission_service))
            AboutClaim(
                Icons.Outlined.Notifications,
                stringResource(R.string.about_permission_notifications),
            )
        }
    }
}

@Composable
private fun AboutClaim(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Preview(name = "Hakkında")
@Preview(name = "Hakkında — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutPreview() {
    SystemAppsTheme { AboutRoute(onNavigateUp = {}) }
}
