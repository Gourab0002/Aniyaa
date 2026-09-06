package com.nyaa.aniyaa.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NyaaHtmlSearchParserTest {

    @Test
    fun parse_readsTorrentRows() {
        val html = """
            <table class="torrent-list">
              <tbody>
                <tr class="success">
                  <td><a href="/?c=1_1" title="Art - Anime"><img alt="Art - Anime"></a></td>
                  <td colspan="2"><a href="/view/42" title="Example Title">Example Title</a></td>
                  <td>
                    <a href="/download/42.torrent"></a>
                    <a href="magnet:?xt=urn:btih:ABCDEF0123456789ABCDEF0123456789ABCDEF01"></a>
                  </td>
                  <td>1.5 GiB</td>
                  <td data-timestamp="1600000000">2020-09-13</td>
                  <td>12</td>
                  <td>3</td>
                  <td>99</td>
                </tr>
              </tbody>
            </table>
        """.trimIndent()

        val torrents = NyaaHtmlSearchParser.parse(html, "https://sukebei.nyaa.si")
        assertEquals(1, torrents.size)
        val torrent = torrents.first()
        assertEquals("42", torrent.id)
        assertEquals("Example Title", torrent.title)
        assertEquals("Art - Anime", torrent.category)
        assertEquals("1.5 GiB", torrent.size)
        assertEquals(12, torrent.seeders)
        assertEquals(3, torrent.leechers)
        assertEquals(99, torrent.downloads)
        assertTrue(torrent.trusted)
        assertEquals("ABCDEF0123456789ABCDEF0123456789ABCDEF01", torrent.infoHash)
        assertEquals("https://sukebei.nyaa.si/view/42", torrent.guid)
    }
}
