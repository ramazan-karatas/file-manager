package dev.rk.systemapps.files.data.operation

import dev.rk.systemapps.files.domain.model.ConflictDecision
import dev.rk.systemapps.files.domain.model.ConflictResolution
import dev.rk.systemapps.files.domain.model.ConflictResolver
import dev.rk.systemapps.files.domain.model.FileOperation
import dev.rk.systemapps.files.domain.model.OperationState
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class FileOperationEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // Kısıtlama kapalı: testler her adımı görmek istiyor.
    private val engine = FileOperationEngine(progressIntervalMs = 0L, now = { counter++ })
    private var counter = 0L

    private fun always(resolution: ConflictResolution, applyToAll: Boolean = false) =
        ConflictResolver { ConflictDecision(resolution, applyToAll) }

    private fun File.child(name: String) = File(this, name)

    // --- kopyalama ---------------------------------------------------------

    @Test
    fun `dosya hedefe kopyalanir kaynak yerinde kalir`() = runTest {
        val source = tempFolder.newFile("rapor.txt").apply { writeText("merhaba") }
        val target = tempFolder.newFolder("hedef")

        val progress = engine.execute(
            FileOperation.Copy(listOf(source.path), target.path),
        ).toList()

        assertEquals(OperationState.DONE, progress.last().state)
        assertEquals("merhaba", target.child("rapor.txt").readText())
        assertTrue(source.exists())
    }

    @Test
    fun `klasor agaci ozyinelemeli kopyalanir`() = runTest {
        val source = tempFolder.newFolder("belgeler")
        source.child("a.txt").writeText("a")
        File(source, "alt").mkdirs()
        source.child("alt").child("b.txt").writeText("b")
        val target = tempFolder.newFolder("hedef")

        engine.execute(FileOperation.Copy(listOf(source.path), target.path)).toList()

        assertEquals("a", target.child("belgeler").child("a.txt").readText())
        assertEquals("b", target.child("belgeler").child("alt").child("b.txt").readText())
    }

    @Test
    fun `on hesap toplam boyut ve oge sayisini bildirir`() = runTest {
        val source = tempFolder.newFolder("veri")
        source.child("bir.bin").writeBytes(ByteArray(100))
        source.child("iki.bin").writeBytes(ByteArray(50))
        val target = tempFolder.newFolder("hedef")

        val progress = engine.execute(
            FileOperation.Copy(listOf(source.path), target.path),
        ).toList()

        val last = progress.last()
        assertEquals(150L, last.totalBytes)
        // klasörün kendisi + iki dosya
        assertEquals(3, last.totalItems)
        assertEquals(3, last.processedItems)
    }

    @Test
    fun `ilk yayin PREPARING son yayin DONE`() = runTest {
        val source = tempFolder.newFile("a.txt").apply { writeText("x") }
        val target = tempFolder.newFolder("hedef")

        val progress = engine.execute(
            FileOperation.Copy(listOf(source.path), target.path),
        ).toList()

        assertEquals(OperationState.PREPARING, progress.first().state)
        assertEquals(OperationState.DONE, progress.last().state)
    }

    // --- çakışma -----------------------------------------------------------

    @Test
    fun `cakismada uzerine yaz`() = runTest {
        val source = tempFolder.newFile("not.txt").apply { writeText("yeni") }
        val target = tempFolder.newFolder("hedef")
        target.child("not.txt").writeText("eski")

        engine.execute(
            FileOperation.Copy(listOf(source.path), target.path),
            always(ConflictResolution.OVERWRITE),
        ).toList()

        assertEquals("yeni", target.child("not.txt").readText())
    }

    @Test
    fun `cakismada atla`() = runTest {
        val source = tempFolder.newFile("not.txt").apply { writeText("yeni") }
        val target = tempFolder.newFolder("hedef")
        target.child("not.txt").writeText("eski")

        engine.execute(
            FileOperation.Copy(listOf(source.path), target.path),
            always(ConflictResolution.SKIP),
        ).toList()

        assertEquals("eski", target.child("not.txt").readText())
    }

    @Test
    fun `cakismada her ikisini tut yeni ad uretir`() = runTest {
        val source = tempFolder.newFile("not.txt").apply { writeText("yeni") }
        val target = tempFolder.newFolder("hedef")
        target.child("not.txt").writeText("eski")

        engine.execute(
            FileOperation.Copy(listOf(source.path), target.path),
            always(ConflictResolution.KEEP_BOTH),
        ).toList()

        assertEquals("eski", target.child("not.txt").readText())
        assertEquals("yeni", target.child("not (1).txt").readText())
    }

    @Test
    fun `cakismada iptal islemi CANCELLED olarak bitirir`() = runTest {
        val source = tempFolder.newFile("not.txt").apply { writeText("yeni") }
        val target = tempFolder.newFolder("hedef")
        target.child("not.txt").writeText("eski")

        val progress = engine.execute(
            FileOperation.Copy(listOf(source.path), target.path),
            always(ConflictResolution.CANCEL),
        ).toList()

        assertEquals(OperationState.CANCELLED, progress.last().state)
        assertEquals("eski", target.child("not.txt").readText())
    }

    @Test
    fun `hepsine uygula secildiginde cozucu bir kez sorar`() = runTest {
        val target = tempFolder.newFolder("hedef")
        val sources = (1..3).map { index ->
            tempFolder.newFile("d$index.txt").apply { writeText("yeni$index") }
        }
        sources.forEach { target.child(it.name).writeText("eski") }

        var askCount = 0
        val resolver = ConflictResolver {
            askCount++
            ConflictDecision(ConflictResolution.OVERWRITE, applyToAll = true)
        }

        engine.execute(
            FileOperation.Copy(sources.map { it.path }, target.path),
            resolver,
        ).toList()

        assertEquals(1, askCount)
        assertEquals("yeni3", target.child("d3.txt").readText())
    }

    @Test
    fun `ayni adli klasorler cakisma sayilmaz icerik birlesir`() = runTest {
        val source = tempFolder.newFolder("ortak")
        source.child("yeni.txt").writeText("y")
        val target = tempFolder.newFolder("hedef")
        File(target, "ortak").mkdirs()
        target.child("ortak").child("eski.txt").writeText("e")

        var askCount = 0
        engine.execute(
            FileOperation.Copy(listOf(source.path), target.path),
            ConflictResolver {
                askCount++
                ConflictDecision(ConflictResolution.SKIP)
            },
        ).toList()

        assertEquals(0, askCount)
        assertTrue(target.child("ortak").child("eski.txt").exists())
        assertTrue(target.child("ortak").child("yeni.txt").exists())
    }

    // --- taşıma ------------------------------------------------------------

    @Test
    fun `tasima kaynagi siler`() = runTest {
        val source = tempFolder.newFile("tasinacak.txt").apply { writeText("içerik") }
        val target = tempFolder.newFolder("hedef")

        engine.execute(FileOperation.Move(listOf(source.path), target.path)).toList()

        assertFalse(source.exists())
        assertEquals("içerik", target.child("tasinacak.txt").readText())
    }

    @Test
    fun `klasor tasimasinda kaynak agaci geride kalmaz`() = runTest {
        val source = tempFolder.newFolder("kaynak")
        source.child("a.txt").writeText("a")
        val target = tempFolder.newFolder("hedef")

        engine.execute(FileOperation.Move(listOf(source.path), target.path)).toList()

        assertFalse(source.exists())
        assertEquals("a", target.child("kaynak").child("a.txt").readText())
    }

    @Test
    fun `cakismada uzerine yazan tasima da kaynagi siler`() = runTest {
        // renameTo kısa yolu çakışmada devreye girmez; kopyala+sil yolu sınanıyor.
        val source = tempFolder.newFile("ayni.txt").apply { writeText("yeni") }
        val target = tempFolder.newFolder("hedef")
        target.child("ayni.txt").writeText("eski")

        engine.execute(
            FileOperation.Move(listOf(source.path), target.path),
            always(ConflictResolution.OVERWRITE),
        ).toList()

        assertFalse(source.exists())
        assertEquals("yeni", target.child("ayni.txt").readText())
    }

    // --- silme -------------------------------------------------------------

    @Test
    fun `silme klasor agacini tumuyle kaldirir`() = runTest {
        val root = tempFolder.newFolder("silinecek")
        root.child("a.txt").writeText("a")
        File(root, "alt").mkdirs()
        root.child("alt").child("b.txt").writeText("b")

        val progress = engine.execute(FileOperation.Delete(listOf(root.path))).toList()

        assertFalse(root.exists())
        assertEquals(OperationState.DONE, progress.last().state)
        assertEquals(4, progress.last().processedItems)
    }

    // --- hata toleransı ----------------------------------------------------

    @Test
    fun `bir oge hata verince digerleri devam eder`() = runTest {
        val target = tempFolder.newFolder("hedef")
        val good = tempFolder.newFile("iyi.txt").apply { writeText("iyi") }
        val missing = File(tempFolder.root, "olmayan.txt")

        val progress = engine.execute(
            FileOperation.Copy(listOf(missing.path, good.path), target.path),
        ).toList()

        val last = progress.last()
        assertEquals(OperationState.DONE, last.state)
        assertEquals(1, last.failures.size)
        assertEquals(missing.path, last.failures.single().path)
        assertEquals("iyi", target.child("iyi.txt").readText())
    }

    @Test
    fun `hedef klasor acilamazsa islem FAILED biter`() = runTest {
        val source = tempFolder.newFile("a.txt").apply { writeText("a") }
        // Var olan bir dosyayı hedef klasör olarak vermek mkdirs'i başarısız kılar.
        val blocker = tempFolder.newFile("engel")

        val progress = engine.execute(
            FileOperation.Copy(listOf(source.path), blocker.path),
        ).toList()

        assertEquals(OperationState.FAILED, progress.last().state)
        assertEquals(1, progress.last().failures.size)
    }

    // --- yardımcı ----------------------------------------------------------

    @Test
    fun `nextAvailableName sirayla artar`() = runTest {
        val directory = tempFolder.newFolder("d")
        directory.child("x.txt").writeText("1")
        assertEquals("x (1).txt", nextAvailableName(directory, "x.txt").name)

        directory.child("x (1).txt").writeText("2")
        assertEquals("x (2).txt", nextAvailableName(directory, "x.txt").name)
    }

    @Test
    fun `uzantisiz adlarda da yeni ad uretilir`() = runTest {
        val directory = tempFolder.newFolder("d")
        File(directory, "klasor").mkdirs()
        assertEquals("klasor (1)", nextAvailableName(directory, "klasor").name)
    }

    // --- iptal -------------------------------------------------------------

    @Test
    fun `iptalde yarim kalan hedef dosya silinir`() = runTest {
        val source = tempFolder.newFile("buyuk.bin").apply { writeBytes(ByteArray(4 * 1024 * 1024)) }
        val target = tempFolder.newFolder("hedef")
        val slowEngine = FileOperationEngine(
            bufferSize = 1024,
            progressIntervalMs = 0L,
            now = { counter++ },
        )

        val job = launch {
            slowEngine.execute(FileOperation.Copy(listOf(source.path), target.path))
                .collect { progress ->
                    // Kopyalama başladıktan sonra iptal et.
                    if (progress.state == OperationState.RUNNING && progress.processedBytes > 0) {
                        this@launch.cancel()
                    }
                }
        }
        job.join()

        assertTrue(job.isCancelled)
        assertFalse("Yarım dosya geride kaldı", File(target, "buyuk.bin").exists())
        assertTrue("Kaynak dosyaya dokunulmamalı", source.exists())
    }

    @Test
    fun `iptalden once kopyalanan dosyalar yerinde kalir`() = runTest {
        val target = tempFolder.newFolder("hedef")
        val small = tempFolder.newFile("kucuk.txt").apply { writeText("tamam") }
        val big = tempFolder.newFile("buyuk.bin").apply { writeBytes(ByteArray(4 * 1024 * 1024)) }
        val slowEngine = FileOperationEngine(
            bufferSize = 1024,
            progressIntervalMs = 0L,
            now = { counter++ },
        )

        val job = launch {
            slowEngine.execute(FileOperation.Copy(listOf(small.path, big.path), target.path))
                .collect { progress ->
                    if (progress.currentFile == big.path && progress.processedBytes > 0) {
                        this@launch.cancel()
                    }
                }
        }
        job.join()

        assertEquals("tamam", File(target, "kucuk.txt").readText())
        assertFalse(File(target, "buyuk.bin").exists())
    }
}
