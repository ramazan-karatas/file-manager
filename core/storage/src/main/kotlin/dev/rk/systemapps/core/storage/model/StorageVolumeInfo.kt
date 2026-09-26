package dev.rk.systemapps.core.storage.model

/**
 * Bir depolama birimi (dahili depolama, SD kart, USB OTG).
 * Bkz. docs/files/SPEC.md §4.
 */
data class StorageVolumeInfo(
    val id: String,
    val label: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val isRemovable: Boolean,
    val isPrimary: Boolean,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)

    /** 0f..1f — depolama kartındaki doluluk çubuğu için. */
    val usedFraction: Float
        get() = if (totalBytes <= 0L) 0f else (usedBytes.toFloat() / totalBytes.toFloat())
}
