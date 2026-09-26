package dev.rk.systemapps.core.common.result

/**
 * Repository katmanı exception fırlatmaz, bu tipi döner (bkz. CLAUDE.md "Kod standartları").
 */
sealed interface Outcome<out T> {

    data class Success<out T>(val value: T) : Outcome<T>

    data class Failure(
        val throwable: Throwable? = null,
        val message: String? = null,
    ) : Outcome<Nothing>
}

inline fun <T> outcomeOf(block: () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: Exception) {
    Outcome.Failure(throwable = e, message = e.message)
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.value
