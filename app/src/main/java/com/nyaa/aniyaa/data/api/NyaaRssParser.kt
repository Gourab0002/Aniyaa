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

    fun buildMagnetLink(infoHash: String, title: String): String {
        val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        val trackerParams = TRACKERS.joinToString("") { "&tr=${URLEncoder.encode(it, "UTF-8")}" }
        return "magnet:?xt=urn:btih:$infoHash&dn=$encodedTitle$trackerParams"
    }

    fun parse(inputStream: InputStream): List<Torrent> {
        val torrents = mutableListOf<Torrent>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(inputStream, null)

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
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val namespace = parser.namespace ?: ""
            val name = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = name
                    if (name == "item") {
                        inItem = true
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
                    } else if (inItem && name == "link" && namespace.isEmpty()) {
                        val href = parser.getAttributeValue(null, "href")
                        if (href != null) link = href
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val text = parser.text?.trim() ?: ""
                        when {
                            currentTag == "title" && namespace.isEmpty() -> title = text
                            currentTag == "link" && namespace.isEmpty() && link.isEmpty() -> link = text
                            currentTag == "guid" -> {
                                guid = text
                                id = text.substringAfterLast("/")
                            }
                            currentTag == "pubDate" -> pubDate = text
                            currentTag == "seeders" && namespace.contains("nyaa") -> seeders = text.toIntOrNull() ?: 0
                            currentTag == "leechers" && namespace.contains("nyaa") -> leechers = text.toIntOrNull() ?: 0
                            currentTag == "downloads" && namespace.contains("nyaa") -> downloads = text.toIntOrNull() ?: 0
                            currentTag == "infoHash" && namespace.contains("nyaa") -> infoHash = text
                            currentTag == "category" && namespace.contains("nyaa") -> category = text
                            currentTag == "size" && namespace.contains("nyaa") -> size = text
                            currentTag == "comments" && namespace.contains("nyaa") -> comments = text.toIntOrNull() ?: 0
                            currentTag == "trusted" && namespace.contains("nyaa") -> trusted = text.equals("Yes", ignoreCase = true)
                            currentTag == "remake" && namespace.contains("nyaa") -> remake = text.equals("Yes", ignoreCase = true)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "item" && inItem) {
                        inItem = false
                        val magnetLink = if (infoHash.isNotEmpty()) buildMagnetLink(infoHash, title) else ""
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
                                magnetLink = magnetLink
                            )
                        )
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return torrents
    }
}
