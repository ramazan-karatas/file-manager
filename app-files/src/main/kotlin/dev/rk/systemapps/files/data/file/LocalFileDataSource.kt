package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.files.domain.MimeTypeResolver
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.LocalFileNode
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Dosya sistemine doğrudan erişen veri kaynağı. Hata durumunda exception fırlatır;
 * [dev.rk.systemapps.core.common.result.Outcome]'a çevirmek repository'nin işidir.
 *
 * Çağıranlar bu sınıfı IO dispatcher'ında kullanmalıdır.
 */
class LocalFileDataSource(
    private val mimeTypeResolver: MimeTypeResolver,
) {

    fun listDirectory(path: String): List<FileNode> {
        val directory = File(path)
        if (!directory.exists()) {
            throw FileNotFoundException("Klasör bulunamadı: $path")
        }
        if (!directory.isDirectory) {
            throw IOException("Bu bir klasör değil: $path")
        }
        // listFiles() izin sorunlarında ve IO hatasında null döner.
        val children = directory.listFiles()
            ?: throw IOException("Klasör okunamadı: $path")

        return children.map { it.toNode() }
    }

    fun stat(path: String): FileNode? {
        val file = File(path)
        return if (file.exists()) file.toNode() else null
    }

    fun exists(path: String): Boolean = File(path).exists()

    private fun File.toNode(): LocalFileNode {
        val directory = isDirectory
        return LocalFileNode(
            id = absolutePath,
            name = name,
            isDirectory = directory,
            size = if (directory) FileNode.SIZE_UNKNOWN else length(),
            lastModified = lastModified(),
            mimeType = if (directory) {
                null
            } else {
                mimeTypeResolver.resolve(extension.lowercase())
            },
            // File.isHidden() Windows'ta DOS özniteliğine bakar; Android'de ölçüt
            // adın nokta ile başlamasıdır, bu yüzden ada bakılıyor.
            isHidden = name.startsWith("."),
        )
    }
}
