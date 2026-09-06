package com.nyaa.aniyaa.util

import com.nyaa.aniyaa.data.model.TorrentFileEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileTreeTest {

    @Test
    fun buildFileTree_nestsFolders() {
        val tree = buildFileTree(
            listOf(
                TorrentFileEntry("folder/inner.mp4", "800.0 MiB"),
                TorrentFileEntry("root.txt", "200.0 MiB")
            )
        )
        assertEquals(2, tree.size)
        val folder = tree.first { it.isFolder }
        assertEquals("folder", folder.name)
        assertEquals(1, folder.children.size)
        assertEquals("inner.mp4", folder.children.first().name)
    }

    @Test
    fun parseAndFormatSize() {
        assertEquals(1024L * 1024L, parseSizeBytes("1.0 MiB"))
        assertTrue(totalSizeLabel(listOf(TorrentFileEntry("a", "1.0 MiB"), TorrentFileEntry("b", "1.0 MiB"))).contains("2.0"))
    }
}
