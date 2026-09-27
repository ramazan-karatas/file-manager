package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.files.domain.MediaMetadataReader
import dev.rk.systemapps.files.domain.MimeTypeResolver
import dev.rk.systemapps.files.domain.model.MediaInfo
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** F-1.8 ile eklenen tekil dosya işlemleri. */
class FileOperationsDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dataSource = LocalFileDataSource(
        mimeTypeResolver = MimeTypeResolver { extension ->
            when (extension) {
                "jpg" -> "image/jpeg"
                "txt" -> "text/plain"
                else -> null
            }
        },
        mediaMetadataReader = MediaMetadataReader { _, mimeType ->
            if (mimeType == "image/jpeg") MediaInfo(width = 1920, height = 1080) else null
        },
    )

    // --- yeniden adlandırma -------------------------------------------------

    @Test
    fun `yeniden adlandirma dosyayi tasir`() {
        val file = tempFolder.newFile("eski.txt").apply { writeText("içerik") }

        val node = dataSource.rename(file.path, "yeni.txt")

        assertEquals("yeni.txt", node.name)
        assertFalse(file.exists())
        assertEquals("içerik", File(tempFolder.root, "yeni.txt").readText())
    }

    @Test
    fun `turkce karakterli ada yeniden adlandirilabilir`() {
        val file = tempFolder.newFile("rapor.txt")

        val node = dataSource.rename(file.path, "şubat_çalışması.txt")

        assertEquals("şubat_çalışması.txt", node.name)
        assertTrue(File(tempFolder.root, "şubat_çalışması.txt").exists())
    }

    @Test
    fun `ayni adli oge varsa yeniden adlandirma reddedilir`() {
        val file = tempFolder.newFile("a.txt")
        tempFolder.newFile("b.txt")

        assertThrows(IOException::class.java) { dataSource.rename(file.path, "b.txt") }
        assertTrue(file.exists())
    }

    @Test
    fun `yol ayiricisi iceren ad reddedilir`() {
        val file = tempFolder.newFile("a.txt")

        assertThrows(IllegalArgumentException::class.java) {
            dataSource.rename(file.path, "alt/klasor.txt")
        }
    }

    @Test
    fun `olmayan oge yeniden adlandirilamaz`() {
        val missing = File(tempFolder.root, "yok.txt").path

        assertThrows(FileNotFoundException::class.java) { dataSource.rename(missing, "yeni.txt") }
    }

    // --- oluşturma ----------------------------------------------------------

    @Test
    fun `yeni klasor olusturulur`() {
        val node = dataSource.createDirectory(tempFolder.root.path, "Belgeler")

        assertTrue(node.isDirectory)
        assertTrue(File(tempFolder.root, "Belgeler").isDirectory)
    }

    @Test
    fun `yeni bos dosya olusturulur`() {
        val node = dataSource.createFile(tempFolder.root.path, "notlar.txt")

        assertFalse(node.isDirectory)
        assertEquals(0L, File(tempFolder.root, "notlar.txt").length())
    }

    @Test
    fun `var olan adla olusturma reddedilir`() {
        tempFolder.newFolder("Belgeler")

        assertThrows(IOException::class.java) {
            dataSource.createDirectory(tempFolder.root.path, "Belgeler")
        }
    }

    @Test
    fun `bos ad reddedilir`() {
        assertThrows(IllegalArgumentException::class.java) {
            dataSource.createDirectory(tempFolder.root.path, "   ")
        }
    }

    // --- özellikler ---------------------------------------------------------

    @Test
    fun `details dosya bilgilerini dondurur`() {
        val file = tempFolder.newFile("foto.jpg").apply { writeBytes(ByteArray(2048)) }

        val details = dataSource.details(file.path)

        assertEquals("foto.jpg", details.node.name)
        assertEquals(2048L, details.node.size)
        assertEquals("image/jpeg", details.node.mimeType)
        assertTrue(details.canRead)
        assertEquals(MediaInfo(width = 1920, height = 1080), details.media)
    }

    @Test
    fun `details klasorde medya bilgisi okumaz`() {
        val directory = tempFolder.newFolder("Belgeler")

        val details = dataSource.details(directory.path)

        assertTrue(details.node.isDirectory)
        assertNull(details.media)
    }

    @Test
    fun `izin metni rwx bicimindedir`() {
        val file = tempFolder.newFile("a.txt")

        val details = dataSource.details(file.path)

        assertEquals(3, details.permissionString.length)
        assertEquals('r', details.permissionString[0])
    }

    @Test
    fun `details olmayan oge icin hata firlatir`() {
        assertThrows(FileNotFoundException::class.java) {
            dataSource.details(File(tempFolder.root, "yok").path)
        }
    }

    // --- klasör istatistikleri ----------------------------------------------

    @Test
    fun `directoryStats agaci ozyinelemeli sayar`() {
        val root = tempFolder.newFolder("veri")
        File(root, "a.bin").writeBytes(ByteArray(100))
        val sub = File(root, "alt").apply { mkdirs() }
        File(sub, "b.bin").writeBytes(ByteArray(50))
        File(sub, "c.bin").writeBytes(ByteArray(25))

        val stats = dataSource.directoryStats(root.path)

        assertEquals(175L, stats.totalBytes)
        assertEquals(3, stats.fileCount)
        assertEquals(1, stats.directoryCount)
        assertEquals(4, stats.itemCount)
    }

    @Test
    fun `bos klasorun istatistikleri sifirdir`() {
        val empty = tempFolder.newFolder("bos")

        val stats = dataSource.directoryStats(empty.path)

        assertEquals(0L, stats.totalBytes)
        assertEquals(0, stats.itemCount)
    }

    @Test
    fun `directoryStats dosya icin hata firlatir`() {
        val file = tempFolder.newFile("a.txt")

        assertThrows(IOException::class.java) { dataSource.directoryStats(file.path) }
    }
}
