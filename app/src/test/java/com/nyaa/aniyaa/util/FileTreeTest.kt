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
        assertEquals("800.0 MiB", folder.children.first().size)
        assertEquals("200.0 MiB", tree.first { !it.isFolder }.size)
        assertEquals("800.0 MiB", folder.size)
    }

    @Test
    fun filterFileEntries_matchesPath() {
        val files = listOf(
            TorrentFileEntry("Show/Episode 01.mkv", "1 GiB"),
            TorrentFileEntry("Show/Episode 02.mkv", "1 GiB"),
            TorrentFileEntry("Show/NFO.txt", "12 KiB")
        )
        val filtered = filterFileEntries(files, "nfo")
        assertEquals(1, filtered.size)
        assertEquals("Show/NFO.txt", filtered.first().name)
        assertEquals(files, filterFileEntries(files, "  "))
    }

    @Test
    fun parseAndFormatSize() {
        assertEquals(1024L * 1024L, parseSizeBytes("1.0 MiB"))
        assertTrue(totalSizeLabel(listOf(TorrentFileEntry("a", "1.0 MiB"), TorrentFileEntry("b", "1.0 MiB"))).contains("2.0"))
    }
}
