package dev.rk.systemapps.core.storage.permission

/**
 * Uygulamanın depolama erişim durumu.
 *
 * - [FULL]    : Tüm dosya sistemi okunup yazılabilir (API 30+ için MANAGE_EXTERNAL_STORAGE,
 *               API 28 ve altında WRITE_EXTERNAL_STORAGE).
 * - [LIMITED] : Yalnızca MediaStore üzerinden görünen içerik listelenebilir.
 *               Uygulama bu durumda da çökmeden çalışmalıdır (docs/files/SPEC.md §3.7).
 * - [NONE]    : Hiçbir depolama izni yok.
 */
enum class StorageAccessLevel {
    NONE,
    LIMITED,
    FULL,
    ;

    val canBrowseFileSystem: Boolean get() = this == FULL
    val canReadMedia: Boolean get() = this != NONE
}
