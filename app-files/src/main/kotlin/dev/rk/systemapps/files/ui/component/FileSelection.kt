package dev.rk.systemapps.files.ui.component

import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.files.data.operation.FileOperationManager
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import dev.rk.systemapps.files.domain.model.ClipboardMode
import dev.rk.systemapps.files.domain.model.DirectoryStats
import dev.rk.systemapps.files.domain.model.FileClipboard
import dev.rk.systemapps.files.domain.model.FileDetails
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.FileOperation
import dev.rk.systemapps.files.domain.repository.FileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Seçim modunda açılabilen diyaloglar; yeni klasör/dosya buraya girmiyor. */
sealed interface SelectionDialog {
    data class Rename(val node: FileNode) : SelectionDialog
    data class Properties(val details: FileDetails) : SelectionDialog
}

enum class SelectionNameError { ALREADY_EXISTS, INVALID, FAILED }

data class SelectionState(
    val selectedIds: Set<String> = emptySet(),
    val dialog: SelectionDialog? = null,
    val directoryStats: DirectoryStats? = null,
    val nameError: SelectionNameError? = null,
) {
    val active: Boolean get() = selectedIds.isNotEmpty()
}

sealed interface SelectionAction {
    data class Toggle(val id: String) : SelectionAction
    data object SelectAll : SelectionAction
    data object Invert : SelectionAction
    data object Clear : SelectionAction
    data object Copy : SelectionAction
    data object Cut : SelectionAction
    data object Delete : SelectionAction
    data object ShowRename : SelectionAction
    data object ShowProperties : SelectionAction
    data object DismissDialog : SelectionAction
    data class ConfirmName(val name: String) : SelectionAction
}

/**
 * Uzun basma seçimi ve üzerindeki dosya işlemleri. Kategori ekranı ve ana ekranın
 * "son değişenler" listesi bunu paylaşır.
 *
 * Gezgin kendi seçimini ayrıca yönetiyor: orada seçim aynı zamanda yapıştırma
 * hedefiyle ilişkili, süreç ölümünde `SavedStateHandle` ile korunuyor ve "tümünü
 * seç" klasör listelemesine bağlı. Bu iki ekranda o yükler yok, o yüzden burada
 * daha küçük bir uygulama duruyor.
 *
 * [items] çağrıldığı anda ekranda duran listeyi döndürmeli; "tümünü seç" ve
 * "yeniden adlandır" bunun üzerinden çalışır.
 */
class FileSelection(
    private val scope: CoroutineScope,
    private val items: () -> List<FileNode>,
    private val repository: FileRepository,
    private val preferences: FilesPreferences,
    private val operations: FileOperationManager,
) {

    private val _state = MutableStateFlow(SelectionState())
    val state: StateFlow<SelectionState> = _state.asStateFlow()

    /** Seçili öğeler; liste yenilenince kaybolanlar düşer. */
    fun selectedNodes(): List<FileNode> {
        val ids = _state.value.selectedIds
        return items().filter { it.id in ids }
    }

    fun singleSelection(): FileNode? {
        val id = _state.value.selectedIds.singleOrNull() ?: return null
        return items().firstOrNull { it.id == id }
    }

    fun onAction(action: SelectionAction) {
        when (action) {
            is SelectionAction.Toggle -> update { current ->
                if (action.id in current) current - action.id else current + action.id
            }

            SelectionAction.SelectAll -> update { items().mapTo(mutableSetOf()) { it.id } }

            SelectionAction.Invert -> update { current ->
                items().filterNot { it.id in current }.mapTo(mutableSetOf()) { it.id }
            }

            SelectionAction.Clear -> update { emptySet() }

            SelectionAction.Copy -> putOnClipboard(ClipboardMode.COPY)
            SelectionAction.Cut -> putOnClipboard(ClipboardMode.MOVE)

            SelectionAction.Delete -> {
                val targets = _state.value.selectedIds.toList()
                if (targets.isNotEmpty()) {
                    operations.enqueue(FileOperation.Delete(targets))
                    update { emptySet() }
                }
            }

            SelectionAction.ShowRename -> singleSelection()?.let { node ->
                _state.update { it.copy(dialog = SelectionDialog.Rename(node), nameError = null) }
            }

            SelectionAction.ShowProperties -> showProperties()

            SelectionAction.DismissDialog -> _state.update {
                it.copy(dialog = null, directoryStats = null, nameError = null)
            }

            is SelectionAction.ConfirmName -> confirmRename(action.name)
        }
    }

    /** Yeniden adlandırma başarılıysa çağıran ekranın listeyi tazelemesi için. */
    var onRenamed: () -> Unit = {}

    private fun showProperties() {
        val node = singleSelection() ?: return
        scope.launch {
            when (val outcome = repository.details(node.id)) {
                is Outcome.Success -> {
                    _state.update { it.copy(dialog = SelectionDialog.Properties(outcome.value)) }
                    if (node.isDirectory) {
                        // Ağaç dolaşımı pahalı; diyalog açıkken arkada hesaplanır.
                        val stats = repository.directoryStats(node.id)
                        if (stats is Outcome.Success) {
                            _state.update { it.copy(directoryStats = stats.value) }
                        }
                    }
                }

                is Outcome.Failure -> _state.update {
                    it.copy(nameError = SelectionNameError.FAILED)
                }
            }
        }
    }

    private fun confirmRename(name: String) {
        val dialog = _state.value.dialog as? SelectionDialog.Rename ?: return
        scope.launch {
            when (val outcome = repository.rename(dialog.node.id, name)) {
                is Outcome.Success -> {
                    _state.value = SelectionState()
                    onRenamed()
                }

                is Outcome.Failure -> _state.update {
                    it.copy(nameError = outcome.toNameError())
                }
            }
        }
    }

    private fun putOnClipboard(mode: ClipboardMode) {
        val paths = _state.value.selectedIds.toList()
        if (paths.isEmpty()) return
        scope.launch {
            // Yapıştırma hedefi burada yok; kullanıcı gezginde bir klasöre gidip yapıştırır.
            preferences.setClipboard(FileClipboard(paths, mode))
            update { emptySet() }
        }
    }

    private fun update(transform: (Set<String>) -> Set<String>) {
        _state.update { it.copy(selectedIds = transform(it.selectedIds)) }
    }

    private fun Outcome.Failure.toNameError(): SelectionNameError = when {
        throwable is IllegalArgumentException -> SelectionNameError.INVALID
        message?.contains("zaten var") == true -> SelectionNameError.ALREADY_EXISTS
        else -> SelectionNameError.FAILED
    }
}
