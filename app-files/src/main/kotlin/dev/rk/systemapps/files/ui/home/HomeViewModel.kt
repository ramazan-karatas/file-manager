package dev.rk.systemapps.files.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.StoragePermissionChecker
import dev.rk.systemapps.core.storage.volume.StorageLocations
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val accessLevel: StorageAccessLevel = StorageAccessLevel.NONE,
    val storageRoot: String = "",
) {
    val showLimitedAccessBanner: Boolean get() = !accessLevel.canBrowseFileSystem
}

sealed interface HomeAction {
    data object Refresh : HomeAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionChecker: StoragePermissionChecker,
    storageLocations: StorageLocations,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            accessLevel = permissionChecker.accessLevel.value,
            storageRoot = storageLocations.primaryExternalStorage(),
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Refresh -> {
                val level = permissionChecker.refresh()
                _uiState.update { it.copy(accessLevel = level) }
            }
        }
    }
}
