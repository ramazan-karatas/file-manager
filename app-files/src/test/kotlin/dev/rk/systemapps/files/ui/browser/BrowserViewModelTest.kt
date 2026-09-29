package dev.rk.systemapps.files.ui.browser

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.SortBy
import dev.rk.systemapps.files.ui.Routes
import dev.rk.systemapps.core.storage.model.StorageVolumeInfo
import dev.rk.systemapps.core.storage.volume.StorageLocations
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
        dispatchers = TestDispatcherProvider(),
        storageLocations = object : StorageLocations {
            override fun primaryExternalStorage() = "/storage/emulated/0"
            override fun volumes() = emptyList<StorageVolumeInfo>()
        },
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
    fun `siralama degisimi diski yeniden okumaz`() = runTest {
        val repository = FakeFileRepository(
            listOf(
                fileNode("b.txt", size = 30, parent = directory),
                fileNode("a.txt", size = 10, parent = directory),
                fileNode("c.txt", size = 20, parent = directory),
            ),
        )
        val preferences = FakeFilesPreferences()
        val vm = viewModel(repository = repository, preferences = preferences)

        vm.uiState.test {
            assertEquals(listOf("a.txt", "b.txt", "c.txt"), awaitItem().items.names())

            vm.onAction(BrowserAction.SetSortBy(SortBy.SIZE))

            // Sıralama arka planda yapılıyor; tercih güncellemesi listeden önce
            // gelebildiği için beklenen sıra üzerinden bekleniyor.
            awaitItemWhere { it.items.names() == listOf("a.txt", "c.txt", "b.txt") }
        }

        assertEquals(SortBy.SIZE, preferences.currentPrefs.sortBy)
        // Asıl kazanç: tercih değişti ama klasör yalnızca açılışta okundu.
        assertEquals(1, repository.listCount)
    }

    @Test
    fun `gizli dosya ve klasor tercihleri siralamaya gecer`() = runTest {
        val repository = FakeFileRepository(
            listOf(
                fileNode("klasor", isDirectory = true, parent = directory),
                fileNode(".gizli.txt", parent = directory),
                fileNode("a.txt", parent = directory),
            ),
        )
        val vm = viewModel(repository = repository)

        vm.uiState.test {
            assertEquals(listOf("klasor", "a.txt"), awaitItem().items.names())

            vm.onAction(BrowserAction.ToggleShowHidden)
            awaitItemWhere { it.items.names() == listOf("klasor", ".gizli.txt", "a.txt") }

            vm.onAction(BrowserAction.ToggleFoldersFirst)
            awaitItemWhere { it.items.names() == listOf(".gizli.txt", "a.txt", "klasor") }
        }

        assertEquals(1, repository.listCount)
    }

    @Test
    fun `ekran one gelince klasor degismediyse okunmaz`() = runTest {
        val repository = FakeFileRepository(nodes).apply {
            statResult = Outcome.Success(fileNode("Belgeler", isDirectory = true, lastModified = 5))
        }
        val vm = viewModel(repository = repository)
        assertEquals(1, repository.listCount)

        vm.onAction(BrowserAction.RefreshIfChanged)

        assertEquals(1, repository.listCount)
    }

    @Test
    fun `ekran one gelince klasor degistiyse yeniden okunur`() = runTest {
        val repository = FakeFileRepository(nodes).apply {
            statResult = Outcome.Success(fileNode("Belgeler", isDirectory = true, lastModified = 5))
        }
        val vm = viewModel(repository = repository)

        // Klasör dışarıdan değişti: damga ilerledi.
        repository.statResult =
            Outcome.Success(fileNode("Belgeler", isDirectory = true, lastModified = 9))
        vm.onAction(BrowserAction.RefreshIfChanged)

        assertEquals(2, repository.listCount)
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

    private suspend fun app.cash.turbine.ReceiveTurbine<BrowserUiState>.awaitItemWhere(
        predicate: (BrowserUiState) -> Boolean,
    ): BrowserUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }
}
