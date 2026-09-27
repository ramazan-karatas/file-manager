package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.model.SortBy
import java.text.CollationKey
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
        if (visible.isEmpty()) return emptyList()

        // Türkçe'de "ş", "ı", "ç" ASCII karşılaştırmasıyla yanlış yere düşer; bu yüzden
        // yerel duyarlı Collator kullanılıyor. Collator.compare pahalı olduğundan
        // (10.000 öğede ~130.000 karşılaştırma) her öğe için bir kez CollationKey
        // üretilip karşılaştırmalar anahtarlar üzerinden yapılıyor.
        val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        val needsExtensionKey = options.sortBy == SortBy.TYPE

        val keyed = visible.map { node ->
            Keyed(
                node = node,
                nameKey = collator.getCollationKey(node.name),
                extensionKey = if (needsExtensionKey) {
                    collator.getCollationKey(node.extension)
                } else {
                    null
                },
            )
        }

        return keyed.sortedWith(comparator(options)).map { it.node }
    }

    private class Keyed(
        val node: FileNode,
        val nameKey: CollationKey,
        val extensionKey: CollationKey?,
    )

    private fun comparator(options: ListingOptions): Comparator<Keyed> {
        val byName = Comparator<Keyed> { a, b -> a.nameKey.compareTo(b.nameKey) }

        val primary: Comparator<Keyed> = when (options.sortBy) {
            SortBy.NAME -> byName

            // Klasörlerin boyutu hesaplanmadığı için kendi aralarında ada göre sıralanır.
            SortBy.SIZE -> Comparator { a, b ->
                when {
                    a.node.isDirectory && b.node.isDirectory -> a.nameKey.compareTo(b.nameKey)
                    else -> a.node.size.compareTo(b.node.size)
                }
            }

            SortBy.DATE -> Comparator { a, b -> a.node.lastModified.compareTo(b.node.lastModified) }

            SortBy.TYPE -> Comparator { a, b ->
                compareValues(a.extensionKey, b.extensionKey)
            }
        }

        val directed = if (options.ascending) primary else primary.reversed()
        // Eşitlikte önce artan ad, sonra kimlik: Collator büyük/küçük harfi eşit saydığı
        // için ("README" ve "readme") son kırıcı olmadan sıra girdiye bağlı kalırdı.
        val withTieBreak = directed.thenComparing(byName).thenBy { it.node.id }

        return if (options.foldersFirst) {
            // Klasörler yön ne olursa olsun üstte kalır.
            Comparator<Keyed> { a, b ->
                b.node.isDirectory.compareTo(a.node.isDirectory)
            }.thenComparing(withTieBreak)
        } else {
            withTieBreak
        }
    }
}
