package dev.rk.systemapps.files.data.operation

import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.files.domain.model.Conflict
import dev.rk.systemapps.files.domain.model.ConflictDecision
import dev.rk.systemapps.files.domain.model.ConflictResolver
import dev.rk.systemapps.files.domain.model.FileOperation
import dev.rk.systemapps.files.domain.model.OperationProgress
import dev.rk.systemapps.files.domain.model.OperationState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperationQueueState(
    val current: OperationProgress? = null,
    /** Sırada bekleyen (henüz başlamamış) işlem sayısı. */
    val queued: Int = 0,
) {
    val isBusy: Boolean get() = current != null
}

/**
 * Dosya işlemlerinin kuyruğu ve tek doğruluk kaynağı.
 *
 * Servis bu sınıfı *barındırır* (uygulama arka plandayken süreç öldürülmesin diye),
 * UI ise doğrudan buraya bağlanır. Böylece çakışma diyaloğu ve ilerleme paneli
 * servisle Intent trafiğine girmeden çalışır.
 */
@Singleton
class FileOperationManager @Inject constructor(
    private val serviceController: OperationServiceController,
    private val engine: FileOperationEngine,
    private val mediaScanner: MediaScanner,
    dispatchers: DispatcherProvider,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val pending = ArrayDeque<FileOperation>()

    private val _state = MutableStateFlow(OperationQueueState())
    val state: StateFlow<OperationQueueState> = _state.asStateFlow()

    private val _pendingConflict = MutableStateFlow<Conflict?>(null)
    val pendingConflict: StateFlow<Conflict?> = _pendingConflict.asStateFlow()

    /** Biten her işlemin son durumu — gezgin listesini tazelemek ve özet göstermek için. */
    private val _completions = MutableSharedFlow<OperationProgress>(extraBufferCapacity = 8)
    val completions: SharedFlow<OperationProgress> = _completions.asSharedFlow()

    private var conflictAnswer: CompletableDeferred<ConflictDecision>? = null
    private var worker: Job? = null
    private var currentJob: Job? = null

    private val resolver = ConflictResolver { conflict ->
        val answer = CompletableDeferred<ConflictDecision>()
        conflictAnswer = answer
        _pendingConflict.value = conflict
        try {
            answer.await()
        } finally {
            _pendingConflict.value = null
            conflictAnswer = null
        }
    }

    fun enqueue(operation: FileOperation) {
        synchronized(pending) { pending.addLast(operation) }
        _state.update { it.copy(queued = pendingCount()) }
        serviceController.start()

        if (worker?.isActive != true) {
            worker = scope.launch { drain() }
        }
    }

    fun resolveConflict(decision: ConflictDecision) {
        conflictAnswer?.complete(decision)
    }

    /** Yalnızca yürüyen işlemi iptal eder; kuyruktakiler devam eder. */
    fun cancelCurrent() {
        currentJob?.cancel()
    }

    fun cancelAll() {
        synchronized(pending) { pending.clear() }
        currentJob?.cancel()
    }

    private suspend fun drain() {
        while (true) {
            val operation = synchronized(pending) { pending.removeFirstOrNull() } ?: break

            var last: OperationProgress? = null
            val job = scope.launch {
                engine.execute(operation, resolver).collect { progress ->
                    last = progress
                    _state.value = OperationQueueState(
                        current = progress,
                        queued = pendingCount(),
                    )
                }
            }
            currentJob = job
            job.join()
            currentJob = null

            val outcome = if (job.isCancelled) {
                (last ?: emptyProgress(operation)).copy(state = OperationState.CANCELLED)
            } else {
                last ?: emptyProgress(operation).copy(state = OperationState.DONE)
            }

            // Galeri ve müzik uygulamaları güncel kalsın (SPEC §5.8).
            mediaScanner.scan(operation.scanTargets())

            _completions.emit(outcome)
            _state.value = OperationQueueState(queued = pendingCount())
        }

        serviceController.stop()
    }

    private fun pendingCount(): Int = synchronized(pending) { pending.size }

    private fun emptyProgress(operation: FileOperation) = OperationProgress(
        opId = operation.id,
        state = OperationState.DONE,
    )

    private fun FileOperation.scanTargets(): List<String> = when (this) {
        is FileOperation.Copy -> sources + targetDirectory
        is FileOperation.Move -> sources + targetDirectory
        is FileOperation.Delete -> sources
    }
}
