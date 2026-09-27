package dev.rk.systemapps.core.storage.volume

import android.os.Environment

/**
 * Depolama kökleri. F-1.10'da bu nesne `StorageManager` üzerinden tüm birimleri
 * ([dev.rk.systemapps.core.storage.model.StorageVolumeInfo]) döndürecek şekilde genişleyecek.
 */
object StoragePaths {

    /** Dahili depolamanın kökü, tipik olarak `/storage/emulated/0`. */
    fun primaryExternalStorage(): String =
        @Suppress("DEPRECATION")
        Environment.getExternalStorageDirectory().absolutePath
}
