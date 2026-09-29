package dev.rk.systemapps.files.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.files.data.file.FileNodeSorter
import dev.rk.systemapps.files.data.media.MediaCatalog
import dev.rk.systemapps.files.data.operation.FileOperationManager
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.FileCategory
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.model.OperationProgress
import dev.rk.systemapps.files.domain.model.SortBy
import dev.rk.systemapps.files.domain.repository.FileRepository
import dev.rk.systemapps.files.ui.component.FileSelection
import dev.rk.systemapps.files.ui.component.SelectionAction
import dev.rk.systemapps.files.ui.component.SelectionState
import dev.rk.systemapps.files.ui.Routes
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CategoryUiState(
    val category: FileCategory,
    val items: List<FileNode> = emptyList(),
    val isLoading: Boolean = true,
    val prefs: BrowserPrefs = BrowserPrefs(),
    val selection: SelectionState = SelectionState(),
    val operation: OperationProgress? = null,
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    val selectedNodes: List<FileNode> get() = items.filter { it.id in selection.selectedIds }

    val singleSelection: Boolean get() = selection.selectedIds.size == 1
}

sealed interface CategoryAction {
    data class SetSortBy(val sortBy: SortBy) : CategoryAction
    data object ToggleSortDirection : CategoryAction
    data object ToggleViewMode : CategoryAction

    /** Uzun basma seçimi ve üzerindeki dosya işlemleri (ui/component/FileSelection.kt). */
    data class Selection(val action: SelectionAction) : CategoryAction
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val mediaCatalog: MediaCatalog,
    private val preferences: FilesPreferences,
    private val dispatchers: DispatcherProvider,
    repository: FileRepository,
    private val operations: FileOperationManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val category: FileCategory = FileCategory.valueOf(
        checkNotNull(savedStateHandle[Routes.ARG_CATEGORY]) {
            "Kategori rotası ${Routes.ARG_CATEGORY} argümanı olmadan açılamaz"
        },
    )

    /**
     * MediaStore'dan gelen ham liste. Sorgu bir kez çalışır, sıralama tercihi
     * değişince yeniden sorgulanmaz — yalnızca bu liste yeniden sıralanır.
     */
    private val source = MutableStateFlow<List<FileNode>?>(null)

    /** Aksiyonlar [uiState]'e değil buna bakar; uiState yalnızca abone varken güncellenir. */
    private var latestPrefs = BrowserPrefs()

    private var sortedItems: List<FileNode> = emptyList()

    private val selection = FileSelection(
        scope = viewModelScope,
        items = { sortedItems },
        repository = repository,
        preferences = preferences,
        operations = operations,
    ).apply { onRenamed = { reload() } }

    val uiState: StateFlow<CategoryUiState> = combine(
        source,
        preferences.browserPrefs,
        selection.state,
        operations.state,
    ) { nodes, prefs, selectionState, queue ->
        val items = nodes?.let { sort(it, prefs) }.orEmpty()
        sortedItems = items
        val present = items.mapTo(HashSet(items.size)) { it.id }
        CategoryUiState(
            category = category,
            items = items,
            isLoading = nodes == null,
            prefs = prefs,
            // Silinen öğeler seçili kalmamalı.
            selection = selectionState.copy(
                selectedIds = selectionState.selectedIds.filterTo(mutableSetOf()) { it in present },
            ),
            operation = queue.current,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryUiState(category = category),
    )

    init {
        reload()
        viewModelScope.launch {
            preferences.browserPrefs.collect { latestPrefs = it }
        }
        viewModelScope.launch {
            // Silme/taşıma bitince MediaStore sonuçları değişmiş olabilir.
            operations.completions.collect { reload() }
        }
    }

    private fun reload() {
        viewModelScope.launch {
            val items = withContext(dispatchers.io) { mediaCatalog.query(category) }
            source.value = items
            sortedItems = sort(items, latestPrefs)
        }
    }

    fun onAction(action: CategoryAction) {
        when (action) {
            is CategoryAction.SetSortBy -> updatePrefs { it.copy(sortBy = action.sortBy) }
            CategoryAction.ToggleSortDirection -> updatePrefs { it.copy(ascending = !it.ascending) }
            CategoryAction.ToggleViewMode -> updatePrefs { it.copy(gridMode = !it.gridMode) }
            is CategoryAction.Selection -> selection.onAction(action.action)
        }
    }

    /**
     * Kategori listesinde klasör yok ve MediaStore gizli dosyaları zaten indekslemiyor,
     * bu yüzden tercihlerden yalnızca ölçüt ve yön alınır; "klasörler üstte" ile
     * "gizli dosyalar" anahtarları bu ekranda gösterilmiyor (docs/files/SPEC.md §3.1).
     */
    private fun sort(nodes: List<FileNode>, prefs: BrowserPrefs): List<FileNode> =
        FileNodeSorter.apply(
            nodes = nodes,
            options = ListingOptions(
                sortBy = prefs.sortBy,
                ascending = prefs.ascending,
                showHidden = true,
                foldersFirst = false,
            ),
        )

    private fun updatePrefs(transform: (BrowserPrefs) -> BrowserPrefs) {
        viewModelScope.launch {
            preferences.setBrowserPrefs(transform(latestPrefs))
        }
    }
}
