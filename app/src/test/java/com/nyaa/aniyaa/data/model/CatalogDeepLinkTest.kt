package com.nyaa.aniyaa.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogDeepLinkTest {

    @Test
    fun parse_viewOnNyaa() {
        val link = CatalogDeepLinks.parse("https://nyaa.si/view/123456")
        assertNotNull(link)
        assertEquals(CatalogSite.NYAA, link!!.site)
        assertEquals("123456", link.viewId)
        assertFalse(link.requiresNsfw)
    }

    @Test
    fun parse_viewOnWwwAndHttp() {
        val http = CatalogDeepLinks.parse("http://www.nyaa.si/view/99#com-2")
        assertEquals("99", http?.viewId)
        assertEquals(CatalogSite.NYAA, http?.site)
    }

    @Test
    fun parse_sukebeiRequiresNsfw() {
        val link = CatalogDeepLinks.parse("https://sukebei.nyaa.si/view/7")
        assertEquals(CatalogSite.SUKEBEI, link?.site)
        assertTrue(link!!.requiresNsfw)
        assertEquals("7", link.viewId)
    }

    @Test
    fun parse_searchQueryAndUser() {
        val link = CatalogDeepLinks.parse("https://nyaa.si/?q=bocchi&c=1_2&f=2&s=seeders&o=desc")
        assertEquals("bocchi", link?.searchParams?.query)
        assertEquals("1_2", link?.searchParams?.category?.value)
        assertEquals(FilterOption.TRUSTED, link?.searchParams?.filter)
        assertEquals(SortField.SEEDERS, link?.searchParams?.sortField)

        val user = CatalogDeepLinks.parse("https://nyaa.si/user/erai?q=1080p")
        assertEquals("user:erai 1080p", user?.searchParams?.query)
    }

    @Test
    fun parse_appSchemes() {
        val view = CatalogDeepLinks.parse("aniyaa://view/42")
        assertEquals("42", view?.viewId)
        val saved = CatalogDeepLinks.parse("aniyaa://saved/9")
        assertEquals(9L, saved?.savedSearchId)
    }

    @Test
    fun parse_relativeViewUsesFallback() {
        val link = CatalogDeepLinks.parse("/view/55", CatalogSite.SUKEBEI)
        assertEquals(CatalogSite.SUKEBEI, link?.site)
        assertEquals("55", link?.viewId)
        assertTrue(link!!.requiresNsfw)
    }

    @Test
    fun parse_blankReturnsNull() {
        assertNull(CatalogDeepLinks.parse("   "))
    }
}
