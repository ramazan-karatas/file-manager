package dev.rk.systemapps.files.data.di

import android.content.Context
import coil3.ImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.rk.systemapps.files.data.image.ApkIconFetcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                // Video karesi ve APK ikonu için özel bileşenler; ağ bileşeni bilinçli olarak yok.
                add(VideoFrameDecoder.Factory())
                add(ApkIconFetcher.Factory(context))
            }
            .crossfade(true)
            .build()
}
