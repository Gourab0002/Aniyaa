package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.NYAA_CATEGORIES
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NyaaRepositoryTest {

    private fun makeTorrent(
        id: String = "1",
        infoHash: String = "hash$id"
    ) = Torrent(
        id = id,
        title = "Torrent $id",
        link = "https://nyaa.si/download/$id.torrent",
        guid = "https://nyaa.si/view/$id",
        pubDate = "Wed, 01 Jan 2025 00:00:00 -0000",
        seeders = 0,
        leechers = 0,
        downloads = 0,
        infoHash = infoHash,
        category = "Art",
        size = "1.0 MiB",
        comments = 0,
        trusted = false,
        remake = false,
        magnetLink = "magnet:?xt=urn:btih:$infoHash"
    )

    @Test
    fun mergeSearchPages_replaceDedupesAndStopsWhenShortPage() {
        val incoming = listOf(makeTorrent("1"), makeTorrent("1"), makeTorrent("2"))
        val (merged, canLoadMore) = mergeSearchPages(emptyList(), incoming, replace = true)
        assertEquals(listOf("1", "2"), merged.map { it.id })
        assertFalse(canLoadMore)
    }

    @Test
    fun mergeSearchPages_replaceAllowsMoreWhenFullPage() {
        val incoming = (1..NYAA_PAGE_SIZE).map { makeTorrent(it.toString()) }
        val (_, canLoadMore) = mergeSearchPages(emptyList(), incoming, replace = true)
        assertTrue(canLoadMore)
    }

    @Test
    fun mergeSearchPages_appendIgnoresDuplicatesAndStops() {
        val existing = listOf(makeTorrent("1"), makeTorrent("2"))
        val incoming = listOf(makeTorrent("2"), makeTorrent("1"))
        val (merged, canLoadMore) = mergeSearchPages(existing, incoming, replace = false)
        assertEquals(listOf("1", "2"), merged.map { it.id })
        assertFalse(canLoadMore)
    }

    @Test
    fun mergeSearchPages_appendAddsNewItems() {
        val existing = listOf(makeTorrent("1"))
        val incoming = (2..(NYAA_PAGE_SIZE + 1)).map { makeTorrent(it.toString()) }
        val (merged, canLoadMore) = mergeSearchPages(existing, incoming, replace = false)
        assertEquals(NYAA_PAGE_SIZE + 1, merged.size)
        assertTrue(canLoadMore)
    }

    @Test
    fun mergeSearchPages_emptyIncomingStopsPagination() {
        val existing = listOf(makeTorrent("1"))
        val (merged, canLoadMore) = mergeSearchPages(existing, emptyList(), replace = false)
        assertEquals(existing, merged)
        assertFalse(canLoadMore)
    }

    @Test
    fun buildSearchUrl_omitsPageForFirstPage() {
        val params = SearchParams(
            query = "test query",
            category = NYAA_CATEGORIES[1],
            filter = FilterOption.TRUSTED,
            sortField = SortField.SEEDERS,
            sortOrder = SortOrder.ASC,
            page = 1
        )

        assertEquals(
            "https://nyaa.si/?page=rss&q=test+query&c=1_0&f=2&s=seeders&o=asc",
            buildSearchUrl(params)
        )
    }

    @Test
    fun buildSearchUrl_includesPageForNextPages() {
        val params = SearchParams(
            query = "another test",
            category = NYAA_CATEGORIES[2],
            filter = FilterOption.NO_REMAKES,
            sortField = SortField.DOWNLOADS,
            sortOrder = SortOrder.DESC,
            page = 3
        )

        assertEquals(
            "https://nyaa.si/?page=rss&q=another+test&c=1_1&f=1&s=downloads&o=desc&p=3",
            buildSearchUrl(params)
        )
    }

    @Test
    fun buildSearchUrl_usesSukebeiHostForSukebeiSite() {
        val params = SearchParams(
            query = "test",
            site = CatalogSite.SUKEBEI,
            category = CatalogSite.SUKEBEI.categories[1]
        )
        assertEquals(
            "https://sukebei.nyaa.si/?page=rss&q=test&c=1_0&f=0&s=id&o=desc",
            buildSearchUrl(params)
        )
    }

    @Test
    fun buildSearchUrl_userQueryUsesUserPath() {
        val params = SearchParams(query = "user:alice extra")
        val url = buildSearchUrl(params, baseUrl = "https://sukebei.nyaa.si")
        assertTrue(url.startsWith("https://sukebei.nyaa.si/user/alice?"))
        assertTrue(url.contains("u=alice"))
        assertTrue(url.contains("q=extra"))
    }

    @Test
    fun buildSearchUrl_htmlModeOmitsRssPage() {
        val params = SearchParams(query = "foo")
        val url = buildSearchUrl(params, rss = false)
        assertFalse(url.contains("page=rss"))
        assertTrue(url.contains("q=foo"))
    }
}
