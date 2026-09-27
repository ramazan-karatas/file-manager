package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.model.SortBy
import dev.rk.systemapps.files.util.dirNode
import dev.rk.systemapps.files.util.fileNode
import dev.rk.systemapps.files.util.names
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FileNodeSorterTest {

    private val tr = Locale.forLanguageTag("tr-TR")

    @Test
    fun `gizli dosyalar varsayilan olarak suzulur`() {
        val nodes = listOf(fileNode("rapor.pdf"), fileNode(".gizli"), dirNode(".thumbnails"))

        val result = FileNodeSorter.apply(nodes, ListingOptions(), tr)

        assertEquals(listOf("rapor.pdf"), result.names())
    }

    @Test
    fun `showHidden acikken gizli dosyalar da listelenir`() {
        val nodes = listOf(fileNode("rapor.pdf"), fileNode(".gizli"))

        val result = FileNodeSorter.apply(nodes, ListingOptions(showHidden = true), tr)

        assertEquals(listOf(".gizli", "rapor.pdf"), result.names())
    }

    @Test
    fun `klasorler varsayilan olarak ustte durur`() {
        val nodes = listOf(fileNode("aaa.txt"), dirNode("zzz"), fileNode("bbb.txt"), dirNode("mmm"))

        val result = FileNodeSorter.apply(nodes, ListingOptions(), tr)

        assertEquals(listOf("mmm", "zzz", "aaa.txt", "bbb.txt"), result.names())
    }

    @Test
    fun `klasorler azalan siralamada da ustte kalir`() {
        val nodes = listOf(fileNode("aaa.txt"), dirNode("zzz"), fileNode("bbb.txt"), dirNode("mmm"))

        val result = FileNodeSorter.apply(nodes, ListingOptions(ascending = false), tr)

        assertEquals(listOf("zzz", "mmm", "bbb.txt", "aaa.txt"), result.names())
    }

    @Test
    fun `foldersFirst kapaliyken klasorler dosyalarla karisir`() {
        val nodes = listOf(dirNode("zzz"), fileNode("aaa.txt"), dirNode("mmm"))

        val result = FileNodeSorter.apply(nodes, ListingOptions(foldersFirst = false), tr)

        assertEquals(listOf("aaa.txt", "mmm", "zzz"), result.names())
    }

    @Test
    fun `turkce siralama yerel kurallara uyar`() {
        // ASCII sıralamasında 'ş' (U+015F) 't'den sonra gelir ve sıra bozulurdu.
        val nodes = listOf(fileNode("tavşan"), fileNode("şeker"), fileNode("sarı"))

        val result = FileNodeSorter.apply(nodes, ListingOptions(foldersFirst = false), tr)

        assertEquals(listOf("sarı", "şeker", "tavşan"), result.names())
    }

    @Test
    fun `boyuta gore siralamada klasorler kendi arasinda ada gore dizilir`() {
        val nodes = listOf(
            fileNode("buyuk.bin", size = 5_000),
            dirNode("zklasor"),
            fileNode("kucuk.bin", size = 10),
            dirNode("aklasor"),
        )

        val result = FileNodeSorter.apply(nodes, ListingOptions(sortBy = SortBy.SIZE), tr)

        assertEquals(listOf("aklasor", "zklasor", "kucuk.bin", "buyuk.bin"), result.names())
    }

    @Test
    fun `tarihe gore azalan siralama en yeniyi one alir`() {
        val nodes = listOf(
            fileNode("eski.txt", lastModified = 1_000),
            fileNode("yeni.txt", lastModified = 9_000),
            fileNode("orta.txt", lastModified = 5_000),
        )

        val result = FileNodeSorter.apply(
            nodes,
            ListingOptions(sortBy = SortBy.DATE, ascending = false),
            tr,
        )

        assertEquals(listOf("yeni.txt", "orta.txt", "eski.txt"), result.names())
    }

    @Test
    fun `ture gore siralama uzantiya bakar`() {
        val nodes = listOf(fileNode("b.zip"), fileNode("a.txt"), fileNode("c.pdf"))

        val result = FileNodeSorter.apply(nodes, ListingOptions(sortBy = SortBy.TYPE), tr)

        assertEquals(listOf("c.pdf", "a.txt", "b.zip"), result.names())
    }

    @Test
    fun `esit anahtarlarda sira deterministiktir`() {
        val nodes = listOf(
            fileNode("readme", lastModified = 100),
            fileNode("README", lastModified = 100),
        )
        val options = ListingOptions(sortBy = SortBy.DATE)

        val first = FileNodeSorter.apply(nodes, options, tr).names()
        val second = FileNodeSorter.apply(nodes.reversed(), options, tr).names()

        assertEquals(first, second)
    }

    @Test
    fun `bos liste bos doner`() {
        assertEquals(emptyList<String>(), FileNodeSorter.apply(emptyList(), ListingOptions(), tr).names())
    }
}
