package dev.rk.systemapps.files.ui.component

import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.files.data.operation.FileOperationEngine
import dev.rk.systemapps.files.data.operation.FileOperationManager
import dev.rk.systemapps.files.data.operation.OperationServiceController
import dev.rk.systemapps.files.domain.model.ClipboardMode
import dev.rk.systemapps.files.util.FakeFileRepository
import dev.rk.systemapps.files.util.FakeFilesPreferences
import dev.rk.systemapps.files.util.MainDispatcherRule
import dev.rk.systemapps.files.util.TestDispatcherProvider
import dev.rk.systemapps.files.util.fileNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FileSelectionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val directory = "/storage/emulated/0/Belgeler"

    private val nodes = listOf(
        fileNode("a.txt", parent = directory),
        fileNode("b.txt", parent = directory),
        fileNode("c.txt", parent = directory),
    )

    private val repository = FakeFileRepository(nodes)
    private val preferences = FakeFilesPreferences()

    private val operations = FileOperationManager(
        serviceController = object : OperationServiceController {
            override fun start() = Unit
            override fun stop() = Unit
        },
        engine = FileOperationEngine(progressIntervalMs = 0L),
        mediaScanner = { },
        dispatchers = TestDispatcherProvider(),
    )

    private fun selection(scope: CoroutineScope) = FileSelection(
        scope = scope,
        items = { nodes },
        repository = repository,
        preferences = preferences,
        operations = operations,
    )

    @Test
    fun `uzun basma secer ve tekrar basma secimi kaldirir`() = runTest {
        val selection = selection(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        selection.onAction(SelectionAction.Toggle(nodes[0].id))
        assertEquals(setOf(nodes[0].id), selection.state.value.selectedIds)
        assertTrue(selection.state.value.active)

        selection.onAction(SelectionAction.Toggle(nodes[0].id))
        assertTrue(selection.state.value.selectedIds.isEmpty())
        assertFalse(selection.state.value.active)
    }

    @Test
    fun `tumunu sec ve tersine cevir`() = runTest {
        val selection = selection(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        selection.onAction(SelectionAction.SelectAll)
        assertEquals(nodes.map { it.id }.toSet(), selection.state.value.selectedIds)

        selection.onAction(SelectionAction.Invert)
        assertTrue(selection.state.value.selectedIds.isEmpty())

        selection.onAction(SelectionAction.Toggle(nodes[1].id))
        selection.onAction(SelectionAction.Invert)
        assertEquals(setOf(nodes[0].id, nodes[2].id), selection.state.value.selectedIds)
    }

    @Test
    fun `kopyalama panoya yazar ve secimi kapatir`() = runTest {
        val selection = selection(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        selection.onAction(SelectionAction.Toggle(nodes[0].id))
        selection.onAction(SelectionAction.Toggle(nodes[2].id))
        selection.onAction(SelectionAction.Copy)

        val clipboard = preferences.currentClipboard
        assertEquals(ClipboardMode.COPY, clipboard?.mode)
        assertEquals(setOf(nodes[0].id, nodes[2].id), clipboard?.paths?.toSet())
        // Pano dolduktan sonra seçim kapanır; yapıştırma gezginde yapılır.
        assertTrue(selection.state.value.selectedIds.isEmpty())
    }

    @Test
    fun `kesme panoya tasima kipiyle yazar`() = runTest {
        val selection = selection(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        selection.onAction(SelectionAction.Toggle(nodes[1].id))
        selection.onAction(SelectionAction.Cut)

        assertEquals(ClipboardMode.MOVE, preferences.currentClipboard?.mode)
    }

    @Test
    fun `silme islem kuyruguna girer ve secim kapanir`() = runTest {
        val selection = selection(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        selection.onAction(SelectionAction.Toggle(nodes[0].id))
        selection.onAction(SelectionAction.Delete)

        assertTrue(selection.state.value.selectedIds.isEmpty())
    }

    @Test
    fun `yeniden adlandirma tek secimde calisir ve listeyi tazeler`() = runTest {
        val selection = selection(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        var reloaded = false
        selection.onRenamed = { reloaded = true }
        repository.nameResult = Outcome.Success(fileNode("yeni.txt", parent = directory))

        selection.onAction(SelectionAction.Toggle(nodes[0].id))
        selection.onAction(SelectionAction.ShowRename)
        assertTrue(selection.state.value.dialog is SelectionDialog.Rename)

        selection.onAction(SelectionAction.ConfirmName("yeni.txt"))

        assertEquals(listOf(Triple("rename", nodes[0].id, "yeni.txt")), repository.nameCalls)
        assertTrue(reloaded)
        assertTrue(selection.state.value.selectedIds.isEmpty())
    }

    @Test
    fun `birden fazla secimde yeniden adlandirma acilmaz`() = runTest {
        val selection = selection(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        selection.onAction(SelectionAction.Toggle(nodes[0].id))
        selection.onAction(SelectionAction.Toggle(nodes[1].id))
        selection.onAction(SelectionAction.ShowRename)

        assertEquals(null, selection.state.value.dialog)
    }
}
