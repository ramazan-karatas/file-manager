package dev.rk.systemapps.files.domain.model

/**
 * Ana ekrandaki kategoriler. Klasör değil, MediaStore sorgusudur
 * (docs/files/SPEC.md §3.1). "İndirilenler" burada yok; o gerçek bir klasör
 * olduğu için doğrudan gezginde açılıyor.
 */
enum class FileCategory {
    IMAGES,
    VIDEO,
    AUDIO,
    DOCUMENTS,
    APK,
    ARCHIVES,
}
