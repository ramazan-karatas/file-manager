package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.model.SortBy
import java.text.Collator
import java.util.Locale

/**
 * Listeleme süzgeci ve sıralaması. Saf Kotlin — Android'e bağlı değil, testi doğrudan.
 */
object FileNodeSorter {

    fun apply(
        nodes: List<FileNode>,
        options: ListingOptions,
        locale: Locale = Locale.getDefault(),
    ): List<FileNode> {
        val visible = if (options.showHidden) nodes else nodes.filterNot { it.isHidden }
        return visible.sortedWith(comparator(options, locale))
    }

    private fun comparator(options: ListingOptions, locale: Locale): Comparator<FileNode> {
        // Türkçe'de "İ/ı/ş/ç" sıralaması ASCII karşılaştırmasıyla yanlış çıkar;
        // bu yüzden yerel duyarlı Collator kullanılıyor.
        val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        val byName = Comparator<FileNode> { a, b -> collator.compare(a.name, b.name) }

        val primary: Comparator<FileNode> = when (options.sortBy) {
            SortBy.NAME -> byName

            // Klasörlerin boyutu hesaplanmadığı için kendi aralarında ada göre sıralanır.
            SortBy.SIZE -> Comparator { a, b ->
                when {
                    a.isDirectory && b.isDirectory -> collator.compare(a.name, b.name)
                    else -> a.size.compareTo(b.size)
                }
            }

            SortBy.DATE -> Comparator { a, b -> a.lastModified.compareTo(b.lastModified) }

            SortBy.TYPE -> Comparator { a, b -> collator.compare(a.extension, b.extension) }
        }

        val directed = if (options.ascending) primary else primary.reversed()
        // Eşitlikte önce artan ad, sonra kimlik: Collator büyük/küçük harfi eşit saydığı
        // için ("README" ve "readme") son kırıcı olmadan sıra girdiye bağlı kalırdı.
        val withTieBreak = directed.thenComparing(byName).thenBy { it.id }

        return if (options.foldersFirst) {
            // Klasörler yön ne olursa olsun üstte kalır.
            Comparator<FileNode> { a, b ->
                b.isDirectory.compareTo(a.isDirectory)
            }.thenComparing(withTieBreak)
        } else {
            withTieBreak
        }
    }
}
