package dev.rk.systemapps.core.storage.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageVolumeInfoTest {

    private fun volume(total: Long, free: Long) = StorageVolumeInfo(
        id = "/storage/emulated/0",
        label = "",
        path = "/storage/emulated/0",
        totalBytes = total,
        freeBytes = free,
        isRemovable = false,
        isPrimary = true,
    )

    @Test
    fun `kullanilan alan toplamdan bostan cikarilir`() {
        assertEquals(30L, volume(total = 100, free = 70).usedBytes)
    }

    @Test
    fun `doluluk orani sifir ile bir arasindadir`() {
        assertEquals(0.3f, volume(total = 100, free = 70).usedFraction, 0.001f)
    }

    @Test
    fun `bilinmeyen boyut sifira bolunmeye yol acmaz`() {
        assertEquals(0f, volume(total = 0, free = 0).usedFraction, 0.001f)
        assertEquals(0L, volume(total = 0, free = 0).usedBytes)
    }

    @Test
    fun `bos alan toplamdan buyukse kullanilan negatif olmaz`() {
        assertEquals(0L, volume(total = 100, free = 200).usedBytes)
    }
}
