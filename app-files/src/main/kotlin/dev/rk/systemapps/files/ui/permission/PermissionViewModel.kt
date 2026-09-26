package dev.rk.systemapps.files.ui.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rk.systemapps.core.storage.permission.StorageAccessLevel
import dev.rk.systemapps.core.storage.permission.StoragePermissionChecker
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionUiState(
    val accessLevel: StorageAccessLevel = StorageAccessLevel.NONE,
    val requiresManageExternalStorage: Boolean = true,
    val runtimePermissions: List<String> = emptyList(),
    /** Ayarlar ekranı hiçbir intent ile açılamadı — kullanıcıya elle yol tarif edilir. */
    val settingsUnavailable: Boolean = false,
    /** Onboarding tamamlandı; çağıran ekranı değiştirmeli. */
    val completed: Boolean = false,
)

sealed interface PermissionAction {
    data object Refresh : PermissionAction
    data object SettingsUnavailable : PermissionAction
    data object ContinueWithLimitedAccess : PermissionAction
    data object RuntimePermissionResult : PermissionAction
}

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val permissionChecker: StoragePermissionChecker,
    private val preferences: FilesPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PermissionUiState(
            accessLevel = permissionChecker.accessLevel.value,
            requiresManageExternalStorage = permissionChecker.requiresManageExternalStorage(),
            runtimePermissions = permissionChecker.runtimePermissions(),
        ),
    )
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    fun onAction(action: PermissionAction) {
        when (action) {
            PermissionAction.Refresh,
            PermissionAction.RuntimePermissionResult,
            -> refresh()

            PermissionAction.SettingsUnavailable ->
                _uiState.update { it.copy(settingsUnavailable = true) }

            PermissionAction.ContinueWithLimitedAccess -> acceptLimitedMode()
        }
    }

    private fun refresh() {
        val level = permissionChecker.refresh()
        _uiState.update { state ->
            state.copy(
                accessLevel = level,
                // Tam erişim alındıysa onboarding'de kalmanın anlamı yok.
                completed = state.completed || level == StorageAccessLevel.FULL,
            )
        }
    }

    private fun acceptLimitedMode() {
        viewModelScope.launch {
            preferences.setLimitedModeAccepted(true)
            _uiState.update { it.copy(completed = true) }
        }
    }
}
