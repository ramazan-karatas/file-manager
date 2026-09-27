package dev.rk.systemapps.files.ui.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import dev.rk.systemapps.files.domain.model.FileNode

/** Dosya türü grupları: ikon seçimi ve önizleme kararı bunlara göre verilir. */
enum class FileKind {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    PDF,
    DOCUMENT,
    ARCHIVE,
    APK,
    CODE,
    TEXT,
    OTHER,
    ;

    /** Bu tür için dosyanın kendisinden küçük resim üretilebilir mi? */
    val hasThumbnail: Boolean get() = this == IMAGE || this == VIDEO || this == APK
}

private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz")
private val CODE_EXTENSIONS =
    setOf("kt", "kts", "java", "py", "js", "ts", "json", "xml", "html", "css", "sh", "yml", "yaml")
private val DOCUMENT_EXTENSIONS =
    setOf("doc", "docx", "odt", "rtf", "xls", "xlsx", "ods", "ppt", "pptx", "odp")
private val TEXT_EXTENSIONS = setOf("txt", "md", "log", "ini", "conf", "csv")

fun FileNode.kind(): FileKind {
    if (isDirectory) return FileKind.FOLDER

    val extension = extension
    return when {
        extension == "apk" -> FileKind.APK
        extension == "pdf" -> FileKind.PDF
        extension in ARCHIVE_EXTENSIONS -> FileKind.ARCHIVE
        extension in DOCUMENT_EXTENSIONS -> FileKind.DOCUMENT
        extension in CODE_EXTENSIONS -> FileKind.CODE
        extension in TEXT_EXTENSIONS -> FileKind.TEXT
        mimeType?.startsWith("image/") == true -> FileKind.IMAGE
        mimeType?.startsWith("video/") == true -> FileKind.VIDEO
        mimeType?.startsWith("audio/") == true -> FileKind.AUDIO
        mimeType?.startsWith("text/") == true -> FileKind.TEXT
        else -> FileKind.OTHER
    }
}

fun FileKind.icon(): ImageVector = when (this) {
    FileKind.FOLDER -> Icons.Outlined.Folder
    FileKind.IMAGE -> Icons.Outlined.Image
    FileKind.VIDEO -> Icons.Outlined.Videocam
    FileKind.AUDIO -> Icons.Outlined.MusicNote
    FileKind.PDF -> Icons.Outlined.PictureAsPdf
    FileKind.DOCUMENT -> Icons.Outlined.Description
    FileKind.ARCHIVE -> Icons.Outlined.Archive
    FileKind.APK -> Icons.Outlined.Android
    FileKind.CODE -> Icons.Outlined.Code
    FileKind.TEXT -> Icons.AutoMirrored.Outlined.TextSnippet
    FileKind.OTHER -> Icons.AutoMirrored.Outlined.InsertDriveFile
}
