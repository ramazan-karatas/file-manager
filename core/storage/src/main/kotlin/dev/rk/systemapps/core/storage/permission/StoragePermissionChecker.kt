package dev.rk.systemapps.core.storage.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Depolama erişim durumunu okur ve akış olarak yayar.
 *
 * İzin durumu uygulama dışında (Ayarlar'da) değişebildiği için kendiliğinden
 * güncellenmez: ekran öne geldiğinde [refresh] çağrılmalıdır.
 */
interface StoragePermissionChecker {
    val accessLevel: StateFlow<StorageAccessLevel>
    fun refresh(): StorageAccessLevel
    fun runtimePermissions(): List<String>
    fun requiresManageExternalStorage(): Boolean
}

internal class AndroidStoragePermissionChecker(
    private val context: Context,
) : StoragePermissionChecker {

    private val _accessLevel = MutableStateFlow(readAccessLevel())
    override val accessLevel: StateFlow<StorageAccessLevel> = _accessLevel.asStateFlow()

    override fun refresh(): StorageAccessLevel = readAccessLevel().also { _accessLevel.value = it }

    override fun runtimePermissions(): List<String> = StoragePermissions.runtimePermissions()

    override fun requiresManageExternalStorage(): Boolean =
        StoragePermissions.requiresManageExternalStorage()

    private fun readAccessLevel(): StorageAccessLevel = StoragePermissions.resolveAccessLevel(
        sdkInt = Build.VERSION.SDK_INT,
        isExternalStorageManager = isExternalStorageManager(),
        grantedPermissions = grantedPermissions(),
    )

    private fun isExternalStorageManager(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    private fun grantedPermissions(): Set<String> = StoragePermissions.runtimePermissions()
        .filterTo(mutableSetOf()) { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
}

@Module
@InstallIn(SingletonComponent::class)
object StoragePermissionModule {

    @Provides
    @Singleton
    fun provideStoragePermissionChecker(
        @ApplicationContext context: Context,
    ): StoragePermissionChecker = AndroidStoragePermissionChecker(context)
}
