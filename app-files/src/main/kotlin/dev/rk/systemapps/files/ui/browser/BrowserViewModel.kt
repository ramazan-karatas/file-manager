package dev.rk.systemapps.files.ui.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.core.storage.volume.StorageLocations
import dev.rk.systemapps.files.data.operation.FileOperationManager
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.ClipboardMode
import dev.rk.systemapps.files.domain.model.Conflict
import dev.rk.systemapps.files.domain.model.ConflictDecision
import dev.rk.systemapps.files.domain.model.FileClipboard
import dev.rk.systemapps.files.domain.model.FileOperation
import dev.rk.systemapps.files.domain.model.OperationProgress
import dev.rk.systemapps.files.domain.model.OperationState
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.SortBy
import dev.rk.systemapps.files.domain.repository.FileRepository
import dev.rk.systemapps.files.ui.Routes
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BrowserError {
    NOT_FOUND,
    NOT_READABLE,
    UNKNOWN,
}

data class BrowserUiState(
    val path: String = "",
    val crumbs: List<Crumb> = emptyList(),
    val items: List<FileNode> = emptyList(),
    val isLoading: Boolean = true,
    val error: BrowserError? = null,
    val prefs: BrowserPrefs = BrowserPrefs(),
    val selectedIds: Set<String> = emptySet(),
    val clipboard: FileClipboard? = null,
    val operation: OperationProgress? = null,
    val conflict: Conflict? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && items.isEmpty()

    val selectionActive: Boolean get() = selectedIds.isNotEmpty()

    val allSelected: Boolean get() = items.isNotEmpty() && selectedIds.size == items.size

    val hasClipboard: Boolean get() = clipboard?.isEmpty == false
}

sealed interface BrowserAction {
    data object Reload : BrowserAction

    // F-1.4 — tercihler
    data class SetSortBy(val sortBy: SortBy) : BrowserAction
    data object ToggleSortDirection : BrowserAction
    data object ToggleViewMode : BrowserAction
    data object ToggleShowHidden : BrowserAction
    data object ToggleFoldersFirst : BrowserAction

    // F-1.5 — seçim
    data class ToggleSelection(val id: String) : BrowserAction
    data object SelectAll : BrowserAction
    data object InvertSelection : BrowserAction
    data object ClearSelection : BrowserAction

    // F-1.7 — dosya işlemleri
    data object CopySelection : BrowserAction
    data object CutSelection : BrowserAction
    data object DeleteSelection : BrowserAction
    data object Paste : BrowserAction
    data object ClearClipboard : BrowserAction
    data class ResolveConflict(val decision: ConflictDecision) : BrowserAction
    data object CancelOperation : BrowserAction
}

/** Tek seferlik bildirimler (özet mesajı gibi); duruma yazılmaz ki tekrar gösterilmesin. */
sealed interface BrowserEvent {
    data class OperationFinished(
        val state: OperationState,
        val failureCount: Int,
    ) : BrowserEvent
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val repository: FileRepository,
    private val preferences: FilesPreferences,
    private val operations: FileOperationManager,
    storageLocations: StorageLocations,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val path: String = checkNotNull(savedStateHandle[Routes.ARG_PATH]) {
        "Gezgin rotası ${Routes.ARG_PATH} argümanı olmadan açılamaz"
    }

    private val crumbs = Crumb.fromPath(path, storageLocations.primaryExternalStorage())

    private val listing = MutableStateFlow(Listing())

    private val _events = MutableSharedFlow<BrowserEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BrowserEvent> = _events.asSharedFlow()

    /**
     * En son okunan tercihler. [uiState] yalnızca abone varken güncellendiği için
     * aksiyonlar ondan değil bu alandan okur.
     */
    private var latestPrefs = BrowserPrefs()

    /**
     * Seçim [SavedStateHandle] üzerinde tutulur: ekran döndürmede ve proses ölümünde korunur
     * (docs/files/PLAN.md F-1.5).
     */
    private val selectedIds: StateFlow<List<String>> =
        savedStateHandle.getStateFlow(KEY_SELECTED_IDS, emptyList())

    private val operationState = combine(
        operations.state,
        operations.pendingConflict,
    ) { queue, conflict -> queue.current to conflict }

    val uiState: StateFlow<BrowserUiState> = combine(
        listing,
        preferences.browserPrefs,
        selectedIds,
        preferences.clipboard,
        operationState,
    ) { listingState, prefs, selected, clipboard, (progress, conflict) ->
        val presentIds = listingState.items.mapTo(HashSet(listingState.items.size)) { it.id }
        BrowserUiState(
            path = path,
            crumbs = crumbs,
            items = listingState.items,
            isLoading = listingState.isLoading,
            error = listingState.error,
            prefs = prefs,
            // Silinen/kaybolan öğeler seçili kalmamalı.
            selectedIds = selected.filterTo(mutableSetOf()) { it in presentIds },
            clipboard = clipboard,
            operation = progress,
            conflict = conflict,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BrowserUiState(path = path, crumbs = crumbs),
    )

    init {
        viewModelScope.launch {
            // Bir işlem bitince klasör içeriği değişmiş olabilir.
            operations.completions.collect { outcome ->
                loadWith(latestPrefs)
                _events.emit(
                    BrowserEvent.OperationFinished(outcome.state, outcome.failures.size),
                )
            }
        }

        viewModelScope.launch {
            // Tercih değişince listeleme baştan yapılır; collectLatest öncekini iptal eder.
            preferences.browserPrefs.collectLatest { prefs ->
                latestPrefs = prefs
                loadWith(prefs)
            }
        }
    }

    fun onAction(action: BrowserAction) {
        when (action) {
            BrowserAction.Reload -> viewModelScope.launch { loadWith(latestPrefs) }

            is BrowserAction.SetSortBy -> updatePrefs { it.copy(sortBy = action.sortBy) }
            BrowserAction.ToggleSortDirection -> updatePrefs { it.copy(ascending = !it.ascending) }
            BrowserAction.ToggleViewMode -> updatePrefs { it.copy(gridMode = !it.gridMode) }
            BrowserAction.ToggleShowHidden -> updatePrefs { it.copy(showHidden = !it.showHidden) }
            BrowserAction.ToggleFoldersFirst ->
                updatePrefs { it.copy(foldersFirst = !it.foldersFirst) }

            is BrowserAction.ToggleSelection -> setSelection { current ->
                if (action.id in current) current - action.id else current + action.id
            }

            BrowserAction.SelectAll -> setSelection { listing.value.items.map { it.id } }

            BrowserAction.InvertSelection -> setSelection { current ->
                listing.value.items.map { it.id }.filterNot { it in current }
            }

            BrowserAction.ClearSelection -> setSelection { emptyList() }

            BrowserAction.CopySelection -> putOnClipboard(ClipboardMode.COPY)
            BrowserAction.CutSelection -> putOnClipboard(ClipboardMode.MOVE)

            BrowserAction.DeleteSelection -> {
                val targets = selectedIds.value.toList()
                if (targets.isNotEmpty()) {
                    operations.enqueue(FileOperation.Delete(targets))
                    setSelection { emptyList() }
                }
            }

            BrowserAction.Paste -> paste()

            BrowserAction.ClearClipboard -> viewModelScope.launch {
                preferences.setClipboard(null)
            }

            is BrowserAction.ResolveConflict -> operations.resolveConflict(action.decision)

            BrowserAction.CancelOperation -> operations.cancelCurrent()
        }
    }

    private fun putOnClipboard(mode: ClipboardMode) {
        val paths = selectedIds.value.toList()
        if (paths.isEmpty()) return
        viewModelScope.launch {
            preferences.setClipboard(FileClipboard(paths, mode))
            setSelection { emptyList() }
        }
    }

    private fun paste() {
        viewModelScope.launch {
            val clipboard = preferences.clipboard.first() ?: return@launch
            if (clipboard.isEmpty) return@launch

            val operation = when (clipboard.mode) {
                ClipboardMode.COPY -> FileOperation.Copy(clipboard.paths, path)
                ClipboardMode.MOVE -> FileOperation.Move(clipboard.paths, path)
            }
            operations.enqueue(operation)

            // Taşımada pano tüketilir; kopyalamada birden fazla yere yapıştırılabilsin
            // diye korunur (MIUI de böyle davranıyor).
            if (clipboard.mode == ClipboardMode.MOVE) preferences.setClipboard(null)
        }
    }

    private suspend fun loadWith(prefs: BrowserPrefs) {
        listing.update { it.copy(isLoading = true, error = null) }
        repository.list(path, prefs.listingOptions).collect { outcome ->
            listing.update {
                when (outcome) {
                    is Outcome.Success -> Listing(items = outcome.value, isLoading = false)
                    is Outcome.Failure -> Listing(isLoading = false, error = outcome.toBrowserError())
                }
            }
        }
    }

    private fun updatePrefs(transform: (BrowserPrefs) -> BrowserPrefs) {
        viewModelScope.launch {
            preferences.setBrowserPrefs(transform(latestPrefs))
        }
    }

    private fun setSelection(transform: (List<String>) -> List<String>) {
        savedStateHandle[KEY_SELECTED_IDS] = ArrayList(transform(selectedIds.value))
    }

    private fun Outcome.Failure.toBrowserError(): BrowserError = when (throwable) {
        is FileNotFoundException -> BrowserError.NOT_FOUND
        is IOException -> BrowserError.NOT_READABLE
        else -> BrowserError.UNKNOWN
    }

    private data class Listing(
        val items: List<FileNode> = emptyList(),
        val isLoading: Boolean = true,
        val error: BrowserError? = null,
    )

    private companion object {
        const val KEY_SELECTED_IDS = "browser_selected_ids"
    }
}
