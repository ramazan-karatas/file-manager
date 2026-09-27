package dev.rk.systemapps.files.data.file

import android.webkit.MimeTypeMap
import dev.rk.systemapps.files.domain.MimeTypeResolver

/**
 * Önce sistemin MIME tablosuna bakar; orada bulunmayan ama dosya yöneticisinde
 * sık karşılaşılan uzantılar için kendi tablosuna düşer.
 */
class AndroidMimeTypeResolver : MimeTypeResolver {

    override fun resolve(extension: String): String? {
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: FALLBACK[extension]
    }

    private companion object {
        val FALLBACK = mapOf(
            "apk" to "application/vnd.android.package-archive",
            "md" to "text/markdown",
            "kt" to "text/x-kotlin",
            "kts" to "text/x-kotlin",
            "json" to "application/json",
            "yml" to "application/yaml",
            "yaml" to "application/yaml",
            "log" to "text/plain",
            "ini" to "text/plain",
            "conf" to "text/plain",
            "opus" to "audio/opus",
            "heic" to "image/heic",
            "webp" to "image/webp",
            "7z" to "application/x-7z-compressed",
            "rar" to "application/vnd.rar",
            "tar" to "application/x-tar",
        )
    }
}
