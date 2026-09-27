package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.files.domain.MimeTypeResolver
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.LocalFileNode
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

class LocalFileDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val resolver = MimeTypeResolver { extension ->
        if (extension == "txt") "text/plain" else null
    }
    private val dataSource = LocalFileDataSource(resolver)

    @Test
    fun `klasor icerigi dosya ve klasorleri dondurur`() {
        tempFolder.newFile("not.txt").writeText("merhaba")
        tempFolder.newFolder("belgeler")

        val result = dataSource.listDirectory(tempFolder.root.absolutePath)

        assertEquals(setOf("not.txt", "belgeler"), result.map { it.name }.toSet())
    }

    @Test
    fun `dosya boyutu okunur klasor boyutu hesaplanmaz`() {
        tempFolder.newFile("veri.bin").writeBytes(ByteArray(128))
        tempFolder.newFolder("klasor")

        val result = dataSource.listDirectory(tempFolder.root.absolutePath).associateBy { it.name }

        assertEquals(128L, result.getValue("veri.bin").size)
        assertEquals(FileNode.SIZE_UNKNOWN, result.getValue("klasor").size)
    }

    @Test
    fun `gizlilik olcutu nokta ile baslamaktir`() {
        tempFolder.newFile(".gizli")
        tempFolder.newFile("acik.txt")

        val result = dataSource.listDirectory(tempFolder.root.absolutePath).associateBy { it.name }

        assertTrue(result.getValue(".gizli").isHidden)
        assertFalse(result.getValue("acik.txt").isHidden)
    }

    @Test
    fun `mime turu cozucuden gelir klasorde bos kalir`() {
        tempFolder.newFile("not.txt")
        tempFolder.newFile("bilinmeyen.zzz")
        tempFolder.newFolder("klasor")

        val result = dataSource.listDirectory(tempFolder.root.absolutePath).associateBy { it.name }

        assertEquals("text/plain", result.getValue("not.txt").mimeType)
        assertNull(result.getValue("bilinmeyen.zzz").mimeType)
        assertNull(result.getValue("klasor").mimeType)
    }

    @Test
    fun `kimlik tam yoldur`() {
        val file = tempFolder.newFile("rapor.txt")

        val node = dataSource.listDirectory(tempFolder.root.absolutePath).single()

        assertEquals(file.absolutePath, node.id)
        assertEquals(file.absolutePath, (node as LocalFileNode).path)
    }

    @Test
    fun `turkce karakterli adlar bozulmaz`() {
        tempFolder.newFile("fatura_şubat_çalışma.txt")

        val node = dataSource.listDirectory(tempFolder.root.absolutePath).single()

        assertEquals("fatura_şubat_çalışma.txt", node.name)
    }

    @Test
    fun `bos klasor bos liste dondurur`() {
        val empty = tempFolder.newFolder("bos")

        assertTrue(dataSource.listDirectory(empty.absolutePath).isEmpty())
    }

    @Test
    fun `olmayan klasor icin hata firlatir`() {
        val missing = File(tempFolder.root, "yok").absolutePath

        assertThrows(FileNotFoundException::class.java) { dataSource.listDirectory(missing) }
    }

    @Test
    fun `klasor yerine dosya verilirse hata firlatir`() {
        val file = tempFolder.newFile("dosya.txt")

        assertThrows(IOException::class.java) { dataSource.listDirectory(file.absolutePath) }
    }

    @Test
    fun `stat var olan ogeyi dondurur yoksa null verir`() {
        val file = tempFolder.newFile("var.txt")

        assertEquals("var.txt", dataSource.stat(file.absolutePath)?.name)
        assertNull(dataSource.stat(File(tempFolder.root, "yok.txt").absolutePath))
    }

    @Test
    fun `exists dosya varligini bildirir`() {
        val file = tempFolder.newFile("var.txt")

        assertTrue(dataSource.exists(file.absolutePath))
        assertFalse(dataSource.exists(File(tempFolder.root, "yok.txt").absolutePath))
    }
}
