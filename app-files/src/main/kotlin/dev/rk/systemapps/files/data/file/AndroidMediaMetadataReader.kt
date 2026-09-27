package dev.rk.systemapps.files.data.file

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import dev.rk.systemapps.files.domain.MediaMetadataReader
import dev.rk.systemapps.files.domain.model.MediaInfo

/**
 * Görsellerde yalnızca başlık okunur (`inJustDecodeBounds`), tüm görsel belleğe alınmaz.
 * Video/ses için süre `MediaMetadataRetriever`dan gelir.
 */
class AndroidMediaMetadataReader : MediaMetadataReader {

    override fun read(path: String, mimeType: String?): MediaInfo? = when {
        mimeType == null -> null
        mimeType.startsWith("image/") -> readImage(path)
        mimeType.startsWith("video/") || mimeType.startsWith("audio/") -> readMedia(path)
        else -> null
    }?.takeIf { !it.isEmpty }

    private fun readImage(path: String): MediaInfo? = runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0) null else MediaInfo(options.outWidth, options.outHeight)
    }.getOrNull()

    private fun readMedia(path: String): MediaInfo? = runCatching {
        // MediaMetadataRetriever ancak API 29'da AutoCloseable oldu; minSdk 26 olduğu
        // için `use` kullanılamaz, release() elle çağrılıyor.
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            MediaInfo(
                width = retriever.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt(),
                height = retriever.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt(),
                durationMs = retriever.extract(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong(),
            )
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun MediaMetadataRetriever.extract(key: Int): String? =
        extractMetadata(key)?.takeIf { it.isNotBlank() }
}
