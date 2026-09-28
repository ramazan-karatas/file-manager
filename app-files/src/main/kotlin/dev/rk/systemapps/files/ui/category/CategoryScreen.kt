package dev.rk.systemapps.files.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import dev.rk.systemapps.files.domain.model.FileCategory
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.LocalFileNode
import dev.rk.systemapps.files.ui.browser.FileActions
import dev.rk.systemapps.files.ui.browser.FileThumbnail
import dev.rk.systemapps.files.ui.component.FileGridMinTileSize
import dev.rk.systemapps.files.ui.component.FileGridTile
import dev.rk.systemapps.files.ui.component.ViewOptionsSheet
import dev.rk.systemapps.files.ui.home.labelRes

@Composable
fun CategoryRoute(
    onNavigateUp: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CategoryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onOpenFile = { node -> FileActions.open(context, node) },
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    uiState: CategoryUiState,
    onAction: (CategoryAction) -> Unit,
    onOpenFile: (FileNode) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var optionsVisible by rememberSaveable { mutableStateOf(false) }

    if (optionsVisible) {
        // Klasör içermeyen bir liste: "klasörler üstte" ve "gizli dosyalar"
        // anahtarları burada gösterilmiyor.
        ViewOptionsSheet(
            prefs = uiState.prefs,
            onSetSortBy = { sortBy -> onAction(CategoryAction.SetSortBy(sortBy)) },
            onToggleSortDirection = { onAction(CategoryAction.ToggleSortDirection) },
            onToggleViewMode = { onAction(CategoryAction.ToggleViewMode) },
            onDismiss = { optionsVisible = false },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(uiState.category.labelRes())) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_up),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { optionsVisible = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = stringResource(R.string.action_options),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when {
            uiState.isLoading -> LoadingState(modifier = contentModifier)

            uiState.isEmpty -> EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(R.string.category_empty),
                modifier = contentModifier,
            )

            uiState.prefs.gridMode -> CategoryGrid(
                items = uiState.items,
                onOpenFile = onOpenFile,
                modifier = contentModifier,
            )

            else -> CategoryList(
                items = uiState.items,
                onOpenFile = onOpenFile,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun CategoryList(
    items: List<FileNode>,
    onOpenFile: (FileNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = items, key = { it.id }) { node ->
            AppListItem(
                title = node.name,
                subtitle = "${formatBytes(node.size)} · ${formatDate(node.lastModified)}",
                leading = { FileThumbnail(node = node) },
                onClick = { onOpenFile(node) },
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    items: List<FileNode>,
    onOpenFile: (FileNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = FileGridMinTileSize),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = items, key = { it.id }) { node ->
            FileGridTile(node = node, onClick = { onOpenFile(node) })
        }
    }
}

@Preview(name = "Kategori — liste")
@Preview(name = "Kategori — koyu", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryScreenPreview() {
    SystemAppsTheme {
        CategoryScreen(
            uiState = previewState(),
            onAction = {},
            onOpenFile = {},
            onNavigateUp = {},
        )
    }
}

private fun previewState() = CategoryUiState(
    category = FileCategory.DOCUMENTS,
    items = listOf(
        previewNode("rapor_şubat.pdf", 1_258_291),
        previewNode("bütçe.xlsx", 22_144),
    ),
    isLoading = false,
)

private fun previewNode(name: String, size: Long) = LocalFileNode(
    id = "/storage/emulated/0/Documents/$name",
    name = name,
    isDirectory = false,
    size = size,
    lastModified = 1_770_000_000_000,
    mimeType = null,
    isHidden = false,
)
