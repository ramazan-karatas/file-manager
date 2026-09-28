package dev.rk.systemapps.files.data.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rk.systemapps.files.domain.model.FileCategory
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.LocalFileNode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kategori ve "son değişenler" listelerinin kaynağı. Arayüz olmasının sebebi
 * `ContentResolver`'ın JVM testlerinde çalışmaması; testler sahte bir uygulama verir.
 */
interface MediaCatalog {

    fun query(category: FileCategory, limit: Int = DEFAULT_LIMIT): List<FileNode>

    /** Son [withinDays] günde değişmiş dosyalar, yeniden eskiye. */
    fun recent(
        limit: Int = DEFAULT_LIMIT,
        withinDays: Int = RECENT_DAYS,
    ): List<FileNode>

    companion object {
        const val DEFAULT_LIMIT = 500
        const val RECENT_DAYS = 7
    }
}

/**
 * Kategori listeleri ve "son değişenler" için MediaStore sorguları (docs/files/SPEC.md §3.1).
 *
 * Kimlik olarak `DATA` (tam yol) okunuyor: uygulamanın geri kalanı [FileNode.id]'yi yol
 * olarak kullanıyor ve tam dosya erişimi zaten var, bu yüzden `content://` kimliğe
 * geçmenin faydası yok.
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MediaCatalog {

    override fun query(category: FileCategory, limit: Int): List<FileNode> {
        val (selection, args) = category.selection()
        return queryFiles(selection, args, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC", limit)
    }

    override fun recent(limit: Int, withinDays: Int): List<FileNode> {
        val since = (System.currentTimeMillis() / 1000) - (withinDays * SECONDS_PER_DAY)
        return queryFiles(
            selection = "${MediaStore.Files.FileColumns.DATE_MODIFIED} >= ? AND " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL",
            args = arrayOf(since.toString()),
            order = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
            limit = limit,
        )
    }

    private fun queryFiles(
        selection: String,
        args: Array<String>,
        order: String,
        limit: Int,
    ): List<FileNode> {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )

        val cursor: Cursor = context.contentResolver.query(
            MediaStore.Files.getContentUri(VOLUME_EXTERNAL),
            projection,
            selection,
            args,
            order,
        ) ?: return emptyList()

        return cursor.use { it.readNodes(limit) }
    }

    private fun Cursor.readNodes(limit: Int): List<FileNode> {
        val dataIndex = getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
        val nameIndex = getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeIndex = getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateIndex = getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
        val mimeIndex = getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

        val nodes = ArrayList<FileNode>(minOf(count, limit))
        while (moveToNext() && nodes.size < limit) {
            val path = getString(dataIndex) ?: continue
            val name = getString(nameIndex) ?: path.substringAfterLast('/')
            nodes += LocalFileNode(
                id = path,
                name = name,
                isDirectory = false,
                size = getLong(sizeIndex),
                // MediaStore saniye tutar, uygulamanın geri kalanı milisaniye.
                lastModified = getLong(dateIndex) * 1000L,
                mimeType = getString(mimeIndex),
                isHidden = name.startsWith("."),
            )
        }
        return nodes
    }

    private fun FileCategory.selection(): Pair<String, Array<String>> {
        val mime = MediaStore.Files.FileColumns.MIME_TYPE
        val name = MediaStore.Files.FileColumns.DISPLAY_NAME

        return when (this) {
            FileCategory.IMAGES -> "$mime LIKE ?" to arrayOf("image/%")
            FileCategory.VIDEO -> "$mime LIKE ?" to arrayOf("video/%")
            FileCategory.AUDIO -> "$mime LIKE ?" to arrayOf("audio/%")

            FileCategory.DOCUMENTS -> {
                val types = DOCUMENT_MIME_TYPES.joinToString(" OR ") { "$mime = ?" }
                "($types)" to DOCUMENT_MIME_TYPES
            }

            // APK ve arşivler MediaStore'da tutarlı bir MIME türüyle indekslenmiyor,
            // bu yüzden dosya adının uzantısına bakılıyor.
            FileCategory.APK -> "$name LIKE ?" to arrayOf("%.apk")

            FileCategory.ARCHIVES -> {
                val patterns = ARCHIVE_EXTENSIONS.map { "%.$it" }.toTypedArray()
                val clause = patterns.joinToString(" OR ") { "$name LIKE ?" }
                "($clause)" to patterns
            }
        }
    }

    private companion object {
        const val VOLUME_EXTERNAL = "external"
        const val SECONDS_PER_DAY = 24 * 60 * 60

        val DOCUMENT_MIME_TYPES = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
        )

        val ARCHIVE_EXTENSIONS = listOf("zip", "rar", "7z", "tar", "gz", "tgz")
    }
}
