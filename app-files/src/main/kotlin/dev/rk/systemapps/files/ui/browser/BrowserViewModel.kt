package dev.rk.systemapps.files.ui.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.repository.FileRepository
import dev.rk.systemapps.files.ui.Routes
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val gridMode: Boolean = false,
    val options: ListingOptions = ListingOptions(),
) {
    val isEmpty: Boolean get() = !isLoading && error == null && items.isEmpty()
}

sealed interface BrowserAction {
    data object Reload : BrowserAction
    data object ToggleViewMode : BrowserAction
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val repository: FileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val path: String = checkNotNull(savedStateHandle[Routes.ARG_PATH]) {
        "Gezgin rotası ${Routes.ARG_PATH} argümanı olmadan açılamaz"
    }

    private val _uiState = MutableStateFlow(
        BrowserUiState(path = path, crumbs = Crumb.fromPath(path)),
    )
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private var listingJob: Job? = null

    init {
        load()
    }

    fun onAction(action: BrowserAction) {
        when (action) {
            BrowserAction.Reload -> load()
            BrowserAction.ToggleViewMode ->
                // F-1.4'te bu tercih DataStore'a yazılacak; şimdilik oturum boyunca geçerli.
                _uiState.update { it.copy(gridMode = !it.gridMode) }
        }
    }

    private fun load() {
        listingJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }

        listingJob = viewModelScope.launch {
            repository.list(path, _uiState.value.options).collect { outcome ->
                _uiState.update { state ->
                    when (outcome) {
                        is Outcome.Success -> state.copy(
                            items = outcome.value,
                            isLoading = false,
                            error = null,
                        )

                        is Outcome.Failure -> state.copy(
                            items = emptyList(),
                            isLoading = false,
                            error = outcome.toBrowserError(),
                        )
                    }
                }
            }
        }
    }

    private fun Outcome.Failure.toBrowserError(): BrowserError = when (throwable) {
        is FileNotFoundException -> BrowserError.NOT_FOUND
        is java.io.IOException -> BrowserError.NOT_READABLE
        else -> BrowserError.UNKNOWN
    }
}
