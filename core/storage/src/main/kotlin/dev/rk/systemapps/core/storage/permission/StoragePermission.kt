package dev.rk.systemapps.core.storage.permission

/**
 * Uygulamanın depolama erişim durumu.
 *
 * [LIMITED] = MANAGE_EXTERNAL_STORAGE yok; yalnızca MediaStore üzerinden okunabilen
 * içerik gösterilebilir. Uygulama bu durumda da çökmeden çalışmalıdır
 * (docs/files/SPEC.md §3.7).
 */
enum class StorageAccessLevel {
    NONE,
    LIMITED,
    FULL,
}
