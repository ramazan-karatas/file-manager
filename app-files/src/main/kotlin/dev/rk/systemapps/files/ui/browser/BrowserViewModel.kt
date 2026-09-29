package dev.rk.systemapps.files.ui.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.core.storage.volume.StorageLocations
import dev.rk.systemapps.files.data.operation.FileOperationManager
import dev.rk.systemapps.files.data.file.FileNodeSorter
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.ClipboardMode
import dev.rk.systemapps.files.domain.model.Conflict
import dev.rk.systemapps.files.domain.model.DirectoryStats
import dev.rk.systemapps.files.domain.model.FileDetails
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BrowserError {
    NOT_FOUND,
    NOT_READABLE,
    UNKNOWN,
}

/** Ad girişi hatası; metin UI tarafında çözülür. */
enum class NameError {
    ALREADY_EXISTS,
    INVALID,
    FAILED,
}

sealed interface BrowserDialog {
    data class Rename(val node: FileNode) : BrowserDialog
    data object NewFolder : BrowserDialog
    data object NewFile : BrowserDialog
    data class Properties(val details: FileDetails) : BrowserDialog
}

data class BrowserUiState(
    val path: String = "",
    val crumbs: List<Crumb> = emptyList(),
    val items: List<FileNode> = emptyList(),
    val isLoading: Boolean = true,
    /** Liste ekranda dururken arkada yeniden okunuyor; tam ekran spinner gösterilmez. */
    val isRefreshing: Boolean = false,
    val error: BrowserError? = null,
    val prefs: BrowserPrefs = BrowserPrefs(),
    val selectedIds: Set<String> = emptySet(),
    val clipboard: FileClipboard? = null,
    val operation: OperationProgress? = null,
    val conflict: Conflict? = null,
    val dialog: BrowserDialog? = null,
    /** Özellikler diyaloğundaki klasör boyutu; hesaplanana kadar null. */
    val directoryStats: DirectoryStats? = null,
    val nameError: NameError? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && items.isEmpty()

    val selectionActive: Boolean get() = selectedIds.isNotEmpty()

    val allSelected: Boolean get() = items.isNotEmpty() && selectedIds.size == items.size

    val hasClipboard: Boolean get() = clipboard?.isEmpty == false

    /** Yeniden adlandırma ve özellikler yalnızca tek öğe seçiliyken anlamlı. */
    val singleSelection: FileNode?
        get() = items.singleOrNull { it.id in selectedIds }?.takeIf { selectedIds.size == 1 }

    val selectedNodes: List<FileNode> get() = items.filter { it.id in selectedIds }
}

sealed interface BrowserAction {
    data object Reload : BrowserAction

    /** Aşağı çekerek yenileme: kullanıcı açıkça istedi, koşulsuz okunur. */
    data object Refresh : BrowserAction

    /**
     * Ekran öne geldiğinde çağrılır. Klasörün kendi değişiklik damgası aynıysa
     * hiç okumaz; n dosya yerine tek `stat` maliyeti.
     */
    data object RefreshIfChanged : BrowserAction

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

    // F-1.8 — tekil işlemler
    data object ShowRename : BrowserAction
    data object ShowNewFolder : BrowserAction
    data object ShowNewFile : BrowserAction
    data object ShowProperties : BrowserAction
    data object DismissDialog : BrowserAction
    data class ConfirmName(val name: String) : BrowserAction
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
    private val dispatchers: DispatcherProvider,
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

    /** Klasörün son okunduğu andaki değişiklik damgası; [refreshIfChanged] buna bakar. */
    private var directoryStamp: Long? = null

    /**
     * Seçim [SavedStateHandle] üzerinde tutulur: ekran döndürmede ve proses ölümünde korunur
     * (docs/files/PLAN.md F-1.5).
     */
    private val selectedIds: StateFlow<List<String>> =
        savedStateHandle.getStateFlow(KEY_SELECTED_IDS, emptyList())

    private val localState = MutableStateFlow(LocalState())

    private val auxState = combine(
        operations.state,
        operations.pendingConflict,
        localState,
    ) { queue, conflict, local -> AuxState(queue.current, conflict, local) }

    /**
     * Süzme + sıralama burada yapılıyor, repository'de değil: tercih değişince
     * klasör yeniden okunmasın diye. Ağır olduğu için [DispatcherProvider.default]
     * üzerinde, `mapLatest` ile — hızlı ardışık değişikliklerde öncekini iptal eder.
     */
    private val content: Flow<Content> = combine(
        listing,
        // Yalnızca sıralamayı etkileyen alanlar: liste↔ızgara geçişi yeniden
        // sıralama tetiklemesin diye gridMode buraya girmiyor.
        preferences.browserPrefs.map { it.listingOptions }.distinctUntilChanged(),
    ) { listingState, options ->
        listingState to options
    }.mapLatest { (listingState, options) ->
        Content(
            items = FileNodeSorter.apply(listingState.items, options),
            isLoading = listingState.isLoading,
            isRefreshing = listingState.isRefreshing,
            error = listingState.error,
        )
    }.flowOn(dispatchers.default)

    val uiState: StateFlow<BrowserUiState> = combine(
        content,
        preferences.browserPrefs,
        selectedIds,
        preferences.clipboard,
        auxState,
    ) { contentState, prefs, selected, clipboard, aux ->
        val presentIds = contentState.items.mapTo(HashSet(contentState.items.size)) { it.id }
        BrowserUiState(
            path = path,
            crumbs = crumbs,
            items = contentState.items,
            isLoading = contentState.isLoading,
            isRefreshing = contentState.isRefreshing,
            error = contentState.error,
            prefs = prefs,
            // Silinen/kaybolan öğeler seçili kalmamalı.
            selectedIds = selected.filterTo(mutableSetOf()) { it in presentIds },
            clipboard = clipboard,
            operation = aux.progress,
            conflict = aux.conflict,
            dialog = aux.local.dialog,
            directoryStats = aux.local.directoryStats,
            nameError = aux.local.nameError,
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
                load()
                _events.emit(
                    BrowserEvent.OperationFinished(outcome.state, outcome.failures.size),
                )
            }
        }

        viewModelScope.launch {
            // Tercihler artık yalnızca sıralamayı etkiliyor; disk okunmuyor.
            preferences.browserPrefs.collect { latestPrefs = it }
        }

        viewModelScope.launch { load() }
    }

    fun onAction(action: BrowserAction) {
        when (action) {
            BrowserAction.Reload -> viewModelScope.launch { load() }
            BrowserAction.Refresh -> viewModelScope.launch { load(asRefresh = true) }
            BrowserAction.RefreshIfChanged -> viewModelScope.launch { refreshIfChanged() }

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

            BrowserAction.ShowRename -> currentSingleSelection()?.let { node ->
                localState.update { it.copy(dialog = BrowserDialog.Rename(node), nameError = null) }
            }

            BrowserAction.ShowNewFolder ->
                localState.update { it.copy(dialog = BrowserDialog.NewFolder, nameError = null) }

            BrowserAction.ShowNewFile ->
                localState.update { it.copy(dialog = BrowserDialog.NewFile, nameError = null) }

            BrowserAction.ShowProperties -> showProperties()

            BrowserAction.DismissDialog -> localState.value = LocalState()

            is BrowserAction.ConfirmName -> confirmName(action.name)
        }
    }

    private fun currentSingleSelection(): FileNode? {
        val selected = selectedIds.value
        if (selected.size != 1) return null
        return listing.value.items.firstOrNull { it.id == selected.single() }
    }

    private fun showProperties() {
        val node = currentSingleSelection() ?: return
        viewModelScope.launch {
            when (val outcome = repository.details(node.id)) {
                is Outcome.Success -> {
                    localState.update {
                        it.copy(dialog = BrowserDialog.Properties(outcome.value))
                    }
                    if (node.isDirectory) {
                        // Ağaç dolaşımı pahalı; diyalog açıkken arkada hesaplanır.
                        val stats = repository.directoryStats(node.id)
                        if (stats is Outcome.Success) {
                            localState.update { it.copy(directoryStats = stats.value) }
                        }
                    }
                }

                is Outcome.Failure -> localState.update { it.copy(nameError = NameError.FAILED) }
            }
        }
    }

    private fun confirmName(name: String) {
        val dialog = localState.value.dialog ?: return
        viewModelScope.launch {
            val outcome = when (dialog) {
                is BrowserDialog.Rename -> repository.rename(dialog.node.id, name)
                BrowserDialog.NewFolder -> repository.createDirectory(path, name)
                BrowserDialog.NewFile -> repository.createFile(path, name)
                is BrowserDialog.Properties -> return@launch
            }

            when (outcome) {
                is Outcome.Success -> {
                    localState.value = LocalState()
                    setSelection { emptyList() }
                    load()
                }

                is Outcome.Failure -> localState.update {
                    it.copy(nameError = outcome.toNameError())
                }
            }
        }
    }

    private fun Outcome.Failure.toNameError(): NameError = when {
        throwable is IllegalArgumentException -> NameError.INVALID
        message?.contains("zaten var") == true -> NameError.ALREADY_EXISTS
        else -> NameError.FAILED
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

    /**
     * [asRefresh] true ise mevcut liste ekranda kalır ve yalnızca yenileme göstergesi
     * döner; false ise tam ekran spinner gösterilir (ilk yükleme ve hata sonrası).
     */
    private suspend fun load(asRefresh: Boolean = false) {
        listing.update {
            if (asRefresh) it.copy(isRefreshing = true) else it.copy(isLoading = true, error = null)
        }
        directoryStamp = readDirectoryStamp()
        repository.list(path).collect { outcome ->
            listing.value = when (outcome) {
                is Outcome.Success -> Listing(items = outcome.value, isLoading = false)
                is Outcome.Failure -> Listing(isLoading = false, error = outcome.toBrowserError())
            }
        }
    }

    /**
     * Ekran öne geldiğinde çağrılır. Klasörün değişiklik damgası aynıysa hiç okumaz:
     * tek `stat`, n dosyalık `readdir` + `stat` yerine.
     */
    private suspend fun refreshIfChanged() {
        // İlk yükleme henüz bitmediyse zaten güncel veri geliyor.
        val previous = directoryStamp ?: return
        if (readDirectoryStamp() != previous) load(asRefresh = true)
    }

    private suspend fun readDirectoryStamp(): Long? =
        (repository.stat(path) as? Outcome.Success)?.value?.lastModified

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

    private data class LocalState(
        val dialog: BrowserDialog? = null,
        val directoryStats: DirectoryStats? = null,
        val nameError: NameError? = null,
    )

    private data class AuxState(
        val progress: OperationProgress?,
        val conflict: Conflict?,
        val local: LocalState,
    )

    /** Diskten geldiği hâliyle; sıralama [uiState] boru hattında yapılıyor. */
    private data class Listing(
        val items: List<FileNode> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: BrowserError? = null,
    )

    /** Sıralanmış içerik; ağır kısım arka planda üretilir. */
    private data class Content(
        val items: List<FileNode>,
        val isLoading: Boolean,
        val isRefreshing: Boolean,
        val error: BrowserError?,
    )

    private companion object {
        const val KEY_SELECTED_IDS = "browser_selected_ids"
    }
}
