package dev.rk.systemapps.files.domain.model

/** Klasör içeriğinin özeti; hesaplanması pahalı olduğu için ayrıca istenir. */
data class DirectoryStats(
    val totalBytes: Long,
    val fileCount: Int,
    val directoryCount: Int,
) {
    val itemCount: Int get() = fileCount + directoryCount
}

/** Görsel/video/ses dosyalarından okunan ek bilgi (docs/files/SPEC.md §3.6). */
data class MediaInfo(
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
) {
    val isEmpty: Boolean get() = width == null && height == null && durationMs == null
}

/** Özellikler diyaloğunun gösterdiği her şey. */
data class FileDetails(
    val node: FileNode,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean,
    val media: MediaInfo? = null,
) {
    /** `rwx` biçiminde izin metni; okunamayan hak `-` ile gösterilir. */
    val permissionString: String
        get() = buildString {
            append(if (canRead) 'r' else '-')
            append(if (canWrite) 'w' else '-')
            append(if (canExecute) 'x' else '-')
        }
}
