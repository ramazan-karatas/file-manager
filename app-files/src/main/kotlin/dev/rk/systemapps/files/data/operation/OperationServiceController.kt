package dev.rk.systemapps.files.data.operation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rk.systemapps.files.service.FileOperationService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kuyruk dolduğunda foreground servisi ayağa kaldırır, boşaldığında durdurur.
 *
 * Arayüz olmasının sebebi [FileOperationManager]'ı Android `Context`'inden kurtarmak;
 * böylece kuyruk mantığı JVM testleriyle doğrulanabiliyor.
 */
interface OperationServiceController {
    fun start()
    fun stop()
}

@Singleton
class AndroidOperationServiceController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : OperationServiceController {

    override fun start() = FileOperationService.start(context)

    override fun stop() = FileOperationService.stop(context)
}
