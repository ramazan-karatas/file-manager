package dev.rk.systemapps.files.data.operation

import dev.rk.systemapps.files.domain.model.Conflict
import dev.rk.systemapps.files.domain.model.ConflictDecision
import dev.rk.systemapps.files.domain.model.ConflictResolution
import dev.rk.systemapps.files.domain.model.ConflictResolver
import dev.rk.systemapps.files.domain.model.FileOperation
import dev.rk.systemapps.files.domain.model.OperationFailure
import dev.rk.systemapps.files.domain.model.OperationProgress
import dev.rk.systemapps.files.domain.model.OperationState
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/**
 * Kopyalama, taşıma ve silmeyi yürüten çekirdek (docs/files/SPEC.md §5).
 *
 * UI, servis ve bildirim bilmez; yalnızca [OperationProgress] yayar. Bu sayede
 * çakışma çözümü, iptal ve kısmi hata davranışı gerçek dosyalar üzerinde
 * JVM testleriyle doğrulanabiliyor.
 *
 * İptal coroutine iptalidir: toplayıcı akışı iptal ettiğinde yarım kalan hedef
 * dosya silinir ve [CancellationException] yeniden fırlatılır.
 */
class FileOperationEngine(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val progressIntervalMs: Long = DEFAULT_PROGRESS_INTERVAL_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {

    fun execute(
        operation: FileOperation,
        resolver: ConflictResolver = ConflictResolver { error("Çakışma çözücü verilmedi") },
    ): Flow<OperationProgress> = flow {
        val run = Run(operation.id)

        emit(run.snapshot(OperationState.PREPARING))
        val sources = operation.sources.map(::File)
        measure(sources, run)
        emit(run.snapshot(OperationState.RUNNING))

        try {
            when (operation) {
                is FileOperation.Copy ->
                    transfer(sources, File(operation.targetDirectory), run, resolver, move = false)

                is FileOperation.Move ->
                    transfer(sources, File(operation.targetDirectory), run, resolver, move = true)

                is FileOperation.Delete -> sources.forEach { source ->
                    deleteTree(source, run)
                }
            }
        } catch (cancellation: CancellationException) {
            // Yarım yazılmış hedef dosya bırakılmaz.
            run.partialTarget?.delete()
            throw cancellation
        } catch (error: Exception) {
            // İşlemin tamamını engelleyen hata (ör. hedef klasör açılamadı).
            run.partialTarget?.delete()
            run.failures += OperationFailure(operation.sources.firstOrNull().orEmpty(), error.message)
            emit(run.snapshot(OperationState.FAILED))
            return@flow
        }

        // Kullanıcı çakışma diyaloğunda "İptal" dediyse bu durum yayınlanabilir;
        // coroutine iptalinden farklı olarak akış hâlâ canlıdır.
        emit(
            run.snapshot(
                if (run.cancelledByUser) OperationState.CANCELLED else OperationState.DONE,
            ),
        )
    }

    // --- ölçüm -------------------------------------------------------------

    private fun measure(sources: List<File>, run: Run) {
        sources.forEach { source -> measureTree(source, run) }
    }

    private fun measureTree(file: File, run: Run) {
        if (!file.exists()) return
        run.totalItems++
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> measureTree(child, run) }
        } else {
            run.totalBytes += file.length()
        }
    }

    // --- kopyala / taşı ----------------------------------------------------

    private suspend fun FlowCollector<OperationProgress>.transfer(
        sources: List<File>,
        targetDirectory: File,
        run: Run,
        resolver: ConflictResolver,
        move: Boolean,
    ) {
        // Hedef klasör değilse hiçbir öğe kopyalanamaz; tek tek hata biriktirmek yerine
        // işlemin tamamı başarısız sayılır.
        if (targetDirectory.exists() && !targetDirectory.isDirectory) {
            throw IOException("Hedef bir klasör değil: ${targetDirectory.path}")
        }
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw IOException("Hedef klasör oluşturulamadı: ${targetDirectory.path}")
        }

        for (source in sources) {
            if (run.cancelledByUser) return
            transferTree(source, targetDirectory, run, resolver, move)
        }
    }

    private suspend fun FlowCollector<OperationProgress>.transferTree(
        source: File,
        targetDirectory: File,
        run: Run,
        resolver: ConflictResolver,
        move: Boolean,
    ) {
        currentCoroutineContext().ensureActive()
        if (run.cancelledByUser) return

        if (!source.exists()) {
            run.fail(source, "Kaynak bulunamadı")
            return
        }

        // Aynı birimdeyse taşıma anlık bir yeniden adlandırmadır; ağacı dolaşmaya gerek yok.
        if (move && !source.isDirectory) {
            val quickTarget = File(targetDirectory, source.name)
            if (!quickTarget.exists() && source.renameTo(quickTarget)) {
                run.completeItem(source.length())
                emitThrottled(run, source.path)
                return
            }
        }

        val target = resolveTarget(source, targetDirectory, run, resolver) ?: return

        if (source.isDirectory) {
            if (!target.exists() && !target.mkdirs()) {
                run.fail(source, "Klasör oluşturulamadı")
                return
            }
            run.completeItem(bytes = 0L)
            emitThrottled(run, source.path)

            source.listFiles()?.forEach { child ->
                transferTree(child, target, run, resolver, move)
            }
            if (move && !run.cancelledByUser) {
                // İçerik taşındıktan sonra boş kalan kaynak klasör kaldırılır.
                source.delete()
            }
        } else {
            copyFile(source, target, run)
            if (move && !run.cancelledByUser && !source.delete()) {
                run.fail(source, "Kaynak silinemedi")
            }
        }
    }

    /**
     * Hedef yolu belirler; çakışma varsa [resolver]'a sorar.
     * `null` dönerse bu öğe atlanmıştır.
     */
    private suspend fun resolveTarget(
        source: File,
        targetDirectory: File,
        run: Run,
        resolver: ConflictResolver,
    ): File? {
        val candidate = File(targetDirectory, source.name)
        if (!candidate.exists()) return candidate

        // İki taraf da klasörse çakışma yok, içerik birleştirilir.
        if (source.isDirectory && candidate.isDirectory) return candidate

        val decision = run.blanketResolution?.let { ConflictDecision(it, applyToAll = true) }
            ?: resolver.resolve(
                Conflict(
                    sourcePath = source.path,
                    targetPath = candidate.path,
                    sourceIsDirectory = source.isDirectory,
                ),
            ).also { decision ->
                if (decision.applyToAll) run.blanketResolution = decision.resolution
            }

        return when (decision.resolution) {
            ConflictResolution.OVERWRITE -> candidate
            ConflictResolution.KEEP_BOTH -> nextAvailableName(targetDirectory, source.name)
            ConflictResolution.SKIP -> {
                run.skipTree(source)
                null
            }

            ConflictResolution.CANCEL -> {
                run.cancelledByUser = true
                null
            }
        }
    }

    private suspend fun FlowCollector<OperationProgress>.copyFile(
        source: File,
        target: File,
        run: Run,
    ) {
        try {
            if (target.isDirectory) {
                run.fail(source, "Hedefte aynı adlı bir klasör var")
                return
            }
            run.partialTarget = target

            source.inputStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(bufferSize)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        run.processedBytes += read
                        emitThrottled(run, source.path)
                    }
                }
            }
            target.setLastModified(source.lastModified())
            run.partialTarget = null
            run.completeItem(bytes = 0L)
            emitThrottled(run, source.path)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // Bir dosyanın hatası işlemi durdurmaz (SPEC §5.6).
            run.partialTarget?.delete()
            run.partialTarget = null
            run.fail(source, error.message)
        }
    }

    // --- sil ---------------------------------------------------------------

    private suspend fun FlowCollector<OperationProgress>.deleteTree(file: File, run: Run) {
        currentCoroutineContext().ensureActive()
        if (!file.exists()) {
            run.fail(file, "Bulunamadı")
            return
        }

        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> deleteTree(child, run) }
        }

        val bytes = if (file.isDirectory) 0L else file.length()
        if (file.delete()) {
            run.completeItem(bytes)
        } else {
            run.fail(file, "Silinemedi")
        }
        emitThrottled(run, file.path)
    }

    // --- ilerleme ----------------------------------------------------------

    /** UI'yı boğmamak için ilerleme en çok [progressIntervalMs]'de bir yayınlanır. */
    private suspend fun FlowCollector<OperationProgress>.emitThrottled(run: Run, currentFile: String) {
        val timestamp = now()
        if (timestamp - run.lastEmitAt < progressIntervalMs) return
        run.lastEmitAt = timestamp
        emit(run.snapshot(OperationState.RUNNING, currentFile))
    }

    private class Run(val opId: String) {
        var totalBytes = 0L
        var totalItems = 0
        var processedBytes = 0L
        var processedItems = 0
        var lastEmitAt = 0L
        var blanketResolution: ConflictResolution? = null
        var cancelledByUser = false

        /** Yazılmakta olan hedef dosya; iptalde silinir. */
        var partialTarget: File? = null

        val failures = mutableListOf<OperationFailure>()

        fun completeItem(bytes: Long) {
            processedItems++
            processedBytes += bytes
        }

        fun fail(file: File, message: String?) {
            failures += OperationFailure(file.path, message)
            processedItems++
        }

        /** Atlanan klasörün tüm alt ağacı sayaçtan düşülür ki ilerleme yerinde saymasın. */
        fun skipTree(file: File) {
            processedItems++
            if (file.isDirectory) {
                file.listFiles()?.forEach { child -> skipTree(child) }
            } else {
                processedBytes += file.length()
            }
        }

        fun snapshot(state: OperationState, currentFile: String = "") = OperationProgress(
            opId = opId,
            state = state,
            currentFile = currentFile,
            processedBytes = processedBytes,
            totalBytes = totalBytes,
            processedItems = processedItems,
            totalItems = totalItems,
            failures = failures.toList(),
        )
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
        const val DEFAULT_PROGRESS_INTERVAL_MS = 200L
    }
}

/** Hedef klasörde çakışmayan ilk adı üretir: `dosya.txt` → `dosya (1).txt`. */
internal fun nextAvailableName(directory: File, name: String): File {
    val baseName = name.substringBeforeLast('.', name)
    val extension = name.substringAfterLast('.', "")
    val suffix = if (extension.isEmpty()) "" else ".$extension"

    var counter = 1
    while (true) {
        val candidate = File(directory, "$baseName ($counter)$suffix")
        if (!candidate.exists()) return candidate
        counter++
    }
}
