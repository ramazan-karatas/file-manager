package dev.rk.systemapps.files.domain.model

enum class ClipboardMode {
    COPY,
    MOVE,
}

/**
 * Yapıştırılmayı bekleyen öğeler. Uygulama yeniden başlasa da korunur
 * (docs/files/SPEC.md §3.4), bu yüzden DataStore'da tutulur.
 */
data class FileClipboard(
    val paths: List<String>,
    val mode: ClipboardMode,
) {
    val isEmpty: Boolean get() = paths.isEmpty()
}
