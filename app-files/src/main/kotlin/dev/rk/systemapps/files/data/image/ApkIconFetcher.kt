package dev.rk.systemapps.files.data.image

import android.content.Context
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import java.io.IOException

/**
 * Coil'e "bu yoldaki APK'nın ikonunu getir" demek için kullanılan model.
 * Ayrı bir tip olması, fetcher'ın yalnızca APK'larda devreye girmesini sağlar.
 */
data class ApkIconRequest(val path: String)

/**
 * APK dosyasının kendi ikonunu çıkarır. Sistem dosya yöneticisinin yaptığı gibi
 * listede jenerik Android ikonu yerine uygulamanın gerçek ikonu görünür.
 */
class ApkIconFetcher(
    private val request: ApkIconRequest,
    private val context: Context,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(request.path, 0)
            ?: throw IOException("APK okunamadı: ${request.path}")

        // sourceDir doldurulmazsa loadIcon() kurulu olmayan paket için kaynak bulamaz.
        val applicationInfo = packageInfo.applicationInfo?.apply {
            sourceDir = request.path
            publicSourceDir = request.path
        } ?: throw IOException("APK uygulama bilgisi yok: ${request.path}")

        val icon = applicationInfo.loadIcon(packageManager)
        return ImageFetchResult(
            image = icon.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<ApkIconRequest> {
        override fun create(
            data: ApkIconRequest,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = ApkIconFetcher(data, context)
    }
}
