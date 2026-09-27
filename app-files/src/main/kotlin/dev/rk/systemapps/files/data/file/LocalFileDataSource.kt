package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.files.domain.MediaMetadataReader
import dev.rk.systemapps.files.domain.MimeTypeResolver
import dev.rk.systemapps.files.domain.model.DirectoryStats
import dev.rk.systemapps.files.domain.model.FileDetails
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
 *
 * Not: `java.nio.file` ile (dizin akışı + öğe başına tek `readAttributes`) bir sürüm denendi.
 * Teoride 3 yerine 1 stat yapmasına rağmen `/sdcard` FUSE üzerinde kazanç vermedi
 * (10.000 dosyada aynı koşulda NIO 2586 ms, File API 2296 ms), bu yüzden basit olan tutuldu.
 */
class LocalFileDataSource(
    private val mimeTypeResolver: MimeTypeResolver,
    private val mediaMetadataReader: MediaMetadataReader = MediaMetadataReader { _, _ -> null },
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

    fun details(path: String): FileDetails {
        val file = File(path)
        if (!file.exists()) throw FileNotFoundException("Bulunamadı: $path")
        val node = file.toNode()
        return FileDetails(
            node = node,
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            canExecute = file.canExecute(),
            media = if (node.isDirectory) null else mediaMetadataReader.read(path, node.mimeType),
        )
    }

    fun directoryStats(path: String): DirectoryStats {
        val directory = File(path)
        if (!directory.isDirectory) throw IOException("Bu bir klasör değil: $path")

        var bytes = 0L
        var files = 0
        var directories = 0

        fun walk(file: File) {
            val children = file.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory) {
                    directories++
                    walk(child)
                } else {
                    files++
                    bytes += child.length()
                }
            }
        }
        walk(directory)

        return DirectoryStats(totalBytes = bytes, fileCount = files, directoryCount = directories)
    }

    fun rename(path: String, newName: String): FileNode {
        require(newName.isValidFileName()) { "Geçersiz dosya adı: $newName" }
        val file = File(path)
        if (!file.exists()) throw FileNotFoundException("Bulunamadı: $path")

        val target = File(file.parentFile, newName)
        if (target.exists()) throw IOException("Bu adda bir öğe zaten var: $newName")
        if (!file.renameTo(target)) throw IOException("Yeniden adlandırılamadı: $path")

        return target.toNode()
    }

    fun createDirectory(parentPath: String, name: String): FileNode {
        require(name.isValidFileName()) { "Geçersiz klasör adı: $name" }
        val target = File(parentPath, name)
        if (target.exists()) throw IOException("Bu adda bir öğe zaten var: $name")
        if (!target.mkdirs()) throw IOException("Klasör oluşturulamadı: ${target.path}")
        return target.toNode()
    }

    fun createFile(parentPath: String, name: String): FileNode {
        require(name.isValidFileName()) { "Geçersiz dosya adı: $name" }
        val target = File(parentPath, name)
        if (target.exists()) throw IOException("Bu adda bir öğe zaten var: $name")
        if (!target.createNewFile()) throw IOException("Dosya oluşturulamadı: ${target.path}")
        return target.toNode()
    }

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

/**
 * Dosya adı doğrulaması: yol ayırıcı ve boş ad kabul edilmez. Android'de dosya adları
 * başka karakterler açısından serbesttir, bu yüzden fazla kısıtlanmıyor.
 */
internal fun String.isValidFileName(): Boolean =
    isNotBlank() && !contains('/') && this != "." && this != ".."
