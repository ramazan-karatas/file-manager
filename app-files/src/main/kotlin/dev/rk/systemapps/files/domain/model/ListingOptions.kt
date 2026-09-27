package dev.rk.systemapps.files.domain.model

enum class SortBy {
    NAME,
    SIZE,
    DATE,
    TYPE,
}

/**
 * Bir klasörün nasıl listeleneceği. F-1.4'te kullanıcı tercihi olarak DataStore'a yazılacak;
 * ızgara/liste gibi yalnızca görünümü ilgilendiren alanlar buraya girmez, UI tarafında kalır.
 */
data class ListingOptions(
    val sortBy: SortBy = SortBy.NAME,
    val ascending: Boolean = true,
    val showHidden: Boolean = false,
    val foldersFirst: Boolean = true,
)
