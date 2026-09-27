package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.files.domain.MimeTypeResolver
import java.io.File
import java.nio.file.Files
import java.util.Locale
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileSearchTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val tr = Locale.forLanguageTag("tr-TR")
    private val dataSource = LocalFileDataSource(MimeTypeResolver { null })

    private fun search(query: String) =
        dataSource.search(tempFolder.root.path, query, tr)

    @Test
    fun `arama alt klasorleri de tarar`() = runTest {
        tempFolder.newFile("rapor.txt")
        val sub = tempFolder.newFolder("belgeler", "2026")
        File(sub, "rapor_subat.txt").writeText("x")

        val names = search("rapor").toList().map { it.name }

        assertEquals(setOf("rapor.txt", "rapor_subat.txt"), names.toSet())
    }

    @Test
    fun `arama buyuk kucuk harf ayirmaz`() = runTest {
        tempFolder.newFile("RAPOR.txt")

        assertEquals(1, search("rapor").toList().size)
    }

    @Test
    fun `klasorler de sonuclara girer`() = runTest {
        tempFolder.newFolder("Raporlar")

        val results = search("rapor").toList()

        assertEquals(1, results.size)
        assertTrue(results.single().isDirectory)
    }

    @Test
    fun `eslesmeyen sorgu bos doner`() = runTest {
        tempFolder.newFile("notlar.txt")

        assertTrue(search("rapor").toList().isEmpty())
    }

    @Test
    fun `bos sorgu hic tarama yapmaz`() = runTest {
        tempFolder.newFile("rapor.txt")

        assertTrue(search("   ").toList().isEmpty())
    }

    @Test
    fun `symlink dongusu aramayi sonsuza sokmaz`() = runTest {
        // a/ -> b/ -> a/ döngüsü. Canonical yol takibi olmasaydı tarama hiç bitmezdi.
        val a = tempFolder.newFolder("a")
        val b = File(a, "b").apply { mkdirs() }
        File(a, "hedef_rapor.txt").writeText("x")

        val linkCreated = runCatching {
            Files.createSymbolicLink(File(b, "geri").toPath(), a.toPath())
        }.isSuccess
        // Windows'ta sembolik bağlantı yönetici hakkı ister; yoksa test atlanır.
        assumeTrue(linkCreated)

        val results = withTimeout(10_000) { search("rapor").toList() }

        assertEquals(1, results.size)
    }
}
