package dev.rk.systemapps.files.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.core.storage.model.StorageVolumeInfo
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.StoragePermissionChecker
import dev.rk.systemapps.core.storage.volume.StorageLocations
import dev.rk.systemapps.files.data.media.MediaStoreDataSource
import dev.rk.systemapps.files.domain.model.FileNode
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
) {
    val showLimitedAccessBanner: Boolean get() = !accessLevel.canBrowseFileSystem
}

sealed interface HomeAction {
    data object Refresh : HomeAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionChecker: StoragePermissionChecker,
    private val storageLocations: StorageLocations,
    private val mediaStore: MediaStoreDataSource,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            accessLevel = permissionChecker.accessLevel.value,
            storageRoot = storageLocations.primaryExternalStorage(),
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Refresh -> refresh()
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
