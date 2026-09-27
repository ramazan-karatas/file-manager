package dev.rk.systemapps.files.ui.category

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rk.systemapps.core.common.format.formatBytes
import dev.rk.systemapps.core.common.format.formatDate
import dev.rk.systemapps.core.design.component.AppListItem
import dev.rk.systemapps.core.design.component.EmptyState
import dev.rk.systemapps.core.design.component.LoadingState
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.ui.browser.FileActions
import dev.rk.systemapps.files.ui.browser.FileThumbnail
import dev.rk.systemapps.files.ui.home.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryRoute(
    onNavigateUp: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(uiState.category.labelRes())) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when {
            uiState.isLoading -> LoadingState(modifier = contentModifier)

            uiState.isEmpty -> EmptyState(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(R.string.category_empty),
                modifier = contentModifier,
            )

            else -> LazyColumn(modifier = contentModifier.fillMaxSize()) {
                items(items = uiState.items, key = { it.id }) { node ->
                    AppListItem(
                        title = node.name,
                        subtitle = "${formatBytes(node.size)} · ${formatDate(node.lastModified)}",
                        leading = { FileThumbnail(node = node) },
                        onClick = { FileActions.open(context, node) },
                    )
                }
            }
        }
    }
}
