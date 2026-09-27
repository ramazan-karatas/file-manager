package dev.rk.systemapps.files.ui.browser

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.SortBy
import dev.rk.systemapps.files.ui.Routes
import dev.rk.systemapps.files.data.operation.FileOperationEngine
import dev.rk.systemapps.files.data.operation.FileOperationManager
import dev.rk.systemapps.files.data.operation.OperationServiceController
import dev.rk.systemapps.files.util.FakeFileRepository
import dev.rk.systemapps.files.util.FakeFilesPreferences
import dev.rk.systemapps.files.util.MainDispatcherRule
import dev.rk.systemapps.files.util.TestDispatcherProvider
import dev.rk.systemapps.files.util.fileNode
import dev.rk.systemapps.files.util.names
import java.io.FileNotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BrowserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val directory = "/storage/emulated/0/Belgeler"

    private val nodes = listOf(
        fileNode("a.txt", parent = directory),
        fileNode("b.txt", parent = directory),
        fileNode("c.txt", parent = directory),
    )

    private fun operationManager() = FileOperationManager(
        serviceController = object : OperationServiceController {
            override fun start() = Unit
            override fun stop() = Unit
        },
        engine = FileOperationEngine(progressIntervalMs = 0L),
        mediaScanner = { },
        dispatchers = TestDispatcherProvider(),
    )

    private fun viewModel(
        repository: FakeFileRepository = FakeFileRepository(nodes),
        preferences: FakeFilesPreferences = FakeFilesPreferences(),
        operations: FileOperationManager = operationManager(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf(Routes.ARG_PATH to directory)),
    ) = BrowserViewModel(
        repository = repository,
        preferences = preferences,
        operations = operations,
        storageLocations = { "/storage/emulated/0" },
        savedStateHandle = savedStateHandle,
    )

    @Test
    fun `acilista klasor icerigi yuklenir`() = runTest {
        viewModel().uiState.test {
            val state = awaitItem()
            assertEquals(listOf("a.txt", "b.txt", "c.txt"), state.items.names())
            assertFalse(state.isLoading)
            assertFalse(state.selectionActive)
        }
    }

    @Test
    fun `hata durumu BrowserError olarak yansir`() = runTest {
        val repository = FakeFileRepository().apply {
            result = Outcome.Failure(FileNotFoundException("yok"))
        }

        viewModel(repository = repository).uiState.test {
            assertEquals(BrowserError.NOT_FOUND, awaitItem().error)
        }
    }

    @Test
    fun `uzun basma secer tekrar basinca secimden cikarir`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()

            vm.onAction(BrowserAction.ToggleSelection(nodes[0].id))
            assertEquals(setOf(nodes[0].id), awaitItem().selectedIds)

            vm.onAction(BrowserAction.ToggleSelection(nodes[0].id))
            assertFalse(awaitItem().selectionActive)
        }
    }

    @Test
    fun `tumunu sec ve secimi temizle`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()

            vm.onAction(BrowserAction.SelectAll)
            val selected = awaitItem()
            assertEquals(3, selected.selectedIds.size)
            assertTrue(selected.allSelected)

            vm.onAction(BrowserAction.ClearSelection)
            assertFalse(awaitItem().selectionActive)
        }
    }

    @Test
    fun `secimi tersine cevir`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()

            vm.onAction(BrowserAction.ToggleSelection(nodes[0].id))
            awaitItem()

            vm.onAction(BrowserAction.InvertSelection)
            assertEquals(setOf(nodes[1].id, nodes[2].id), awaitItem().selectedIds)
        }
    }

    @Test
    fun `listeden kaybolan oge secili kalmaz`() = runTest {
        val repository = FakeFileRepository(nodes)
        val vm = viewModel(repository = repository)

        vm.uiState.test {
            awaitItem()
            vm.onAction(BrowserAction.SelectAll)
            assertEquals(3, awaitItem().selectedIds.size)

            // Klasör dışarıdan değişti: iki dosya silindi.
            repository.result = Outcome.Success(listOf(nodes[0]))
            vm.onAction(BrowserAction.Reload)

            val state = awaitItem()
            assertEquals(setOf(nodes[0].id), state.selectedIds)
        }
    }

    @Test
    fun `secim SavedStateHandle uzerinde korunur`() = runTest {
        val handle = SavedStateHandle(mapOf(Routes.ARG_PATH to directory))
        val first = viewModel(savedStateHandle = handle)

        first.uiState.test {
            awaitItem()
            first.onAction(BrowserAction.ToggleSelection(nodes[1].id))
            assertEquals(setOf(nodes[1].id), awaitItem().selectedIds)
        }

        // Proses ölümü / ekran döndürme: aynı handle ile yeni ViewModel.
        val restored = viewModel(savedStateHandle = handle)

        restored.uiState.test {
            assertEquals(setOf(nodes[1].id), awaitItem().selectedIds)
        }
    }

    @Test
    fun `siralama degisimi tercihlere yazilir ve yeniden listeler`() = runTest {
        val repository = FakeFileRepository(nodes)
        val preferences = FakeFilesPreferences()
        val vm = viewModel(repository = repository, preferences = preferences)

        vm.uiState.test {
            awaitItem()

            vm.onAction(BrowserAction.SetSortBy(SortBy.SIZE))
            assertEquals(SortBy.SIZE, awaitItem().prefs.sortBy)
        }

        assertEquals(SortBy.SIZE, preferences.currentPrefs.sortBy)
        assertEquals(SortBy.SIZE, repository.requestedOptions.last().sortBy)
    }

    @Test
    fun `gizli dosya ve klasor tercihleri repository secenegine gecer`() = runTest {
        val repository = FakeFileRepository(nodes)
        val vm = viewModel(repository = repository)

        vm.uiState.test {
            awaitItem()

            vm.onAction(BrowserAction.ToggleShowHidden)
            assertTrue(awaitItem().prefs.showHidden)

            vm.onAction(BrowserAction.ToggleFoldersFirst)
            assertFalse(awaitItem().prefs.foldersFirst)
        }

        val last = repository.requestedOptions.last()
        assertTrue(last.showHidden)
        assertFalse(last.foldersFirst)
    }

    @Test
    fun `gorunum kipi kalici tercihe yazilir`() = runTest {
        val preferences = FakeFilesPreferences(BrowserPrefs(gridMode = false))
        val vm = viewModel(preferences = preferences)

        vm.uiState.test {
            awaitItem()
            vm.onAction(BrowserAction.ToggleViewMode)
            assertTrue(awaitItem().prefs.gridMode)
        }

        assertTrue(preferences.currentPrefs.gridMode)
    }
}
