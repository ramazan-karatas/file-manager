package dev.rk.systemapps.files.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.StoragePermissionChecker
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val accessLevel: StorageAccessLevel = StorageAccessLevel.NONE,
) {
    val showLimitedAccessBanner: Boolean get() = !accessLevel.canBrowseFileSystem
}

sealed interface HomeAction {
    data object Refresh : HomeAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionChecker: StoragePermissionChecker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(accessLevel = permissionChecker.accessLevel.value),
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
