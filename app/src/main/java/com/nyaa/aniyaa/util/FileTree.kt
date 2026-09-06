package com.nyaa.aniyaa.util

import com.nyaa.aniyaa.data.model.TorrentFileEntry
import java.util.Locale
import kotlin.math.roundToLong

data class FileNode(
    val name: String,
    val path: String,
    val size: String,
    val isFolder: Boolean,
    val children: List<FileNode> = emptyList()
)

fun filterFileEntries(files: List<TorrentFileEntry>, query: String): List<TorrentFileEntry> {
    val needle = query.trim()
    if (needle.isEmpty()) return files
    return files.filter { it.name.contains(needle, ignoreCase = true) }
}

fun buildFileTree(files: List<TorrentFileEntry>): List<FileNode> {
    if (files.isEmpty()) return emptyList()
    val root = mutableMapOf<String, MutableNode>()
    for (file in files) {
        val parts = file.name.split('/').filter { it.isNotBlank() }
        if (parts.isEmpty()) continue
        var level = root
        var path = ""
        parts.forEachIndexed { index, part ->
            path = if (path.isEmpty()) part else "$path/$part"
            val isFile = index == parts.lastIndex
            val node = level.getOrPut(part) {
                MutableNode(name = part, path = path, size = if (isFile) file.size else "", isFolder = !isFile)
            }
            if (isFile) {
                node.size = file.size
                node.isFolder = false
            } else {
                node.isFolder = true
                level = node.children
            }
        }
    }
    return root.values.map { it.toFileNode() }
}

fun totalSizeLabel(files: List<TorrentFileEntry>): String {
    val totalBytes = files.mapNotNull { parseSizeBytes(it.size) }.sum()
    if (totalBytes <= 0L) return ""
    return formatSizeBytes(totalBytes)
}

fun parseSizeBytes(raw: String): Long? {
    val match = SIZE_REGEX.find(raw.trim()) ?: return null
    val amount = match.groupValues[1].toDoubleOrNull() ?: return null
    val unit = match.groupValues[2].uppercase(Locale.US)
    val multiplier = when (unit) {
        "B" -> 1.0
        "KB", "KIB" -> 1024.0
        "MB", "MIB" -> 1024.0 * 1024.0
        "GB", "GIB" -> 1024.0 * 1024.0 * 1024.0
        "TB", "TIB" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
        else -> return null
    }
    return (amount * multiplier).roundToLong()
}

fun formatSizeBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024) return String.format(Locale.US, "%.1f KiB", kib)
    val mib = kib / 1024.0
    if (mib < 1024) return String.format(Locale.US, "%.1f MiB", mib)
    val gib = mib / 1024.0
    if (gib < 1024) return String.format(Locale.US, "%.2f GiB", gib)
    return String.format(Locale.US, "%.2f TiB", gib / 1024.0)
}

private val SIZE_REGEX = Regex("""([\d.]+)\s*([A-Za-z]+)""")

private class MutableNode(
    val name: String,
    val path: String,
    var size: String,
    var isFolder: Boolean,
    val children: MutableMap<String, MutableNode> = mutableMapOf()
) {
    fun toFileNode(): FileNode = FileNode(
        name = name,
        path = path,
        size = if (isFolder) {
            val total = flattenFiles().mapNotNull { parseSizeBytes(it.size) }.sum()
            if (total > 0L) formatSizeBytes(total) else ""
        } else {
            size
        },
        isFolder = isFolder,
        children = children.values.map { it.toFileNode() }.sortedWith(
            compareByDescending<FileNode> { it.isFolder }.thenBy { it.name.lowercase(Locale.US) }
        )
    )

    private fun flattenFiles(): List<MutableNode> {
        if (!isFolder) return listOf(this)
        return children.values.flatMap { it.flattenFiles() }
    }
}
