package dev.rk.systemapps.files.ui.category

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.rk.systemapps.files.domain.model.FileCategory
import dev.rk.systemapps.files.domain.model.SortBy
import dev.rk.systemapps.files.ui.Routes
import dev.rk.systemapps.files.util.FakeFilesPreferences
import dev.rk.systemapps.files.util.FakeMediaCatalog
import dev.rk.systemapps.files.util.MainDispatcherRule
import dev.rk.systemapps.files.util.TestDispatcherProvider
import dev.rk.systemapps.files.util.fileNode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Adlar bilerek ASCII: sıralama beklentisi makinenin yerel ayarına bağlı olmasın.
    private val catalog = FakeMediaCatalog(
        results = listOf(
            fileNode("b.xlsx", size = 300, lastModified = 30),
            fileNode("a.pdf", size = 100, lastModified = 10),
            fileNode("c.docx", size = 200, lastModified = 20),
        ),
    )

    private val preferences = FakeFilesPreferences()

    private fun viewModel() = CategoryViewModel(
        mediaCatalog = catalog,
        preferences = preferences,
        dispatchers = TestDispatcherProvider(),
        savedStateHandle = SavedStateHandle(
            mapOf(Routes.ARG_CATEGORY to FileCategory.DOCUMENTS.name),
        ),
    )

    @Test
    fun `varsayilan olarak ada gore artan siralanir`() = runTest {
        viewModel().uiState.test {
            val state = awaitReady()
            assertEquals(listOf("a.pdf", "b.xlsx", "c.docx"), state.items.map { it.name })
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `olcut degisince liste yeniden sorgulanmadan siralanir`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            awaitReady()
            vm.onAction(CategoryAction.SetSortBy(SortBy.SIZE))

            val sorted = awaitItemWhere { it.prefs.sortBy == SortBy.SIZE }
            assertEquals(listOf("a.pdf", "c.docx", "b.xlsx"), sorted.items.map { it.name })
        }

        // Tercih değişikliği yalnızca yeniden sıralama; MediaStore bir kez sorgulandı.
        assertEquals(1, catalog.queryCount)
        assertEquals(SortBy.SIZE, preferences.currentPrefs.sortBy)
    }

    @Test
    fun `yon degistirince sira tersine doner`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            awaitReady()
            vm.onAction(CategoryAction.ToggleSortDirection)

            val sorted = awaitItemWhere { !it.prefs.ascending }
            assertEquals(listOf("c.docx", "b.xlsx", "a.pdf"), sorted.items.map { it.name })
        }
    }

    @Test
    fun `gorunum degisimi tercihe yazilir`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            awaitReady()
            vm.onAction(CategoryAction.ToggleViewMode)
            assertTrue(awaitItemWhere { it.prefs.gridMode }.prefs.gridMode)
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<CategoryUiState>.awaitReady():
        CategoryUiState = awaitItemWhere { !it.isLoading }

    private suspend fun app.cash.turbine.ReceiveTurbine<CategoryUiState>.awaitItemWhere(
        predicate: (CategoryUiState) -> Boolean,
    ): CategoryUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }
}
