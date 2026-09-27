package dev.rk.systemapps.files.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.rk.systemapps.files.ui.about.AboutRoute
import dev.rk.systemapps.files.ui.browser.BrowserRoute
import dev.rk.systemapps.files.ui.category.CategoryRoute
import dev.rk.systemapps.files.ui.home.HomeRoute
import dev.rk.systemapps.files.ui.permission.PermissionRoute
import dev.rk.systemapps.files.ui.search.SearchRoute

@Composable
fun FilesApp(viewModel: FilesAppViewModel = hiltViewModel()) {
    val appState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = appState) {
        // Tercihler okunurken spinner göstermek yerine tema zemini: açılışta
        // yarım saniyelik bir flaş yerine düz bir geçiş oluyor.
        AppUiState.Loading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
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
            HomeRoute(
                onOpenFolder = { path -> navController.navigate(Routes.browser(path)) },
                onOpenCategory = { category ->
                    navController.navigate(Routes.category(category.name))
                },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }

        composable(
            route = Routes.BROWSER,
            arguments = listOf(navArgument(Routes.ARG_PATH) { type = NavType.StringType }),
        ) {
            BrowserRoute(
                onNavigateToFolder = { path -> navController.navigate(Routes.browser(path)) },
                onNavigateUp = { navController.navigateUp() },
                onNavigateToCrumb = navController::rebuildBrowserStack,
                onSearch = { path -> navController.navigate(Routes.search(path)) },
            )
        }

        composable(Routes.ABOUT) {
            AboutRoute(onNavigateUp = { navController.navigateUp() })
        }

        composable(
            route = Routes.CATEGORY,
            arguments = listOf(navArgument(Routes.ARG_CATEGORY) { type = NavType.StringType }),
        ) {
            CategoryRoute(onNavigateUp = { navController.navigateUp() })
        }

        composable(
            route = Routes.SEARCH,
            arguments = listOf(navArgument(Routes.ARG_PATH) { type = NavType.StringType }),
        ) {
            SearchRoute(
                onOpenFolder = { path -> navController.navigate(Routes.browser(path)) },
                onNavigateUp = { navController.navigateUp() },
            )
        }
    }
}

/**
 * Breadcrumb'tan bir üst klasöre atlarken geri yığını kökten hedefe kadar yeniden kurulur;
 * böylece geri tuşu yine bir üst klasöre gider (docs/files/SPEC.md §3.2).
 */
private fun NavHostController.rebuildBrowserStack(paths: List<String>) {
    popBackStack(Routes.HOME, inclusive = false)
    paths.forEach { path -> navigate(Routes.browser(path)) }
}
