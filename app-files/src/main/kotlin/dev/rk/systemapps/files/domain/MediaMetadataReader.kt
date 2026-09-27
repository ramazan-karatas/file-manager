package dev.rk.systemapps.files.domain

import dev.rk.systemapps.files.domain.model.MediaInfo

/**
 * Görsel çözünürlüğü ve medya süresi okur. Arayüz olmasının sebebi
 * `BitmapFactory`/`MediaMetadataRetriever`in JVM testlerinde çalışmaması.
 */
fun interface MediaMetadataReader {
    fun read(path: String, mimeType: String?): MediaInfo?
}
