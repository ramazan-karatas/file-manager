package dev.rk.systemapps.files.domain.model

import java.util.UUID

/**
 * Kuyruğa alınabilen bir dosya işlemi (docs/files/SPEC.md §5).
 *
 * Arşiv işlemleri (`Compress` / `Extract`) burada yok; uygulamalarıyla birlikte
 * F-2.4'te ekleneceler — boş dal bırakmanın faydası yok.
 */
sealed interface FileOperation {

    val id: String

    /** İşlemin dokunduğu kaynak öğeler. */
    val sources: List<String>

    data class Copy(
        override val sources: List<String>,
        val targetDirectory: String,
        override val id: String = newId(),
    ) : FileOperation

    data class Move(
        override val sources: List<String>,
        val targetDirectory: String,
        override val id: String = newId(),
    ) : FileOperation

    data class Delete(
        override val sources: List<String>,
        override val id: String = newId(),
    ) : FileOperation

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

enum class OperationState {
    /** Toplam boyut ve öğe sayısı hesaplanıyor. */
    PREPARING,
    RUNNING,
    DONE,

    /** İşlem baştan başarısız oldu (tek tek dosya hataları [OperationProgress.failures]'ta). */
    FAILED,

    /**
     * İptal edildi. Motor bu durumu **yayınlamaz** — iptal coroutine iptalidir ve
     * iptal edilmiş bir akışa değer gönderilemez. Bu durumu işlemi iptal eden taraf
     * (F-1.7'deki servis) kendi durumuna yazar.
     */
    CANCELLED,
}

data class OperationFailure(
    val path: String,
    val message: String?,
)

data class OperationProgress(
    val opId: String,
    val state: OperationState,
    val currentFile: String = "",
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val processedItems: Int = 0,
    val totalItems: Int = 0,
    /** Atlanan/başarısız olan öğeler; işlem bunlar yüzünden durmaz. */
    val failures: List<OperationFailure> = emptyList(),
) {
    /** 0f..1f; toplam bilinmiyorsa null (belirsiz ilerleme çubuğu). */
    val fraction: Float?
        get() = if (totalBytes > 0L) {
            (processedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
}

enum class ConflictResolution {
    OVERWRITE,
    SKIP,

    /** Hedefte `dosya (1).txt` gibi yeni bir ad üretilir. */
    KEEP_BOTH,

    /** Tüm işlem iptal edilir. */
    CANCEL,
}

data class Conflict(
    val sourcePath: String,
    val targetPath: String,
    val sourceIsDirectory: Boolean,
)

data class ConflictDecision(
    val resolution: ConflictResolution,
    /** "Hepsine uygula" işaretliyse sonraki çakışmalar sorulmadan aynı şekilde çözülür. */
    val applyToAll: Boolean = false,
)

/**
 * Çakışmayı kullanıcıya soran taraf. Motor UI bilmez; testlerde sabit karar veren
 * bir uygulama kullanılır.
 */
fun interface ConflictResolver {
    suspend fun resolve(conflict: Conflict): ConflictDecision
}
