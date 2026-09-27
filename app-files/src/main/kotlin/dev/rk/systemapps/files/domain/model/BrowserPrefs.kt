package dev.rk.systemapps.files.domain.model

/**
 * Gezgin tercihleri. Kalıcıdır (DataStore) ve tüm klasörler için ortaktır —
 * MIUI'de olduğu gibi klasör başına ayrı tercih tutulmuyor.
 */
data class BrowserPrefs(
    val sortBy: SortBy = SortBy.NAME,
    val ascending: Boolean = true,
    val gridMode: Boolean = false,
    val showHidden: Boolean = false,
    val foldersFirst: Boolean = true,
) {
    /** Repository'nin ilgilendiği alt küme; [gridMode] yalnızca görünümü ilgilendirir. */
    val listingOptions: ListingOptions
        get() = ListingOptions(
            sortBy = sortBy,
            ascending = ascending,
            showHidden = showHidden,
            foldersFirst = foldersFirst,
        )
}
