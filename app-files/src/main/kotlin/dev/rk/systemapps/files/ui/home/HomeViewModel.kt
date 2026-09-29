package dev.rk.systemapps.files.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.core.storage.model.StorageVolumeInfo
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.StoragePermissionChecker
import dev.rk.systemapps.core.storage.volume.StorageLocations
import dev.rk.systemapps.files.data.media.MediaCatalog
import dev.rk.systemapps.files.data.operation.FileOperationManager
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.OperationProgress
import dev.rk.systemapps.files.domain.repository.FileRepository
import dev.rk.systemapps.files.ui.component.FileSelection
import dev.rk.systemapps.files.ui.component.SelectionAction
import dev.rk.systemapps.files.ui.component.SelectionState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val accessLevel: StorageAccessLevel = StorageAccessLevel.NONE,
    val storageRoot: String = "",
    val volumes: List<StorageVolumeInfo> = emptyList(),
    val recent: List<FileNode> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val operation: OperationProgress? = null,
) {
    val showLimitedAccessBanner: Boolean get() = !accessLevel.canBrowseFileSystem

    val selectedNodes: List<FileNode> get() = recent.filter { it.id in selection.selectedIds }

    val singleSelection: Boolean get() = selection.selectedIds.size == 1
}

sealed interface HomeAction {
    data object Refresh : HomeAction

    /** Uzun basma seçimi; yalnızca "son değişenler" listesinde geçerli. */
    data class Selection(val action: SelectionAction) : HomeAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionChecker: StoragePermissionChecker,
    private val storageLocations: StorageLocations,
    private val mediaStore: MediaCatalog,
    private val dispatchers: DispatcherProvider,
    private val operations: FileOperationManager,
    repository: FileRepository,
    preferences: FilesPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            accessLevel = permissionChecker.accessLevel.value,
            storageRoot = storageLocations.primaryExternalStorage(),
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val selection = FileSelection(
        scope = viewModelScope,
        items = { _uiState.value.recent },
        repository = repository,
        preferences = preferences,
        operations = operations,
    ).apply { onRenamed = { refresh() } }

    init {
        refresh()

        viewModelScope.launch {
            selection.state.collect { state ->
                // Silinen öğeler seçili kalmamalı.
                val present = _uiState.value.recent.mapTo(mutableSetOf()) { it.id }
                _uiState.update {
                    it.copy(
                        selection = state.copy(
                            selectedIds = state.selectedIds.filterTo(mutableSetOf()) {
                                id -> id in present
                            },
                        ),
                    )
                }
            }
        }

        viewModelScope.launch {
            operations.state.collect { queue ->
                _uiState.update { it.copy(operation = queue.current) }
            }
        }

        viewModelScope.launch {
            // Silme bitince "son değişenler" listesi değişmiş olabilir.
            operations.completions.collect { refresh() }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Refresh -> refresh()
            is HomeAction.Selection -> selection.onAction(action.action)
        }
    }

    private fun refresh() {
        val level = permissionChecker.refresh()
        _uiState.update { it.copy(accessLevel = level) }

        viewModelScope.launch {
            // Birim listesi ve MediaStore sorgusu disk erişimi; ana thread'de olmaz.
            val volumes = withContext(dispatchers.io) { storageLocations.volumes() }
            val recent = withContext(dispatchers.io) {
                if (level.canReadMedia) mediaStore.recent(limit = RECENT_LIMIT) else emptyList()
            }
            _uiState.update { it.copy(volumes = volumes, recent = recent) }
        }
    }

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
