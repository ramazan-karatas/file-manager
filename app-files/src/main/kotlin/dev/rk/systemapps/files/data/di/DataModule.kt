package dev.rk.systemapps.files.data.di

import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.files.data.file.AndroidMimeTypeResolver
import dev.rk.systemapps.files.data.file.FileRepositoryImpl
import dev.rk.systemapps.files.data.file.LocalFileDataSource
import dev.rk.systemapps.files.domain.MimeTypeResolver
import dev.rk.systemapps.files.domain.repository.FileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideMimeTypeResolver(): MimeTypeResolver = AndroidMimeTypeResolver()

    @Provides
    @Singleton
    fun provideLocalFileDataSource(resolver: MimeTypeResolver): LocalFileDataSource =
        LocalFileDataSource(resolver)

    @Provides
    @Singleton
    fun provideFileRepository(
        dataSource: LocalFileDataSource,
        dispatchers: DispatcherProvider,
    ): FileRepository = FileRepositoryImpl(dataSource, dispatchers)
}
