package dev.rk.systemapps.files.data.operation

import android.content.Context
import android.media.MediaScannerConnection
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dosya işlemlerinden sonra MediaStore'u haberdar eder; aksi hâlde silinen bir fotoğraf
 * galeride görünmeye devam eder (docs/files/SPEC.md §5.8).
 */
fun interface MediaScanner {
    fun scan(paths: List<String>)
}

@Singleton
class AndroidMediaScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MediaScanner {

    override fun scan(paths: List<String>) {
        if (paths.isEmpty()) return
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
    }
}
