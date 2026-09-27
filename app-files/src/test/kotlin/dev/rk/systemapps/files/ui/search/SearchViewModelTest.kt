package dev.rk.systemapps.files.ui.search

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.rk.systemapps.files.ui.Routes
import dev.rk.systemapps.files.util.FakeFileRepository
import dev.rk.systemapps.files.util.MainDispatcherRule
import dev.rk.systemapps.files.util.fileNode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val root = "/storage/emulated/0"

    private val repository = FakeFileRepository().apply {
        searchSource = listOf(
            fileNode("rapor.txt", parent = root),
            fileNode("rapor_subat.txt", parent = root),
            fileNode("notlar.txt", parent = root),
        )
    }

    private fun viewModel() = SearchViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_PATH to root)),
    )

    @Test
    fun `sorgu sonuclari akisla dolar`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            assertTrue(awaitItem().results.isEmpty())

            vm.onAction(SearchAction.QueryChanged("rapor"))

            // Sonuçlar tek tek eklendiği için son durum ikisini de içerir.
            val names = awaitLastResults()
            assertEquals(listOf("rapor.txt", "rapor_subat.txt"), names)
        }
    }

    @Test
    fun `hizli yazmada yalnizca son sorgu calisir`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            // debounce: aradaki harfler tarama başlatmaz.
            vm.onAction(SearchAction.QueryChanged("r"))
            vm.onAction(SearchAction.QueryChanged("ra"))
            vm.onAction(SearchAction.QueryChanged("rapor"))

            awaitLastResults()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("rapor"), repository.searchQueries)
    }

    @Test
    fun `temizleme sonuclari sifirlar`() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            awaitItem()
            vm.onAction(SearchAction.QueryChanged("rapor"))
            awaitLastResults()

            vm.onAction(SearchAction.ClearQuery)
            val state = expectMostRecentItem()
            assertTrue(state.results.isEmpty())
            assertEquals("", state.query)
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<SearchUiState>.awaitLastResults():
        List<String> {
        var names = emptyList<String>()
        while (true) {
            val state = awaitItem()
            if (state.results.isNotEmpty()) names = state.results.map { it.name }
            if (!state.isSearching && names.isNotEmpty()) return names
        }
    }
}
