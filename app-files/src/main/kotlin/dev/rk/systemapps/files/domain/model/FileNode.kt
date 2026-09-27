package dev.rk.systemapps.files.domain.model

/**
 * Gezginde listelenen tek bir öğe.
 *
 * UI katmanı `java.io.File` bilmez, yalnızca bu tipi bilir; böylece dahili depolama
 * (doğrudan File) ve SD kart / `Android/data` (SAF) aynı ekranlarla çalışır
 * (docs/files/SPEC.md §4).
 *
 * Tip bilinçli olarak Android framework'ünden bağımsızdır: `android.net.Uri` yerine
 * [id] metni taşır, böylece sıralama ve filtreleme mantığı Robolectric'siz test edilebilir.
 * Uri'ye ihtiyaç duyan yerler (paylaşma, açma) dönüşümü veri katmanında yapar.
 */
sealed interface FileNode {

    /** Kimlik: yerel dosyada tam yol, SAF düğümünde document URI'sinin metni. */
    val id: String

    val name: String
    val isDirectory: Boolean

    /** Bayt cinsinden boyut; klasörlerde [SIZE_UNKNOWN] (hesaplanması pahalıdır). */
    val size: Long

    val lastModified: Long
    val mimeType: String?

    /** Android'de gizlilik ölçütü dosya adının nokta ile başlamasıdır. */
    val isHidden: Boolean

    /** Uzantı, noktasız ve küçük harfli; yoksa boş metin. */
    val extension: String
        get() = if (isDirectory) {
            ""
        } else {
            name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        }

    companion object {
        const val SIZE_UNKNOWN = -1L
    }
}

/** Doğrudan dosya sistemi üzerinden erişilen düğüm (MANAGE_EXTERNAL_STORAGE yolu). */
data class LocalFileNode(
    override val id: String,
    override val name: String,
    override val isDirectory: Boolean,
    override val size: Long,
    override val lastModified: Long,
    override val mimeType: String?,
    override val isHidden: Boolean,
) : FileNode {
    /** [id] zaten tam yoldur; okunabilirlik için takma ad. */
    val path: String get() = id
}

/**
 * SAF (Storage Access Framework) üzerinden erişilen düğüm.
 *
 * F-2.7'de SD kart ve `Android/data` erişimi bu tiple gelecek. Şimdiden tanımlı olmasının
 * sebebi, "tek UI - iki depolama arka ucu" kuralının sealed hiyerarşide görünür olması.
 */
data class DocumentNode(
    override val id: String,
    override val name: String,
    override val isDirectory: Boolean,
    override val size: Long,
    override val lastModified: Long,
    override val mimeType: String?,
    override val isHidden: Boolean,
) : FileNode {
    /** [id] document URI'sinin metin hâlidir. */
    val documentUri: String get() = id
}
