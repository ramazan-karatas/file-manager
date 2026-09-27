package dev.rk.systemapps.files.data.operation

import app.cash.turbine.test
import dev.rk.systemapps.files.domain.model.ConflictDecision
import dev.rk.systemapps.files.domain.model.ConflictResolution
import dev.rk.systemapps.files.domain.model.FileOperation
import dev.rk.systemapps.files.domain.model.OperationState
import dev.rk.systemapps.files.util.TestDispatcherProvider
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class FileOperationManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class RecordingServiceController : OperationServiceController {
        var startCount = 0
        var stopCount = 0
        override fun start() { startCount++ }
        override fun stop() { stopCount++ }
    }

    private class RecordingScanner : MediaScanner {
        val scanned = mutableListOf<String>()
        override fun scan(paths: List<String>) { scanned += paths }
    }

    private val serviceController = RecordingServiceController()
    private val scanner = RecordingScanner()

    private fun manager() = FileOperationManager(
        serviceController = serviceController,
        engine = FileOperationEngine(progressIntervalMs = 0L),
        mediaScanner = scanner,
        dispatchers = TestDispatcherProvider(),
    )

    @Test
    fun `kuyruga alinan islem yurutulur ve servis baslatilir`() = runTest {
        val source = tempFolder.newFile("a.txt").apply { writeText("a") }
        val target = tempFolder.newFolder("hedef")
        val manager = manager()

        manager.completions.test {
            manager.enqueue(FileOperation.Copy(listOf(source.path), target.path))

            val completion = awaitItem()
            assertEquals(OperationState.DONE, completion.state)
        }

        assertEquals("a", File(target, "a.txt").readText())
        assertEquals(1, serviceController.startCount)
    }

    @Test
    fun `kuyruk bosalinca servis durdurulur`() = runTest {
        val source = tempFolder.newFile("a.txt").apply { writeText("a") }
        val target = tempFolder.newFolder("hedef")
        val manager = manager()

        manager.completions.test {
            manager.enqueue(FileOperation.Copy(listOf(source.path), target.path))
            awaitItem()
        }

        assertEquals(1, serviceController.stopCount)
    }

    @Test
    fun `birden fazla islem sirayla yurutulur`() = runTest {
        val target = tempFolder.newFolder("hedef")
        val first = tempFolder.newFile("bir.txt").apply { writeText("1") }
        val second = tempFolder.newFile("iki.txt").apply { writeText("2") }
        val manager = manager()

        manager.completions.test {
            manager.enqueue(FileOperation.Copy(listOf(first.path), target.path))
            manager.enqueue(FileOperation.Copy(listOf(second.path), target.path))

            assertEquals(OperationState.DONE, awaitItem().state)
            assertEquals(OperationState.DONE, awaitItem().state)
        }

        assertTrue(File(target, "bir.txt").exists())
        assertTrue(File(target, "iki.txt").exists())
    }

    @Test
    fun `cakisma UI'ya bildirilir ve verilen karar uygulanir`() = runTest {
        val source = tempFolder.newFile("ayni.txt").apply { writeText("yeni") }
        val target = tempFolder.newFolder("hedef")
        File(target, "ayni.txt").writeText("eski")
        val manager = manager()

        manager.pendingConflict.test {
            assertEquals(null, awaitItem())

            manager.enqueue(FileOperation.Copy(listOf(source.path), target.path))

            val conflict = awaitItem()
            assertTrue(conflict != null)
            assertEquals(File(target, "ayni.txt").path, conflict?.targetPath)

            manager.resolveConflict(ConflictDecision(ConflictResolution.OVERWRITE))
            assertEquals(null, awaitItem())
        }

        assertEquals("yeni", File(target, "ayni.txt").readText())
    }

    @Test
    fun `islem bitince MediaStore taramasi tetiklenir`() = runTest {
        val source = tempFolder.newFile("foto.jpg").apply { writeText("x") }
        val target = tempFolder.newFolder("hedef")
        val manager = manager()

        manager.completions.test {
            manager.enqueue(FileOperation.Copy(listOf(source.path), target.path))
            awaitItem()
        }

        assertTrue(source.path in scanner.scanned)
        assertTrue(target.path in scanner.scanned)
    }

    @Test
    fun `silme islemi dosyayi kaldirir ve durum yayilir`() = runTest {
        val victim = tempFolder.newFile("silinecek.txt").apply { writeText("x") }
        val manager = manager()

        manager.completions.test {
            manager.enqueue(FileOperation.Delete(listOf(victim.path)))
            assertEquals(OperationState.DONE, awaitItem().state)
        }

        assertFalse(victim.exists())
    }

    @Test
    fun `bosta kuyruk durumu mesgul degildir`() = runTest {
        val manager = manager()
        assertFalse(manager.state.value.isBusy)
        assertEquals(0, manager.state.value.queued)
    }
}
