package com.nyaa.aniyaa.data.api

import android.util.Xml
import com.nyaa.aniyaa.data.model.Torrent
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URLEncoder

object NyaaRssParser {

    private val TRACKERS = listOf(
        "http://nyaa.tracker.wf:7777/announce",
        "https://tracker.nanoha.org/announce",
        "https://tracker.opentrackr.org/announce",
        "http://tracker.openbittorrent.com:80/announce"
    )

    private val TRACKER_QUERY: String =
        TRACKERS.joinToString(separator = "") { "&tr=${URLEncoder.encode(it, "UTF-8")}" }

    fun buildMagnetLink(infoHash: String, title: String): String {
        if (infoHash.isEmpty()) return ""
        val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        return "magnet:?xt=urn:btih:$infoHash&dn=$encodedTitle$TRACKER_QUERY"
    }

    fun parse(inputStream: InputStream, parser: XmlPullParser = newDefaultParser()): List<Torrent> {
        parser.setInput(inputStream, null)

        val torrents = mutableListOf<Torrent>()
        val textBuffer = StringBuilder()

        var eventType = parser.eventType
        var inItem = false
        var id = ""
        var title = ""
        var link = ""
        var guid = ""
        var pubDate = ""
        var seeders = 0
        var leechers = 0
        var downloads = 0
        var infoHash = ""
        var category = ""
        var size = ""
        var comments = 0
        var trusted = false
        var remake = false

        fun resetItem() {
            id = ""
            title = ""
            link = ""
            guid = ""
            pubDate = ""
            seeders = 0
            leechers = 0
            downloads = 0
            infoHash = ""
            category = ""
            size = ""
            comments = 0
            trusted = false
            remake = false
            textBuffer.setLength(0)
        }

        fun applyText(tag: String, namespace: String, text: String) {
            when {
                tag == "title" && namespace.isEmpty() -> title = text
                tag == "link" && namespace.isEmpty() && link.isEmpty() -> link = text
                tag == "guid" -> {
                    guid = text
                    id = text.substringAfterLast("/")
                }
                tag == "pubDate" -> pubDate = text
                tag == "seeders" && namespace.contains("nyaa") -> seeders = text.toIntOrNull() ?: 0
                tag == "leechers" && namespace.contains("nyaa") -> leechers = text.toIntOrNull() ?: 0
                tag == "downloads" && namespace.contains("nyaa") -> downloads = text.toIntOrNull() ?: 0
                tag == "infoHash" && namespace.contains("nyaa") -> infoHash = text
                tag == "category" && namespace.contains("nyaa") -> category = text
                tag == "size" && namespace.contains("nyaa") -> size = text
                tag == "comments" && namespace.contains("nyaa") -> comments = text.toIntOrNull() ?: 0
                tag == "trusted" && namespace.contains("nyaa") -> trusted = text.equals("Yes", ignoreCase = true)
                tag == "remake" && namespace.contains("nyaa") -> remake = text.equals("Yes", ignoreCase = true)
            }
        }

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val namespace = parser.namespace ?: ""
            val name = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    textBuffer.setLength(0)
                    if (name == "item") {
                        inItem = true
                        resetItem()
                    } else if (inItem && name == "link" && namespace.isEmpty()) {
                        val href = parser.getAttributeValue(null, "href")
                        if (href != null) link = href
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> {
                    if (inItem) {
                        textBuffer.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (inItem) {
                        val text = textBuffer.toString().trim()
                        if (name == "item") {
                            torrents.add(
                                Torrent(
                                    id = id,
                                    title = title,
                                    link = link,
                                    guid = guid,
                                    pubDate = pubDate,
                                    seeders = seeders,
                                    leechers = leechers,
                                    downloads = downloads,
                                    infoHash = infoHash,
                                    category = category,
                                    size = size,
                                    comments = comments,
                                    trusted = trusted,
                                    remake = remake,
                                    magnetLink = ""
                                )
                            )
                            inItem = false
                        } else {
                            applyText(name, namespace, text)
                        }
                    }
                    textBuffer.setLength(0)
                }
            }
            eventType = parser.next()
        }
        return torrents
    }

    private fun newDefaultParser(): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        return parser
    }
}

fun Torrent.resolvedMagnet(): String = when {
    magnetLink.isNotEmpty() -> magnetLink
    infoHash.isNotEmpty() -> NyaaRssParser.buildMagnetLink(infoHash, title)
    else -> ""
}

fun Torrent.withMagnet(): Torrent {
    val magnet = resolvedMagnet()
    return if (magnet == magnetLink) this else copy(magnetLink = magnet)
}
