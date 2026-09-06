package com.nyaa.aniyaa.data.api

import com.nyaa.aniyaa.data.model.Torrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

class NyaaRssParserTest {

    private fun parser(): XmlPullParser = KXmlParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
    }

    private fun parse(rss: String): List<Torrent> =
        NyaaRssParser.parse(ByteArrayInputStream(rss.toByteArray()), parser())

    @Test
    fun parse_readsNyaaFieldsAndBuildsMagnet() {
        val rss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss xmlns:nyaa="https://nyaa.si/xmlns/nyaa" version="2.0">
              <channel>
                <item>
                  <title>Test &amp; Title</title>
                  <link>https://sukebei.nyaa.si/download/123.torrent</link>
                  <guid isPermaLink="true">https://sukebei.nyaa.si/view/123</guid>
                  <pubDate>Wed, 01 Jan 2025 00:00:00 -0000</pubDate>
                  <nyaa:seeders>10</nyaa:seeders>
                  <nyaa:leechers>2</nyaa:leechers>
                  <nyaa:downloads>100</nyaa:downloads>
                  <nyaa:infoHash>ABCDEF</nyaa:infoHash>
                  <nyaa:category>Art - Anime</nyaa:category>
                  <nyaa:size>1.5 GiB</nyaa:size>
                  <nyaa:comments>3</nyaa:comments>
                  <nyaa:trusted>Yes</nyaa:trusted>
                  <nyaa:remake>No</nyaa:remake>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val torrents = parse(rss)
        assertEquals(1, torrents.size)
        val torrent = torrents[0]
        assertEquals("123", torrent.id)
        assertEquals("Test & Title", torrent.title)
        assertEquals("https://sukebei.nyaa.si/download/123.torrent", torrent.link)
        assertEquals(10, torrent.seeders)
        assertEquals(2, torrent.leechers)
        assertEquals(100, torrent.downloads)
        assertEquals("ABCDEF", torrent.infoHash)
        assertEquals("Art - Anime", torrent.category)
        assertEquals("1.5 GiB", torrent.size)
        assertEquals(3, torrent.comments)
        assertTrue(torrent.trusted)
        assertFalse(torrent.remake)
        assertEquals("", torrent.magnetLink)
        val magnet = NyaaRssParser.buildMagnetLink(torrent.infoHash, torrent.title)
        assertTrue(magnet.startsWith("magnet:?xt=urn:btih:ABCDEF"))
        assertTrue(magnet.contains("Test"))
        assertEquals(magnet, torrent.resolvedMagnet())
    }

    @Test
    fun parse_handlesMultipleItems() {
        val rss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss xmlns:nyaa="https://nyaa.si/xmlns/nyaa" version="2.0">
              <channel>
                <item>
                  <title>One</title>
                  <guid>https://sukebei.nyaa.si/view/1</guid>
                  <nyaa:infoHash>AAA</nyaa:infoHash>
                </item>
                <item>
                  <title>Two</title>
                  <guid>https://sukebei.nyaa.si/view/2</guid>
                  <nyaa:infoHash>BBB</nyaa:infoHash>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val torrents = parse(rss)
        assertEquals(listOf("1", "2"), torrents.map { it.id })
        assertEquals(listOf("One", "Two"), torrents.map { it.title })
    }
}
