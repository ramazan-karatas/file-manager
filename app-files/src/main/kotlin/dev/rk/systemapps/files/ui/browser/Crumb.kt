package dev.rk.systemapps.files.ui.browser

/**
 * Breadcrumb'taki tek parça. [path] o parçaya kadar olan tam yoldur.
 */
data class Crumb(
    val name: String,
    val path: String,
    /** Kök parçası ekranda dosya adı yerine "Dahili depolama" olarak gösterilir. */
    val isStorageRoot: Boolean = false,
) {
    companion object {

        fun fromPath(path: String, storageRoot: String): List<Crumb> {
            if (path.isEmpty()) return emptyList()

            val crumbs = mutableListOf<Crumb>()
            val relative: String
            val base: String

            if (path == storageRoot || path.startsWith("$storageRoot/")) {
                // Kullanıcıya "/storage/emulated/0" göstermenin anlamı yok.
                crumbs += Crumb(name = storageRoot, path = storageRoot, isStorageRoot = true)
                base = storageRoot
                relative = path.removePrefix(storageRoot).trim('/')
            } else {
                crumbs += Crumb(name = "/", path = "/")
                base = ""
                relative = path.trim('/')
            }

            var current = base
            relative.split('/').filter { it.isNotEmpty() }.forEach { segment ->
                current = "$current/$segment"
                crumbs += Crumb(name = segment, path = current)
            }
            return crumbs
        }
    }
}
