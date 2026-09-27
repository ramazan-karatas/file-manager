package dev.rk.systemapps.files.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rk.systemapps.core.common.format.formatBytes
import dev.rk.systemapps.core.common.format.formatDate
import dev.rk.systemapps.core.common.format.formatPercent
import dev.rk.systemapps.core.design.component.AppListItem
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.core.storage.model.StorageVolumeInfo
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.launchManageAllFilesSettings
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.domain.model.FileCategory
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.ui.browser.FileActions
import dev.rk.systemapps.files.ui.browser.FileThumbnail
import dev.rk.systemapps.files.ui.component.AccessWarningBanner

@Composable
fun HomeRoute(
    onOpenFolder: (String) -> Unit,
    onOpenCategory: (FileCategory) -> Unit,
    onOpenAbout: () -> Unit,
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
        onOpenCategory = onOpenCategory,
        onOpenFile = { node -> FileActions.open(context, node) },
        onOpenAbout = onOpenAbout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onGrantAccess: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenCategory: (FileCategory) -> Unit,
    onOpenFile: (FileNode) -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Büyük başlık kaydırınca küçülüyor: ekranın üstü boşa gitmiyor, liste yer kazanıyor.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.about_title),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            if (uiState.showLimitedAccessBanner) {
                item { AccessWarningBanner(onGrantAccess = onGrantAccess) }
            }

            items(items = uiState.volumes, key = { it.id }) { volume ->
                StorageCard(volume = volume, onClick = { onOpenFolder(volume.path) })
            }

            item {
                SectionTitle(stringResource(R.string.home_categories))
                CategoryGrid(
                    onOpenCategory = onOpenCategory,
                    onOpenDownloads = { onOpenFolder("${uiState.storageRoot}/Download") },
                )
            }

            if (uiState.recent.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.home_recent)) }
                items(items = uiState.recent, key = { it.id }) { node ->
                    AppListItem(
                        title = node.name,
                        subtitle = "${formatBytes(node.size)} · ${formatDate(node.lastModified)}",
                        leading = { FileThumbnail(node = node) },
                        onClick = { onOpenFile(node) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun StorageCard(volume: StorageVolumeInfo, onClick: () -> Unit) {
    // Birincil birimin adı sistemden gelmiyor; yerelleştirilmiş metin burada veriliyor.
    val label = volume.label.ifEmpty { stringResource(R.string.storage_internal) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // İkon renkli bir kap içinde: kart içinde tek başına yüzmüyor.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (volume.isRemovable) {
                            Icons.Outlined.SdCard
                        } else {
                            Icons.Outlined.Smartphone
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                ) {
                    Text(text = label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            R.string.storage_free,
                            formatBytes(volume.freeBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatPercent(volume.usedBytes, volume.totalBytes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            LinearProgressIndicator(
                progress = { volume.usedFraction },
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(8.dp),
            )
            Text(
                text = stringResource(
                    R.string.storage_usage,
                    formatBytes(volume.usedBytes),
                    formatBytes(volume.totalBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    onOpenCategory: (FileCategory) -> Unit,
    onOpenDownloads: () -> Unit,
) {
    // LazyColumn içinde olduğu için iç içe kaydırma yaratmayan basit satırlar.
    val entries = FileCategory.entries
    val tiles = buildList {
        add(Triple(Icons.Outlined.Download, R.string.category_downloads, onOpenDownloads))
        entries.forEach { category ->
            add(Triple(category.icon(), category.labelRes()) { onOpenCategory(category) })
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiles.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (icon, labelRes, onClick) ->
                    CategoryTile(
                        icon = icon,
                        label = stringResource(labelRes),
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Son satır eksik kalırsa kutular genişlemesin diye boşluk bırakılıyor.
                repeat(COLUMNS - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val COLUMNS = 4

@Composable
private fun CategoryTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

internal fun FileCategory.icon(): ImageVector = when (this) {
    FileCategory.IMAGES -> Icons.Outlined.Image
    FileCategory.VIDEO -> Icons.Outlined.Videocam
    FileCategory.AUDIO -> Icons.Outlined.MusicNote
    FileCategory.DOCUMENTS -> Icons.Outlined.Description
    FileCategory.APK -> Icons.Outlined.Android
    FileCategory.ARCHIVES -> Icons.Outlined.Archive
}

internal fun FileCategory.labelRes(): Int = when (this) {
    FileCategory.IMAGES -> R.string.category_images
    FileCategory.VIDEO -> R.string.category_video
    FileCategory.AUDIO -> R.string.category_audio
    FileCategory.DOCUMENTS -> R.string.category_documents
    FileCategory.APK -> R.string.category_apk
    FileCategory.ARCHIVES -> R.string.category_archives
}

@Preview(name = "Ana ekran")
@Preview(name = "Ana ekran — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    SystemAppsTheme {
        HomeScreen(
            uiState = HomeUiState(
                accessLevel = StorageAccessLevel.FULL,
                storageRoot = "/storage/emulated/0",
                volumes = listOf(
                    StorageVolumeInfo(
                        id = "/storage/emulated/0",
                        label = "",
                        path = "/storage/emulated/0",
                        totalBytes = 128_000_000_000,
                        freeBytes = 42_000_000_000,
                        isRemovable = false,
                        isPrimary = true,
                    ),
                    StorageVolumeInfo(
                        id = "/storage/1A2B",
                        label = "SD kart",
                        path = "/storage/1A2B",
                        totalBytes = 64_000_000_000,
                        freeBytes = 60_000_000_000,
                        isRemovable = true,
                        isPrimary = false,
                    ),
                ),
            ),
            onGrantAccess = {},
            onOpenFolder = {},
            onOpenCategory = {},
            onOpenFile = {},
            onOpenAbout = {},
        )
    }
}
