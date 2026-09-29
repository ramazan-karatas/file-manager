package dev.rk.systemapps.files.ui.browser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rk.systemapps.core.common.format.formatBytes
import dev.rk.systemapps.core.common.format.formatDate
import dev.rk.systemapps.core.design.component.AppListItem
import dev.rk.systemapps.core.design.component.ConfirmDialog
import dev.rk.systemapps.core.design.component.EmptyState
import dev.rk.systemapps.core.design.component.SkeletonGrid
import dev.rk.systemapps.core.design.component.SkeletonList
import dev.rk.systemapps.core.design.component.SelectionTopBar
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.LocalFileNode
import dev.rk.systemapps.files.domain.model.OperationState
import dev.rk.systemapps.files.ui.component.FileGridMinTileSize
import dev.rk.systemapps.files.ui.component.FileGridTile
import dev.rk.systemapps.files.ui.component.ViewOptionsSheet
import kotlinx.coroutines.launch

@Composable
fun BrowserRoute(
    onNavigateToFolder: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSearch: (String) -> Unit,
    /** Kökten hedefe kadar olan yollar; geri yığını buna göre yeniden kurulur. */
    onNavigateToCrumb: (List<String>) -> Unit,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Liste bellekte tutuluyor; ekran öne geldiğinde klasör dışarıdan değişmiş mi
    // diye bakılır. Damga aynıysa disk hiç okunmaz.
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(BrowserAction.RefreshIfChanged)
        onPauseOrDispose {}
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Reddedilse de işlem sürer; yalnızca ilerleme bildirimi görünmez. */ }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val doneMessage = stringResource(R.string.operation_done)
    val cancelledMessage = stringResource(R.string.operation_cancelled)
    val openFailedMessage = stringResource(R.string.open_failed)
    val shareFailedMessage = stringResource(R.string.share_failed)
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.OperationFinished -> snackbarHostState.showSnackbar(
                    when {
                        event.state == OperationState.CANCELLED -> cancelledMessage
                        event.failureCount > 0 -> resources.getQuantityString(
                            R.plurals.operation_done_with_failures,
                            event.failureCount,
                            event.failureCount,
                        )

                        else -> doneMessage
                    },
                )
            }
        }
    }

    BrowserScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
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
        onItemClick = { node ->
            if (node.isDirectory) {
                onNavigateToFolder(node.id)
            } else if (!FileActions.open(context, node)) {
                scope.launch { snackbarHostState.showSnackbar(openFailedMessage) }
            }
        },
        onShare = { nodes ->
            if (!FileActions.share(context, nodes)) {
                scope.launch { snackbarHostState.showSnackbar(shareFailedMessage) }
            }
        },
        onNavigateUp = onNavigateUp,
        onSearch = { onSearch(uiState.path) },
        onCrumbClick = { crumb ->
            val index = uiState.crumbs.indexOf(crumb)
            if (index >= 0 && index != uiState.crumbs.lastIndex) {
                onNavigateToCrumb(uiState.crumbs.take(index + 1).map { it.path })
            }
        },
    )
}

private fun BrowserAction.startsOperation(): Boolean =
    this is BrowserAction.Paste || this is BrowserAction.DeleteSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    uiState: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    onItemClick: (FileNode) -> Unit,
    onShare: (List<FileNode>) -> Unit,
    onNavigateUp: () -> Unit,
    onSearch: () -> Unit,
    onCrumbClick: (Crumb) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Seçim modundayken geri tuşu önce seçimi kapatır (docs/files/SPEC.md §3.2).
    BackHandler(enabled = uiState.selectionActive) {
        onAction(BrowserAction.ClearSelection)
    }

    var deleteRequested by remember { mutableStateOf(false) }
    var optionsVisible by rememberSaveable { mutableStateOf(false) }

    if (optionsVisible) {
        ViewOptionsSheet(
            prefs = uiState.prefs,
            onSetSortBy = { sortBy -> onAction(BrowserAction.SetSortBy(sortBy)) },
            onToggleSortDirection = { onAction(BrowserAction.ToggleSortDirection) },
            onToggleViewMode = { onAction(BrowserAction.ToggleViewMode) },
            onDismiss = { optionsVisible = false },
            onToggleFoldersFirst = { onAction(BrowserAction.ToggleFoldersFirst) },
            onToggleShowHidden = { onAction(BrowserAction.ToggleShowHidden) },
        )
    }

    if (deleteRequested) {
        ConfirmDialog(
            title = pluralStringResource(
                R.plurals.delete_confirm_title,
                uiState.selectedIds.size,
                uiState.selectedIds.size,
            ),
            message = stringResource(R.string.delete_confirm_message),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                deleteRequested = false
                onAction(BrowserAction.DeleteSelection)
            },
            onDismiss = { deleteRequested = false },
            destructive = true,
        )
    }

    when (val dialog = uiState.dialog) {
        is BrowserDialog.Rename -> NameInputDialog(
            title = stringResource(R.string.dialog_rename_title),
            initialName = dialog.node.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { name -> onAction(BrowserAction.ConfirmName(name)) },
            onDismiss = { onAction(BrowserAction.DismissDialog) },
            errorMessage = uiState.nameError?.let { stringResource(it.messageRes()) },
        )

        BrowserDialog.NewFolder -> NameInputDialog(
            title = stringResource(R.string.dialog_new_folder_title),
            initialName = "",
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = { name -> onAction(BrowserAction.ConfirmName(name)) },
            onDismiss = { onAction(BrowserAction.DismissDialog) },
            errorMessage = uiState.nameError?.let { stringResource(it.messageRes()) },
        )

        BrowserDialog.NewFile -> NameInputDialog(
            title = stringResource(R.string.dialog_new_file_title),
            initialName = "",
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = { name -> onAction(BrowserAction.ConfirmName(name)) },
            onDismiss = { onAction(BrowserAction.DismissDialog) },
            errorMessage = uiState.nameError?.let { stringResource(it.messageRes()) },
        )

        is BrowserDialog.Properties -> PropertiesDialog(
            details = dialog.details,
            stats = uiState.directoryStats,
            onDismiss = { onAction(BrowserAction.DismissDialog) },
        )

        null -> Unit
    }

    uiState.conflict?.let { conflict ->
        ConflictDialog(
            conflict = conflict,
            onDecision = { decision -> onAction(BrowserAction.ResolveConflict(decision)) },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.selectionActive && uiState.error == null) {
                NewItemFab(onAction = onAction)
            }
        },
        bottomBar = {
            // Scaffold yuvası kullanılıyor ki FAB bu çubukların üstünde konumlansın.
            Column {
                uiState.operation?.let { progress ->
                    OperationProgressBar(
                        progress = progress,
                        onCancel = { onAction(BrowserAction.CancelOperation) },
                    )
                }
                uiState.clipboard?.takeIf { !it.isEmpty }?.let { clipboard ->
                    PasteBar(
                        clipboard = clipboard,
                        onPaste = { onAction(BrowserAction.Paste) },
                        onCancel = { onAction(BrowserAction.ClearClipboard) },
                    )
                }
            }
        },
        topBar = {
            if (uiState.selectionActive) {
                SelectionTopBar(
                    selectedCount = uiState.selectedIds.size,
                    onClose = { onAction(BrowserAction.ClearSelection) },
                    closeContentDescription = stringResource(R.string.action_close_selection),
                    actions = {
                        // Üst çubuğa yalnızca sık kullanılan dört işlem sığdırılıyor;
                        // fazlası sayacı ve kapatma düğmesini ekrandan taşırıyordu.
                        IconButton(onClick = { onAction(BrowserAction.CopySelection) }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy),
                            )
                        }
                        IconButton(onClick = { onAction(BrowserAction.CutSelection) }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCut,
                                contentDescription = stringResource(R.string.action_cut),
                            )
                        }
                        IconButton(onClick = { deleteRequested = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                        IconButton(onClick = { onShare(uiState.selectedNodes) }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.action_share),
                            )
                        }
                        SelectionOverflowMenu(uiState = uiState, onAction = onAction)
                    },
                )
            } else {
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
                        IconButton(onClick = onSearch) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.action_search),
                            )
                        }
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
        Column(modifier = Modifier.padding(innerPadding)) {
            Breadcrumbs(crumbs = uiState.crumbs, onCrumbClick = onCrumbClick)

            // İçerik kalan alanı kaplar; ilerleme ve yapıştırma çubukları altta sabit kalır.
            // Aksi hâlde fillMaxSize kullanan boş/yükleniyor durumları çubukları
            // ekran dışına itiyordu.
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onAction(BrowserAction.Refresh) },
                modifier = Modifier.weight(1f),
            ) {
                when {
                    // Spinner yerine gelecek satırların yer tutucusu: düzen
                    // önden görünüyor ve liste gelince zıplama olmuyor.
                    uiState.isLoading -> if (uiState.prefs.gridMode) {
                        SkeletonGrid(minTileSize = FileGridMinTileSize)
                    } else {
                        SkeletonList()
                    }

                    uiState.error != null -> EmptyState(
                        icon = Icons.Outlined.FolderOff,
                        title = stringResource(uiState.error.titleRes()),
                        description = uiState.path,
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = { onAction(BrowserAction.Reload) },
                    )

                    // Boş klasörde de aşağı çekilebilsin diye kaydırılabilir kılınıyor;
                    // kaydırma olayı olmadan yenileme hareketi tetiklenmiyor.
                    uiState.isEmpty -> EmptyState(
                        icon = Icons.Outlined.FolderOff,
                        title = stringResource(R.string.browser_empty_title),
                        description = stringResource(R.string.browser_empty_description),
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )

                    uiState.prefs.gridMode -> FileGrid(
                        uiState = uiState,
                        onItemClick = onItemClick,
                        onAction = onAction,
                    )

                    else -> FileList(
                        uiState = uiState,
                        onItemClick = onItemClick,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionOverflowMenu(
    uiState: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val singleSelection = uiState.singleSelection != null

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.action_more),
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        // Yeniden adlandırma ve özellikler yalnızca tek öğede anlamlı.
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_rename)) },
            enabled = singleSelection,
            onClick = {
                expanded = false
                onAction(BrowserAction.ShowRename)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_properties)) },
            enabled = singleSelection,
            onClick = {
                expanded = false
                onAction(BrowserAction.ShowProperties)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_select_all)) },
            onClick = {
                expanded = false
                onAction(BrowserAction.SelectAll)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_invert_selection)) },
            onClick = {
                expanded = false
                onAction(BrowserAction.InvertSelection)
            },
        )
    }
}

@Composable
private fun NewItemFab(onAction: (BrowserAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.action_new),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_new_folder)) },
                onClick = {
                    expanded = false
                    onAction(BrowserAction.ShowNewFolder)
                },
                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_new_file)) },
                onClick = {
                    expanded = false
                    onAction(BrowserAction.ShowNewFile)
                },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null) },
            )
        }
    }
}

private fun NameError.messageRes(): Int = when (this) {
    NameError.ALREADY_EXISTS -> R.string.name_error_exists
    NameError.INVALID -> R.string.name_error_invalid
    NameError.FAILED -> R.string.name_error_failed
}

@Composable
private fun BrowserUiState.currentTitle(): String {
    val crumb = crumbs.lastOrNull() ?: return stringResource(R.string.home_title)
    return if (crumb.isStorageRoot) stringResource(R.string.storage_internal) else crumb.name
}

@Composable
private fun FileList(
    uiState: BrowserUiState,
    onItemClick: (FileNode) -> Unit,
    onAction: (BrowserAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        // key: yeniden sıralamada ve yenilemede satırların kimliğini korur.
        items(items = uiState.items, key = { it.id }) { node ->
            AppListItem(
                title = node.name,
                subtitle = node.subtitle(),
                leading = { FileThumbnail(node = node) },
                selected = node.id in uiState.selectedIds,
                onClick = {
                    // Seçim modundayken tıklama seçer, gezmez.
                    if (uiState.selectionActive) {
                        onAction(BrowserAction.ToggleSelection(node.id))
                    } else {
                        onItemClick(node)
                    }
                },
                onLongClick = { onAction(BrowserAction.ToggleSelection(node.id)) },
            )
        }
    }
}

@Composable
private fun FileGrid(
    uiState: BrowserUiState,
    onItemClick: (FileNode) -> Unit,
    onAction: (BrowserAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = FileGridMinTileSize),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = uiState.items, key = { it.id }) { node ->
            FileGridTile(
                node = node,
                selected = node.id in uiState.selectedIds,
                onClick = {
                    if (uiState.selectionActive) {
                        onAction(BrowserAction.ToggleSelection(node.id))
                    } else {
                        onItemClick(node)
                    }
                },
                onLongClick = { onAction(BrowserAction.ToggleSelection(node.id)) },
            )
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
            uiState = previewState(),
            onAction = {},
            onItemClick = {},
            onShare = {},
            onNavigateUp = {},
            onSearch = {},
            onCrumbClick = {},
        )
    }
}

@Preview(name = "Gezgin — seçim modu")
@Composable
private fun BrowserScreenSelectionPreview() {
    SystemAppsTheme {
        val state = previewState()
        BrowserScreen(
            uiState = state.copy(selectedIds = setOf(state.items[1].id)),
            onAction = {},
            onItemClick = {},
            onShare = {},
            onNavigateUp = {},
            onSearch = {},
            onCrumbClick = {},
        )
    }
}

private fun previewState() = BrowserUiState(
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
)

private fun previewNode(name: String, isDirectory: Boolean = false, size: Long = 0) = LocalFileNode(
    id = "/storage/emulated/0/Belgeler/$name",
    name = name,
    isDirectory = isDirectory,
    size = if (isDirectory) FileNode.SIZE_UNKNOWN else size,
    lastModified = 1_770_000_000_000,
    mimeType = null,
    isHidden = false,
)
