package dev.rk.systemapps.core.storage.volume

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.rk.systemapps.core.storage.model.StorageVolumeInfo
import java.io.File
import javax.inject.Singleton

/**
 * Depolama kökleri. Arayüz olmasının sebebi `Environment`/`StorageManager` çağrılarının
 * JVM testlerinde çalışmaması; testler sahte bir uygulama verir.
 */
interface StorageLocations {

    /** Dahili depolamanın kökü, tipik olarak `/storage/emulated/0`. */
    fun primaryExternalStorage(): String

    /** Dahili depolama + takılı SD kart / USB bellek. */
    fun volumes(): List<StorageVolumeInfo>
}

internal class AndroidStorageLocations(
    private val context: Context,
) : StorageLocations {

    override fun primaryExternalStorage(): String =
        @Suppress("DEPRECATION")
        Environment.getExternalStorageDirectory().absolutePath

    override fun volumes(): List<StorageVolumeInfo> {
        val primary = File(primaryExternalStorage())
        val volumes = mutableListOf(primary.toVolumeInfo(isPrimary = true))

        // Çıkarılabilir birimlerin kökü doğrudan alınamıyor; uygulamaya özel dizinden
        // `/Android/data/<paket>/files` kısmı kırpılarak bulunuyor. API 26'dan beri
        // çalışan tek taşınabilir yol bu.
        context.getExternalFilesDirs(null)
            .filterNotNull()
            .drop(1)
            .mapNotNull { it.volumeRoot() }
            .forEach { root -> volumes += root.toVolumeInfo(isPrimary = false) }

        return volumes
    }

    private fun File.volumeRoot(): File? {
        val marker = "/Android/data/"
        val index = absolutePath.indexOf(marker)
        return if (index > 0) File(absolutePath.substring(0, index)) else null
    }

    private fun File.toVolumeInfo(isPrimary: Boolean): StorageVolumeInfo {
        val stat = runCatching { StatFs(absolutePath) }.getOrNull()
        return StorageVolumeInfo(
            id = absolutePath,
            label = describe(this, isPrimary),
            path = absolutePath,
            totalBytes = stat?.let { it.blockCountLong * it.blockSizeLong } ?: 0L,
            freeBytes = stat?.let { it.availableBlocksLong * it.blockSizeLong } ?: 0L,
            isRemovable = !isPrimary,
            isPrimary = isPrimary,
        )
    }

    /** Sistemin verdiği ad ("SD kart" gibi) varsa o kullanılır; yoksa klasör adı. */
    private fun describe(directory: File, isPrimary: Boolean): String {
        if (isPrimary) return ""

        val description = runCatching {
            val manager = context.getSystemService(StorageManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager?.storageVolumes
                    ?.firstOrNull { it.directory?.absolutePath == directory.absolutePath }
                    ?.getDescription(context)
            } else {
                null
            }
        }.getOrNull()

        return description ?: directory.name
    }
}

@Module
@InstallIn(SingletonComponent::class)
object StorageLocationsModule {

    @Provides
    @Singleton
    fun provideStorageLocations(@ApplicationContext context: Context): StorageLocations =
        AndroidStorageLocations(context)
}
