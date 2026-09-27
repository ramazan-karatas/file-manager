package dev.rk.systemapps.files.ui

import android.net.Uri

/**
 * Rota sabitleri. Argümanlı rotalara geçilirken tip güvenli
 * (kotlinx.serialization) sürüme taşınacak.
 */
object Routes {
    const val PERMISSION = "permission"
    const val HOME = "home"

    const val ARG_PATH = "path"
    const val BROWSER = "browser/{$ARG_PATH}"

    /** Dosya yolları `/` içerdiği için rotaya kodlanarak konur. */
    fun browser(path: String): String = "browser/${Uri.encode(path)}"

    const val SEARCH = "search/{$ARG_PATH}"

    fun search(path: String): String = "search/${Uri.encode(path)}"

    const val ARG_CATEGORY = "category"
    const val CATEGORY = "category/{$ARG_CATEGORY}"

    fun category(name: String): String = "category/$name"

    const val ABOUT = "about"
}
