package dev.rk.systemapps.files.util

import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.LocalFileNode

fun fileNode(
    name: String,
    isDirectory: Boolean = false,
    size: Long = 0L,
    lastModified: Long = 0L,
    parent: String = "/storage/emulated/0",
): LocalFileNode = LocalFileNode(
    id = "$parent/$name",
    name = name,
    isDirectory = isDirectory,
    size = if (isDirectory) FileNode.SIZE_UNKNOWN else size,
    lastModified = lastModified,
    mimeType = null,
    isHidden = name.startsWith("."),
)

fun dirNode(name: String, parent: String = "/storage/emulated/0"): LocalFileNode =
    fileNode(name = name, isDirectory = true, parent = parent)

fun List<FileNode>.names(): List<String> = map { it.name }
