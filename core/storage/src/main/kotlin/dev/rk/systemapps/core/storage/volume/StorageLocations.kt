package dev.rk.systemapps.core.storage.volume

import android.os.Environment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Depolama kökleri. Arayüz olmasının sebebi `Environment` çağrılarının JVM testlerinde
 * çalışmaması; testler sahte bir kök verir.
 *
 * F-1.10'da tüm birimleri ([dev.rk.systemapps.core.storage.model.StorageVolumeInfo])
 * döndüren bir fonksiyonla genişleyecek.
 */
fun interface StorageLocations {
    /** Dahili depolamanın kökü, tipik olarak `/storage/emulated/0`. */
    fun primaryExternalStorage(): String
}

@Module
@InstallIn(SingletonComponent::class)
object StorageLocationsModule {

    @Provides
    @Singleton
    fun provideStorageLocations(): StorageLocations = StorageLocations {
        @Suppress("DEPRECATION")
        Environment.getExternalStorageDirectory().absolutePath
    }
}
