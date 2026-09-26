package dev.rk.systemapps.files.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.rk.systemapps.core.design.component.LoadingState
import dev.rk.systemapps.files.ui.home.HomeRoute
import dev.rk.systemapps.files.ui.permission.PermissionRoute

@Composable
fun FilesApp(viewModel: FilesAppViewModel = hiltViewModel()) {
    val appState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = appState) {
        AppUiState.Loading -> LoadingState()
        is AppUiState.Ready -> FilesNavHost(startWithOnboarding = state.startWithOnboarding)
    }
}

@Composable
private fun FilesNavHost(startWithOnboarding: Boolean) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (startWithOnboarding) Routes.PERMISSION else Routes.HOME,
    ) {
        composable(Routes.PERMISSION) {
            PermissionRoute(
                onCompleted = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeRoute()
        }
    }
}
