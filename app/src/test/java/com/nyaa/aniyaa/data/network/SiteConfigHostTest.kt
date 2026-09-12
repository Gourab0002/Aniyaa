package com.nyaa.aniyaa.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteConfigHostTest {

    @Test
    fun isCatalogHost_matchesNyaaFamily() {
        assertTrue(SiteConfig.isCatalogHost("nyaa.si"))
        assertTrue(SiteConfig.isCatalogHost("www.sukebei.nyaa.si"))
        assertTrue(SiteConfig.isCatalogHost("nyaa.iss.one"))
        assertFalse(SiteConfig.isCatalogHost("api.github.com"))
        assertFalse(SiteConfig.isCatalogHost(""))
    }

    @Test
    fun normalize_upgradesHttpToHttps() {
        assertEquals(
            "https://nyaa.iss.one",
            SiteConfig.normalize("http://nyaa.iss.one", com.nyaa.aniyaa.data.model.CatalogSite.NYAA)
        )
    }
}
