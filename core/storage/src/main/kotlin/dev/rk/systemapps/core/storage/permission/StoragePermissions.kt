package dev.rk.systemapps.core.storage.permission

import android.Manifest
import android.os.Build

/**
 * Depolama izinlerinin API seviyesine göre saf (framework'süz) mantığı.
 * Android çağrılarından ayrı tutuldu ki JVM testleriyle her API seviyesi doğrulanabilsin.
 */
object StoragePermissions {

    /**
     * Sınırlı mod için istenmesi gereken çalışma zamanı izinleri.
     * MANAGE_EXTERNAL_STORAGE bu listede yer almaz — o bir ayarlar ekranından verilir,
     * çalışma zamanı izni değildir.
     */
    fun runtimePermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> = when {
        // API 34+: kullanıcı tüm galeri yerine yalnızca seçtiği öğeleri paylaşabilir.
        // Bu izin istenmezse sistem kısmi erişim seçeneğini hiç sunmaz.
        sdkInt >= 34 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        sdkInt >= 33 -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
        sdkInt >= 29 -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    /** API 30+ cihazlarda tam erişim yalnızca MANAGE_EXTERNAL_STORAGE ile alınabilir. */
    fun requiresManageExternalStorage(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= 30

    /**
     * @param isExternalStorageManager `Environment.isExternalStorageManager()` sonucu (API 30+).
     * @param grantedPermissions o an verilmiş çalışma zamanı izinleri.
     */
    fun resolveAccessLevel(
        sdkInt: Int,
        isExternalStorageManager: Boolean,
        grantedPermissions: Set<String>,
    ): StorageAccessLevel = when {
        sdkInt >= 30 -> when {
            isExternalStorageManager -> StorageAccessLevel.FULL
            hasAnyMediaRead(sdkInt, grantedPermissions) -> StorageAccessLevel.LIMITED
            else -> StorageAccessLevel.NONE
        }

        // API 29: targetSdk 36 olduğu için requestLegacyExternalStorage geçersiz;
        // izin verilse bile erişim scoped storage ile sınırlıdır.
        sdkInt == 29 ->
            if (Manifest.permission.READ_EXTERNAL_STORAGE in grantedPermissions) {
                StorageAccessLevel.LIMITED
            } else {
                StorageAccessLevel.NONE
            }

        // API 26..28: WRITE_EXTERNAL_STORAGE gerçek anlamda tam erişim verir.
        Manifest.permission.WRITE_EXTERNAL_STORAGE in grantedPermissions -> StorageAccessLevel.FULL
        Manifest.permission.READ_EXTERNAL_STORAGE in grantedPermissions -> StorageAccessLevel.LIMITED
        else -> StorageAccessLevel.NONE
    }

    private fun hasAnyMediaRead(sdkInt: Int, granted: Set<String>): Boolean = when {
        sdkInt >= 34 ->
            Manifest.permission.READ_MEDIA_IMAGES in granted ||
                Manifest.permission.READ_MEDIA_VIDEO in granted ||
                Manifest.permission.READ_MEDIA_AUDIO in granted ||
                // Kısmi erişim de sınırlı moddur: seçilen öğeler listelenebilir.
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED in granted

        sdkInt >= 33 ->
            Manifest.permission.READ_MEDIA_IMAGES in granted ||
                Manifest.permission.READ_MEDIA_VIDEO in granted ||
                Manifest.permission.READ_MEDIA_AUDIO in granted

        else -> Manifest.permission.READ_EXTERNAL_STORAGE in granted
    }
}
