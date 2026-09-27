package dev.rk.systemapps.files.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.repository.FileRepository
import dev.rk.systemapps.files.ui.Routes
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

data class SearchUiState(
    val root: String = "",
    val query: String = "",
    val results: List<FileNode> = emptyList(),
    val isSearching: Boolean = false,
) {
    val isEmpty: Boolean get() = query.isNotBlank() && !isSearching && results.isEmpty()
}

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data object ClearQuery : SearchAction
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: FileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val root: String = checkNotNull(savedStateHandle[Routes.ARG_PATH]) {
        "Arama rotası ${Routes.ARG_PATH} argümanı olmadan açılamaz"
    }

    private val query = MutableStateFlow("")

    private val _uiState = MutableStateFlow(SearchUiState(root = root))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                // flatMapLatest: yeni sorgu gelince önceki tarama iptal edilir.
                .flatMapLatest { text ->
                    _uiState.value = _uiState.value.copy(results = emptyList())
                    repository.search(root, text)
                        .onStart { _uiState.value = _uiState.value.copy(isSearching = text.isNotBlank()) }
                        .onCompletion { _uiState.value = _uiState.value.copy(isSearching = false) }
                }
                .collect { node ->
                    // Sonuçlar bulundukça eklenir; tarama bitmeden liste dolmaya başlar.
                    _uiState.value = _uiState.value.copy(results = _uiState.value.results + node)
                }
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> {
                query.value = action.query
                _uiState.value = _uiState.value.copy(query = action.query)
            }

            SearchAction.ClearQuery -> {
                query.value = ""
                _uiState.value = SearchUiState(root = root)
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
    }
}
