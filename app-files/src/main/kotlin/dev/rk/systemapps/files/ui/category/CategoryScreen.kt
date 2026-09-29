package dev.rk.systemapps.files.ui.category

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import dev.rk.systemapps.files.ui.browser.OperationProgressBar
import dev.rk.systemapps.files.ui.component.FileSelectionTopBar
import dev.rk.systemapps.files.ui.component.SelectionAction
import dev.rk.systemapps.files.ui.component.SelectionDialogs
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
import dev.rk.systemapps.core.design.component.SkeletonGrid
import dev.rk.systemapps.core.design.component.SkeletonList
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

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Reddedilse de işlem sürer; yalnızca ilerleme bildirimi görünmez. */ }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }

    CategoryScreen(
        uiState = uiState,
        onAction = { action ->
            // Bildirim izni, ilk gerçek dosya işleminde isteniyor: bağlamsız sorulmuyor.
            if (action.startsOperation() && !notificationAsked) {
                notificationAsked = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            viewModel.onAction(action)
        },
        onOpenFile = { node -> FileActions.open(context, node) },
        onShare = { nodes -> FileActions.share(context, nodes) },
        onNavigateUp = onNavigateUp,
    )
}

private fun CategoryAction.startsOperation(): Boolean =
    this is CategoryAction.Selection && action is SelectionAction.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    uiState: CategoryUiState,
    onAction: (CategoryAction) -> Unit,
    onOpenFile: (FileNode) -> Unit,
    onShare: (List<FileNode>) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var optionsVisible by rememberSaveable { mutableStateOf(false) }
    var deleteRequested by remember { mutableStateOf(false) }

    val selection = uiState.selection
    val onSelection: (SelectionAction) -> Unit = { onAction(CategoryAction.Selection(it)) }

    // Seçim modundayken geri tuşu önce seçimi kapatır (docs/files/SPEC.md §3.2).
    BackHandler(enabled = selection.active) { onSelection(SelectionAction.Clear) }

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

    SelectionDialogs(
        state = selection,
        deleteRequested = deleteRequested,
        onDeleteConfirm = {
            deleteRequested = false
            onSelection(SelectionAction.Delete)
        },
        onDeleteDismiss = { deleteRequested = false },
        onAction = onSelection,
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            uiState.operation?.let { progress ->
                OperationProgressBar(progress = progress, onCancel = {})
            }
        },
        topBar = {
            if (selection.active) {
                FileSelectionTopBar(
                    selectedCount = selection.selectedIds.size,
                    singleSelection = uiState.singleSelection,
                    onAction = onSelection,
                    onShare = { onShare(uiState.selectedNodes) },
                    onDeleteRequest = { deleteRequested = true },
                )
            } else {
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
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        val onItemClick: (FileNode) -> Unit = { node ->
            // Seçim modundayken tıklama seçer, açmaz.
            if (selection.active) onSelection(SelectionAction.Toggle(node.id)) else onOpenFile(node)
        }

        when {
            uiState.isLoading -> if (uiState.prefs.gridMode) {
                SkeletonGrid(minTileSize = FileGridMinTileSize, modifier = contentModifier)
            } else {
                SkeletonList(modifier = contentModifier)
            }

            uiState.isEmpty -> EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(R.string.category_empty),
                modifier = contentModifier,
            )

            uiState.prefs.gridMode -> CategoryGrid(
                items = uiState.items,
                selectedIds = selection.selectedIds,
                onItemClick = onItemClick,
                onLongClick = { node -> onSelection(SelectionAction.Toggle(node.id)) },
                modifier = contentModifier,
            )

            else -> CategoryList(
                items = uiState.items,
                selectedIds = selection.selectedIds,
                onItemClick = onItemClick,
                onLongClick = { node -> onSelection(SelectionAction.Toggle(node.id)) },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun CategoryList(
    items: List<FileNode>,
    selectedIds: Set<String>,
    onItemClick: (FileNode) -> Unit,
    onLongClick: (FileNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = items, key = { it.id }) { node ->
            AppListItem(
                title = node.name,
                subtitle = "${formatBytes(node.size)} · ${formatDate(node.lastModified)}",
                leading = { FileThumbnail(node = node) },
                selected = node.id in selectedIds,
                onClick = { onItemClick(node) },
                onLongClick = { onLongClick(node) },
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    items: List<FileNode>,
    selectedIds: Set<String>,
    onItemClick: (FileNode) -> Unit,
    onLongClick: (FileNode) -> Unit,
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
            FileGridTile(
                node = node,
                selected = node.id in selectedIds,
                onClick = { onItemClick(node) },
                onLongClick = { onLongClick(node) },
            )
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
            onShare = {},
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
