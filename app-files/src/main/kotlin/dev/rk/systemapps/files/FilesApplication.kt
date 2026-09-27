package dev.rk.systemapps.files

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class FilesApplication : Application(), SingletonImageLoader.Factory {

    /**
     * Provider ile enjekte ediliyor: ImageLoader ilk görsel istendiğinde kurulsun,
     * uygulama açılışını yavaşlatmasın.
     */
    @Inject
    lateinit var imageLoader: Provider<ImageLoader>

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()
}
