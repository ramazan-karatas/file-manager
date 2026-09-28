package dev.rk.systemapps.files.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.files.data.file.FileNodeSorter
import dev.rk.systemapps.files.data.media.MediaCatalog
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.FileCategory
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.model.SortBy
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
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

/** Kategori ekranı yalnızca görünümü etkiler; dosya işlemi yapmaz. */
sealed interface CategoryAction {
    data class SetSortBy(val sortBy: SortBy) : CategoryAction
    data object ToggleSortDirection : CategoryAction
    data object ToggleViewMode : CategoryAction
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val mediaCatalog: MediaCatalog,
    private val preferences: FilesPreferences,
    private val dispatchers: DispatcherProvider,
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

    val uiState: StateFlow<CategoryUiState> = combine(
        source,
        preferences.browserPrefs,
    ) { nodes, prefs ->
        CategoryUiState(
            category = category,
            items = nodes?.let { sort(it, prefs) }.orEmpty(),
            isLoading = nodes == null,
            prefs = prefs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryUiState(category = category),
    )

    init {
        viewModelScope.launch {
            source.value = withContext(dispatchers.io) { mediaCatalog.query(category) }
        }
        viewModelScope.launch {
            preferences.browserPrefs.collect { latestPrefs = it }
        }
    }

    fun onAction(action: CategoryAction) {
        when (action) {
            is CategoryAction.SetSortBy -> updatePrefs { it.copy(sortBy = action.sortBy) }
            CategoryAction.ToggleSortDirection -> updatePrefs { it.copy(ascending = !it.ascending) }
            CategoryAction.ToggleViewMode -> updatePrefs { it.copy(gridMode = !it.gridMode) }
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
