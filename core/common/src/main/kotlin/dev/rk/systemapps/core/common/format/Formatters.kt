package dev.rk.systemapps.core.common.format

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val BINARY_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

/**
 * Dosya boyutunu ikilik (1024) tabanda biçimlendirir — dosya yöneticilerinin
 * yerleşik davranışı budur. Bayt altında ondalık gösterilmez.
 */
fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes ${BINARY_UNITS[0]}"

    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < BINARY_UNITS.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    val pattern = if (value >= 100.0) "%.0f %s" else "%.1f %s"
    return String.format(locale, pattern, value, BINARY_UNITS[unitIndex])
}

/** Parça/medya süresi: 1 saat altında `d:ss`, üstünde `s:dd:ss`. */
fun formatDuration(millis: Long): String {
    if (millis < 0) return "--:--"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

fun formatDate(epochMillis: Long, locale: Locale = Locale.getDefault()): String {
    if (epochMillis <= 0) return "—"
    val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
    return format.format(Date(epochMillis))
}

/** Toplam içindeki pay — depolama analizi ekranında kullanılır. */
fun formatPercent(part: Long, total: Long, locale: Locale = Locale.getDefault()): String {
    if (total <= 0 || part < 0) return "—"
    val ratio = part.toDouble() / total.toDouble() * 100.0
    val pattern = if (abs(ratio) < 10.0) "%.1f%%" else "%.0f%%"
    return String.format(locale, pattern, ratio)
}
