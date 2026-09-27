package dev.rk.systemapps.files.ui.browser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import dev.rk.systemapps.files.R
import dev.rk.systemapps.files.domain.model.FileNode
import java.io.File

/**
 * Dosyayı başka uygulamaya açma ve paylaşma.
 *
 * `file://` URI'si API 24'ten beri başka uygulamalara verilemez, bu yüzden her şey
 * [FileProvider] üzerinden `content://` olarak paylaşılıyor.
 */
object FileActions {

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    fun contentUri(context: Context, node: FileNode): Uri =
        FileProvider.getUriForFile(context, authority(context), File(node.id))

    /** @return açabilecek bir uygulama bulunduysa true. */
    fun open(context: Context, node: FileNode): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri(context, node), node.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return context.startActivitySafely(intent)
    }

    /**
     * `ShareCompat` kullanılıyor çünkü intent'i `createChooser` sarmaladığında
     * `FLAG_GRANT_READ_URI_PERMISSION` tek başına yetmiyor: sistem seçicisi önizlemeyi
     * çizmek için URI'yi kendisi okumak istiyor ve izin reddi alıyor. `ShareCompat`
     * akışları `ClipData` olarak da ekleyip izni seçiciye taşıyor.
     */
    fun share(context: Context, nodes: List<FileNode>): Boolean {
        if (nodes.isEmpty()) return false

        val builder = ShareCompat.IntentBuilder(context)
            .setType(if (nodes.size == 1) nodes.single().mimeType ?: "*/*" else nodes.commonMimeType())
            .setChooserTitle(R.string.action_share)
        nodes.forEach { node -> builder.addStream(contentUri(context, node)) }

        val chooser = builder.createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return context.startActivitySafely(chooser)
    }

    private fun List<FileNode>.commonMimeType(): String {
        val types = mapNotNull { it.mimeType }.distinct()
        return when {
            types.isEmpty() -> "*/*"
            types.size == 1 -> types.single()
            types.map { it.substringBefore('/') }.distinct().size == 1 ->
                "${types.first().substringBefore('/')}/*"

            else -> "*/*"
        }
    }

    private fun Context.startActivitySafely(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
