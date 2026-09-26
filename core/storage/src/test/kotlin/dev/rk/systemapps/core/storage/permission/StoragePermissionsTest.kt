package dev.rk.systemapps.core.storage.permission

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoragePermissionsTest {

    private val readExternal = Manifest.permission.READ_EXTERNAL_STORAGE
    private val writeExternal = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val readImages = Manifest.permission.READ_MEDIA_IMAGES
    private val readAudio = Manifest.permission.READ_MEDIA_AUDIO

    @Test
    fun `api 34 manage izni varsa tam erisim`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 34,
            isExternalStorageManager = true,
            grantedPermissions = emptySet(),
        )
        assertEquals(StorageAccessLevel.FULL, level)
    }

    @Test
    fun `api 34 manage izni yoksa medya izniyle sinirli mod`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 34,
            isExternalStorageManager = false,
            grantedPermissions = setOf(readImages),
        )
        assertEquals(StorageAccessLevel.LIMITED, level)
    }

    @Test
    fun `api 34 hicbir izin yoksa erisim yok`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 34,
            isExternalStorageManager = false,
            grantedPermissions = emptySet(),
        )
        assertEquals(StorageAccessLevel.NONE, level)
    }

    @Test
    fun `api 31 medya izinleri henuz bolunmemistir`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 31,
            isExternalStorageManager = false,
            grantedPermissions = setOf(readExternal),
        )
        assertEquals(StorageAccessLevel.LIMITED, level)
    }

    @Test
    fun `api 31 medya izni 33 sabitleriyle verilmis sayilmaz`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 31,
            isExternalStorageManager = false,
            grantedPermissions = setOf(readAudio),
        )
        assertEquals(StorageAccessLevel.NONE, level)
    }

    @Test
    fun `api 29 scoped storage nedeniyle en fazla sinirli olabilir`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 29,
            isExternalStorageManager = false,
            grantedPermissions = setOf(readExternal, writeExternal),
        )
        assertEquals(StorageAccessLevel.LIMITED, level)
    }

    @Test
    fun `api 28 yazma izni tam erisim demektir`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 28,
            isExternalStorageManager = false,
            grantedPermissions = setOf(readExternal, writeExternal),
        )
        assertEquals(StorageAccessLevel.FULL, level)
    }

    @Test
    fun `api 26 sadece okuma izni sinirli moddur`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 26,
            isExternalStorageManager = false,
            grantedPermissions = setOf(readExternal),
        )
        assertEquals(StorageAccessLevel.LIMITED, level)
    }

    @Test
    fun `api 34 kismi gorsel erisimi de sinirli moddur`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 34,
            isExternalStorageManager = false,
            grantedPermissions = setOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
        )
        assertEquals(StorageAccessLevel.LIMITED, level)
    }

    @Test
    fun `kismi erisim izni api 33 te taninmaz`() {
        val level = StoragePermissions.resolveAccessLevel(
            sdkInt = 33,
            isExternalStorageManager = false,
            grantedPermissions = setOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
        )
        assertEquals(StorageAccessLevel.NONE, level)
    }

    @Test
    fun `istenecek izinler api seviyesine gore degisir`() {
        assertEquals(4, StoragePermissions.runtimePermissions(34).size)
        assertEquals(3, StoragePermissions.runtimePermissions(33).size)
        assertEquals(listOf(readExternal), StoragePermissions.runtimePermissions(30))
        assertEquals(listOf(readExternal, writeExternal), StoragePermissions.runtimePermissions(27))
    }

    @Test
    fun `manage external storage yalnizca api 30 ve ustunde gerekir`() {
        assertTrue(StoragePermissions.requiresManageExternalStorage(30))
        assertTrue(StoragePermissions.requiresManageExternalStorage(36))
        assertFalse(StoragePermissions.requiresManageExternalStorage(29))
    }

    @Test
    fun `erisim seviyesi yetenek bayraklari`() {
        assertTrue(StorageAccessLevel.FULL.canBrowseFileSystem)
        assertFalse(StorageAccessLevel.LIMITED.canBrowseFileSystem)
        assertTrue(StorageAccessLevel.LIMITED.canReadMedia)
        assertFalse(StorageAccessLevel.NONE.canReadMedia)
    }
}
