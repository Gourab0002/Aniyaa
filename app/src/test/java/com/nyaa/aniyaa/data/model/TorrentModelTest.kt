package com.nyaa.aniyaa.data.model

import org.junit.Test
import org.junit.Assert.*

class TorrentModelTest {

    @Test
    fun torrent_creation() {
        val torrent = Torrent(
            id = "123",
            title = "Test Torrent",
            link = "https://nyaa.si/download/123.torrent",
            guid = "https://nyaa.si/view/123",
            pubDate = "Wed, 01 Jan 2025 00:00:00 -0000",
            seeders = 10,
            leechers = 5,
            downloads = 100,
            infoHash = "abc123",
            category = "Anime - English",
            size = "1.5 GiB",
            comments = 3,
            trusted = true,
            remake = false,
            magnetLink = "magnet:?xt=urn:btih:abc123"
        )
        assertEquals("123", torrent.id)
        assertEquals("Test Torrent", torrent.title)
        assertEquals(10, torrent.seeders)
        assertEquals(5, torrent.leechers)
        assertEquals(100, torrent.downloads)
        assertTrue(torrent.trusted)
        assertFalse(torrent.remake)
    }

    @Test
    fun sortField_values() {
        assertEquals("id", SortField.DATE.value)
        assertEquals("seeders", SortField.SEEDERS.value)
        assertEquals("leechers", SortField.LEECHERS.value)
        assertEquals("size", SortField.SIZE.value)
        assertEquals("downloads", SortField.DOWNLOADS.value)
        assertEquals("comments", SortField.COMMENTS.value)
    }

    @Test
    fun sortOrder_values() {
        assertEquals("desc", SortOrder.DESC.value)
        assertEquals("asc", SortOrder.ASC.value)
    }

    @Test
    fun filterOption_values() {
        assertEquals(0, FilterOption.ALL.value)
        assertEquals(1, FilterOption.NO_REMAKES.value)
        assertEquals(2, FilterOption.TRUSTED.value)
    }

    @Test
    fun categories_list_isNotEmpty() {
        assertTrue(CATEGORIES.isNotEmpty())
        assertEquals("0_0", CATEGORIES[0].value)
        assertEquals("All Categories", CATEGORIES[0].displayName)
    }

    @Test
    fun searchParams_defaults() {
        val params = SearchParams()
        assertEquals("", params.query)
        assertEquals(CATEGORIES[0], params.category)
        assertEquals(FilterOption.ALL, params.filter)
        assertEquals(SortField.DATE, params.sortField)
        assertEquals(SortOrder.DESC, params.sortOrder)
        assertEquals(1, params.page)
    }

    @Test
    fun searchParams_customValues() {
        val params = SearchParams(
            query = "naruto",
            category = CATEGORIES[1],
            filter = FilterOption.TRUSTED,
            sortField = SortField.SEEDERS,
            sortOrder = SortOrder.ASC,
            page = 2
        )
        assertEquals("naruto", params.query)
        assertEquals("1_0", params.category.value)
        assertEquals("Anime", params.category.displayName)
        assertEquals(FilterOption.TRUSTED, params.filter)
        assertEquals(SortField.SEEDERS, params.sortField)
        assertEquals(SortOrder.ASC, params.sortOrder)
        assertEquals(2, params.page)
    }
}
