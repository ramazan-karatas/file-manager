package dev.rk.systemapps.core.storage.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * "Tüm dosyalara erişim" ekranını açmak için denenecek intent'ler, öncelik sırasıyla.
 *
 * MIUI/HyperOS'ta uygulamaya özel ekran (ilk aday) bazı sürümlerde açılmıyor;
 * bu yüzden genel listeye ve son çare olarak uygulama detay ekranına düşülür
 * (docs/files/SPEC.md §8 "Bilinen riskler").
 */
fun manageAllFilesIntents(packageName: String): List<Intent> {
    val appDetails = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return listOf(appDetails)

    return listOf(
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.fromParts("package", packageName, null),
        ),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        appDetails,
    )
}

/**
 * Adayları sırayla dener; hiçbiri açılamazsa `false` döner ve çağıran tarafın
 * kullanıcıya açıklama göstermesi beklenir. Asla exception fırlatmaz.
 */
fun Context.launchManageAllFilesSettings(): Boolean {
    for (intent in manageAllFilesIntents(packageName)) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (_: Exception) {
            // Sıradaki adaya geç.
        }
    }
    return false
}
