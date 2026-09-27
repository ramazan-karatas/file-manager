package dev.rk.systemapps.files.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.files.data.media.MediaStoreDataSource
import dev.rk.systemapps.files.domain.model.FileCategory
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.ui.Routes
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CategoryUiState(
    val category: FileCategory,
    val items: List<FileNode> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val mediaStore: MediaStoreDataSource,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val category: FileCategory = FileCategory.valueOf(
        checkNotNull(savedStateHandle[Routes.ARG_CATEGORY]) {
            "Kategori rotası ${Routes.ARG_CATEGORY} argümanı olmadan açılamaz"
        },
    )

    private val _uiState = MutableStateFlow(CategoryUiState(category = category))
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = withContext(dispatchers.io) { mediaStore.query(category) }
            _uiState.value = CategoryUiState(category, items, isLoading = false)
        }
    }
}
