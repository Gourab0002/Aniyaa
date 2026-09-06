package com.nyaa.aniyaa.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSiteTest {

    @Test
    fun fromHost_detectsNyaaAndSukebei() {
        assertEquals(CatalogSite.NYAA, CatalogSite.fromHost("nyaa.si"))
        assertEquals(CatalogSite.NYAA, CatalogSite.fromHost("www.nyaa.si"))
        assertEquals(CatalogSite.SUKEBEI, CatalogSite.fromHost("sukebei.nyaa.si"))
        assertEquals(CatalogSite.SUKEBEI, CatalogSite.fromHost("www.sukebei.nyaa.si"))
    }

    @Test
    fun fromUrl_readsGuidHosts() {
        assertEquals(CatalogSite.NYAA, CatalogSite.fromUrl("https://nyaa.si/view/12"))
        assertEquals(CatalogSite.SUKEBEI, CatalogSite.fromUrl("https://sukebei.nyaa.si/view/12"))
    }

    @Test
    fun categories_differBySite() {
        assertTrue(CatalogSite.NYAA.categories.any { it.displayName == "Anime" })
        assertTrue(CatalogSite.SUKEBEI.categories.any { it.displayName == "Art" })
        assertFalse(CatalogSite.NYAA.categories.any { it.displayName == "Art" })
        assertFalse(CatalogSite.SUKEBEI.categories.any { it.displayName == "Anime" })
    }

    @Test
    fun bookmarkKey_includesSite() {
        val nyaa = Torrent(
            id = "12", title = "Title", link = "https://nyaa.si/download/12.torrent",
            guid = "https://nyaa.si/view/12", pubDate = "", seeders = 0, leechers = 0,
            downloads = 0, infoHash = "ff", category = "Anime", size = "1 MiB",
            comments = 0, trusted = false, remake = false, magnetLink = "",
            site = CatalogSite.NYAA
        )
        val sukebei = nyaa.copy(site = CatalogSite.SUKEBEI)
        assertEquals("nyaa:12", nyaa.bookmarkKey())
        assertEquals("sukebei:12", sukebei.bookmarkKey())
        assertTrue(nyaa.identity() == sukebei.identity())
    }
}
