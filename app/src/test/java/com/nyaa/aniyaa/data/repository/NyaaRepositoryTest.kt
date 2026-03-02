package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.model.CATEGORIES
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import org.junit.Assert.*
import org.junit.Test

class NyaaRepositoryTest {

    // region parseSizeToBytes

    @Test
    fun parseSizeToBytes_bytes() {
        assertEquals(512L, parseSizeToBytes("512 B"))
        assertEquals(512L, parseSizeToBytes("512 Bytes"))
    }

    @Test
    fun parseSizeToBytes_kib() {
        assertEquals(1024L, parseSizeToBytes("1.0 KiB"))
        assertEquals(512L, parseSizeToBytes("0.5 KiB"))
    }

    @Test
    fun parseSizeToBytes_mib() {
        assertEquals(1024L * 1024, parseSizeToBytes("1.0 MiB"))
        assertEquals(700 * 1024L * 1024, parseSizeToBytes("700.0 MiB"))
    }

    @Test
    fun parseSizeToBytes_gib() {
        assertEquals(1024L * 1024 * 1024, parseSizeToBytes("1.0 GiB"))
        assertEquals((1.5 * 1024 * 1024 * 1024).toLong(), parseSizeToBytes("1.5 GiB"))
    }

    @Test
    fun parseSizeToBytes_tib() {
        assertEquals(1024L * 1024 * 1024 * 1024, parseSizeToBytes("1.0 TiB"))
    }

    @Test
    fun parseSizeToBytes_invalid() {
        assertEquals(0L, parseSizeToBytes("unknown"))
        assertEquals(0L, parseSizeToBytes(""))
        assertEquals(0L, parseSizeToBytes("1.0 ZiB"))
    }

    // endregion

    // region sortTorrents helpers

    private fun makeTorrent(
        id: String = "1",
        seeders: Int = 0,
        leechers: Int = 0,
        downloads: Int = 0,
        comments: Int = 0,
        size: String = "1.0 MiB"
    ) = Torrent(
        id = id,
        title = "Torrent $id",
        link = "https://nyaa.si/download/$id.torrent",
        guid = "https://nyaa.si/view/$id",
        pubDate = "Wed, 01 Jan 2025 00:00:00 -0000",
        seeders = seeders,
        leechers = leechers,
        downloads = downloads,
        infoHash = "hash$id",
        category = "Anime",
        size = size,
        comments = comments,
        trusted = false,
        remake = false,
        magnetLink = "magnet:?xt=urn:btih:hash$id"
    )

    private fun params(field: SortField, order: SortOrder) =
        SearchParams(category = CATEGORIES[0], filter = FilterOption.ALL, sortField = field, sortOrder = order)

    // endregion

    // region sortTorrents

    @Test
    fun sortTorrents_byDate_desc() {
        val torrents = listOf(makeTorrent(id = "10"), makeTorrent(id = "30"), makeTorrent(id = "20"))
        val result = sortTorrents(torrents, params(SortField.DATE, SortOrder.DESC))
        assertEquals(listOf("30", "20", "10"), result.map { it.id })
    }

    @Test
    fun sortTorrents_byDate_asc() {
        val torrents = listOf(makeTorrent(id = "30"), makeTorrent(id = "10"), makeTorrent(id = "20"))
        val result = sortTorrents(torrents, params(SortField.DATE, SortOrder.ASC))
        assertEquals(listOf("10", "20", "30"), result.map { it.id })
    }

    @Test
    fun sortTorrents_bySeeders_desc() {
        val torrents = listOf(makeTorrent("1", seeders = 5), makeTorrent("2", seeders = 50), makeTorrent("3", seeders = 15))
        val result = sortTorrents(torrents, params(SortField.SEEDERS, SortOrder.DESC))
        assertEquals(listOf(50, 15, 5), result.map { it.seeders })
    }

    @Test
    fun sortTorrents_bySeeders_asc() {
        val torrents = listOf(makeTorrent("1", seeders = 50), makeTorrent("2", seeders = 5), makeTorrent("3", seeders = 15))
        val result = sortTorrents(torrents, params(SortField.SEEDERS, SortOrder.ASC))
        assertEquals(listOf(5, 15, 50), result.map { it.seeders })
    }

    @Test
    fun sortTorrents_byLeechers_desc() {
        val torrents = listOf(makeTorrent("1", leechers = 3), makeTorrent("2", leechers = 30), makeTorrent("3", leechers = 10))
        val result = sortTorrents(torrents, params(SortField.LEECHERS, SortOrder.DESC))
        assertEquals(listOf(30, 10, 3), result.map { it.leechers })
    }

    @Test
    fun sortTorrents_byDownloads_desc() {
        val torrents = listOf(makeTorrent("1", downloads = 100), makeTorrent("2", downloads = 500), makeTorrent("3", downloads = 250))
        val result = sortTorrents(torrents, params(SortField.DOWNLOADS, SortOrder.DESC))
        assertEquals(listOf(500, 250, 100), result.map { it.downloads })
    }

    @Test
    fun sortTorrents_byComments_asc() {
        val torrents = listOf(makeTorrent("1", comments = 7), makeTorrent("2", comments = 1), makeTorrent("3", comments = 4))
        val result = sortTorrents(torrents, params(SortField.COMMENTS, SortOrder.ASC))
        assertEquals(listOf(1, 4, 7), result.map { it.comments })
    }

    @Test
    fun sortTorrents_bySize_desc() {
        val torrents = listOf(
            makeTorrent("1", size = "500.0 MiB"),
            makeTorrent("2", size = "1.5 GiB"),
            makeTorrent("3", size = "200.0 MiB")
        )
        val result = sortTorrents(torrents, params(SortField.SIZE, SortOrder.DESC))
        assertEquals(listOf("2", "1", "3"), result.map { it.id })
    }

    @Test
    fun sortTorrents_bySize_asc() {
        val torrents = listOf(
            makeTorrent("1", size = "1.5 GiB"),
            makeTorrent("2", size = "200.0 MiB"),
            makeTorrent("3", size = "500.0 MiB")
        )
        val result = sortTorrents(torrents, params(SortField.SIZE, SortOrder.ASC))
        assertEquals(listOf("2", "3", "1"), result.map { it.id })
    }

    @Test
    fun sortTorrents_emptyList() {
        val result = sortTorrents(emptyList(), params(SortField.SEEDERS, SortOrder.DESC))
        assertTrue(result.isEmpty())
    }

    // endregion
}
