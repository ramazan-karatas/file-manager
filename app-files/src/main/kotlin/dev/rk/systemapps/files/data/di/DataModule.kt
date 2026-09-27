package dev.rk.systemapps.files.data.di

import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.files.data.file.AndroidMediaMetadataReader
import dev.rk.systemapps.files.data.file.AndroidMimeTypeResolver
import dev.rk.systemapps.files.data.file.FileRepositoryImpl
import dev.rk.systemapps.files.data.file.LocalFileDataSource
import dev.rk.systemapps.files.data.operation.AndroidMediaScanner
import dev.rk.systemapps.files.data.operation.AndroidOperationServiceController
import dev.rk.systemapps.files.data.operation.FileOperationEngine
import dev.rk.systemapps.files.data.operation.MediaScanner
import dev.rk.systemapps.files.data.operation.OperationServiceController
import dev.rk.systemapps.files.domain.MediaMetadataReader
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
    fun provideMediaMetadataReader(): MediaMetadataReader = AndroidMediaMetadataReader()

    @Provides
    @Singleton
    fun provideLocalFileDataSource(
        resolver: MimeTypeResolver,
        mediaMetadataReader: MediaMetadataReader,
    ): LocalFileDataSource = LocalFileDataSource(resolver, mediaMetadataReader)

    @Provides
    @Singleton
    fun provideFileOperationEngine(): FileOperationEngine = FileOperationEngine()

    @Provides
    @Singleton
    fun provideMediaScanner(scanner: AndroidMediaScanner): MediaScanner = scanner

    @Provides
    @Singleton
    fun provideOperationServiceController(
        controller: AndroidOperationServiceController,
    ): OperationServiceController = controller

    @Provides
    @Singleton
    fun provideFileRepository(
        dataSource: LocalFileDataSource,
        dispatchers: DispatcherProvider,
    ): FileRepository = FileRepositoryImpl(dataSource, dispatchers)
}
