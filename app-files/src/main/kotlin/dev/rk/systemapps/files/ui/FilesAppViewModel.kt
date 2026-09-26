package dev.rk.systemapps.files.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.StoragePermissionChecker
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AppUiState {
    /** Tercihler DataStore'dan okunana kadar hangi ekranla başlanacağı bilinmiyor. */
    data object Loading : AppUiState

    data class Ready(val startWithOnboarding: Boolean) : AppUiState
}

@HiltViewModel
class FilesAppViewModel @Inject constructor(
    permissionChecker: StoragePermissionChecker,
    preferences: FilesPreferences,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = preferences.limitedModeAccepted
        .map { limitedAccepted ->
            val level = permissionChecker.refresh()
            val satisfied = level == StorageAccessLevel.FULL ||
                (level == StorageAccessLevel.LIMITED && limitedAccepted)
            AppUiState.Ready(startWithOnboarding = !satisfied)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState.Loading,
        )
}
