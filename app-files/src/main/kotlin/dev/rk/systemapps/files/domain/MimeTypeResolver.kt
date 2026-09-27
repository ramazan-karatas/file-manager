package dev.rk.systemapps.files.domain

/**
 * Uzantıdan MIME türü çözer. Arayüz olmasının sebebi `android.webkit.MimeTypeMap`in
 * JVM testlerinde çalışmaması; testler sahte bir uygulama kullanır.
 */
fun interface MimeTypeResolver {
    /** @param extension noktasız, küçük harfli uzantı. */
    fun resolve(extension: String): String?
}
