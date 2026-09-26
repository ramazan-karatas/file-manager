package dev.rk.systemapps.core.common.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    private val tr = Locale.forLanguageTag("tr-TR")

    @Test
    fun `bayt alti degerler ondalikszdir`() {
        assertEquals("0 B", formatBytes(0, tr))
        assertEquals("512 B", formatBytes(512, tr))
        assertEquals("1023 B", formatBytes(1023, tr))
    }

    @Test
    fun `turkce yerelde ondalik ayraci virguldur`() {
        assertEquals("1,5 GB", formatBytes(1_610_612_736L, tr))
        assertEquals("1,0 KB", formatBytes(1024, tr))
    }

    @Test
    fun `yuz ustu degerlerde ondalik gosterilmez`() {
        assertEquals("500 MB", formatBytes(524_288_000L, tr))
    }

    @Test
    fun `negatif boyut hesaplanmamis demektir`() {
        assertEquals("—", formatBytes(-1, tr))
    }

    @Test
    fun `sure bir saat altinda dakika saniye`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("3:07", formatDuration(187_000))
        assertEquals("59:59", formatDuration(3_599_000))
    }

    @Test
    fun `sure bir saat ustunde saat dakika saniye`() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("2:05:09", formatDuration(7_509_000))
    }

    @Test
    fun `gecersiz sure cizgi doner`() {
        assertEquals("--:--", formatDuration(-1))
    }

    @Test
    fun `yuzde hesabi`() {
        assertEquals("50%", formatPercent(50, 100, tr))
        assertEquals("1,5%", formatPercent(15, 1000, tr))
        assertEquals("—", formatPercent(10, 0, tr))
    }
}
