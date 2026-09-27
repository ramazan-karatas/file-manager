package dev.rk.systemapps.files.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rk.systemapps.core.common.format.formatBytes
import dev.rk.systemapps.core.common.format.formatDate
import dev.rk.systemapps.core.design.component.AppListItem
import dev.rk.systemapps.core.design.component.EmptyState
import dev.rk.systemapps.core.design.component.LoadingState
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.LocalFileNode

@Composable
fun BrowserRoute(
    onNavigateToFolder: (String) -> Unit,
    onNavigateUp: () -> Unit,
    /** Kökten hedefe kadar olan yollar; geri yığını buna göre yeniden kurulur. */
    onNavigateToCrumb: (List<String>) -> Unit,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BrowserScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onItemClick = { node ->
            // Dosya açma F-1.8'de gelecek; şimdilik yalnızca klasörler gezilebilir.
            if (node.isDirectory) onNavigateToFolder(node.id)
        },
        onNavigateUp = onNavigateUp,
        onCrumbClick = { crumb ->
            val index = uiState.crumbs.indexOf(crumb)
            if (index >= 0 && index != uiState.crumbs.lastIndex) {
                onNavigateToCrumb(uiState.crumbs.take(index + 1).map { it.path })
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    uiState: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    onItemClick: (FileNode) -> Unit,
    onNavigateUp: () -> Unit,
    onCrumbClick: (Crumb) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_up),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(BrowserAction.ToggleViewMode) }) {
                        Icon(
                            imageVector = if (uiState.gridMode) {
                                Icons.AutoMirrored.Outlined.ViewList
                            } else {
                                Icons.Outlined.GridView
                            },
                            contentDescription = stringResource(
                                if (uiState.gridMode) {
                                    R.string.action_view_list
                                } else {
                                    R.string.action_view_grid
                                },
                            ),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Breadcrumbs(crumbs = uiState.crumbs, onCrumbClick = onCrumbClick)

            when {
                uiState.isLoading -> LoadingState()

                uiState.error != null -> EmptyState(
                    icon = Icons.Outlined.FolderOff,
                    title = stringResource(uiState.error.titleRes()),
                    description = uiState.path,
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = { onAction(BrowserAction.Reload) },
                )

                uiState.isEmpty -> EmptyState(
                    icon = Icons.Outlined.FolderOff,
                    title = stringResource(R.string.browser_empty_title),
                    description = stringResource(R.string.browser_empty_description),
                )

                uiState.gridMode -> FileGrid(items = uiState.items, onItemClick = onItemClick)

                else -> FileList(items = uiState.items, onItemClick = onItemClick)
            }
        }
    }
}

@Composable
private fun BrowserUiState.currentTitle(): String {
    val crumb = crumbs.lastOrNull() ?: return stringResource(R.string.home_title)
    return if (crumb.isStorageRoot) stringResource(R.string.storage_internal) else crumb.name
}

@Composable
private fun FileList(items: List<FileNode>, onItemClick: (FileNode) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // key: yeniden sıralamada ve yenilemede satırların kimliğini korur.
        items(items = items, key = { it.id }) { node ->
            AppListItem(
                title = node.name,
                subtitle = node.subtitle(),
                leading = { FileThumbnail(node = node) },
                onClick = { onItemClick(node) },
            )
        }
    }
}

@Composable
private fun FileGrid(items: List<FileNode>, onItemClick: (FileNode) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(items = items, key = { it.id }) { node ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(node) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FileThumbnail(node = node, size = 56.dp, cornerRadius = 8.dp)
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun FileNode.subtitle(): String = if (isDirectory) {
    // Öğe sayısı her klasör için ayrı bir dizin okuması demek; listeleme bütçesini
    // aşmamak için burada tarih gösteriliyor, sayı özellikler diyaloğunda verilecek (F-1.8).
    formatDate(lastModified)
} else {
    "${formatBytes(size)} · ${formatDate(lastModified)}"
}

private fun BrowserError.titleRes(): Int = when (this) {
    BrowserError.NOT_FOUND -> R.string.browser_error_not_found
    BrowserError.NOT_READABLE -> R.string.browser_error_not_readable
    BrowserError.UNKNOWN -> R.string.browser_error_unknown
}

@Preview(name = "Gezgin — liste")
@Preview(name = "Gezgin — liste, koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BrowserScreenPreview() {
    SystemAppsTheme {
        BrowserScreen(
            uiState = BrowserUiState(
                path = "/storage/emulated/0/Belgeler",
                crumbs = listOf(
                    Crumb("/storage/emulated/0", "/storage/emulated/0", isStorageRoot = true),
                    Crumb("Belgeler", "/storage/emulated/0/Belgeler"),
                ),
                items = listOf(
                    previewNode("Faturalar", isDirectory = true),
                    previewNode("rapor_şubat.pdf", size = 1_258_291),
                    previewNode("notlar.txt", size = 4_096),
                ),
                isLoading = false,
            ),
            onAction = {},
            onItemClick = {},
            onNavigateUp = {},
            onCrumbClick = {},
        )
    }
}

private fun previewNode(name: String, isDirectory: Boolean = false, size: Long = 0) = LocalFileNode(
    id = "/storage/emulated/0/Belgeler/$name",
    name = name,
    isDirectory = isDirectory,
    size = if (isDirectory) FileNode.SIZE_UNKNOWN else size,
    lastModified = 1_770_000_000_000,
    mimeType = null,
    isHidden = false,
)
