package com.nyaa.aniyaa.data.api

import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.network.SiteConfig
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NyaaHtmlSearchParser {

    private val viewIdRegex = Regex("/view/(\\d+)")
    private val infoHashRegex = Regex("urn:btih:([a-fA-F0-9]{32,40})", RegexOption.IGNORE_CASE)

    fun parse(html: String, baseUrl: String = SiteConfig.baseUrl): List<Torrent> {
        val doc = Jsoup.parse(html, baseUrl)
        val rows = doc.select("table.torrent-list tbody tr, table.table tbody tr")
        if (rows.isEmpty()) return emptyList()

        val results = ArrayList<Torrent>(rows.size)
        for (row in rows) {
            val tds = row.select("td")
            if (tds.size < 5) continue
            val titleLink = tds.asSequence()
                .flatMap { it.select("a[href*=/view/]") }
                .firstOrNull { !it.attr("href").contains("#comments") }
                ?: continue
            val id = viewIdRegex.find(titleLink.attr("href"))?.groupValues?.getOrNull(1).orEmpty()
            if (id.isEmpty()) continue

            val magnetHref = row.select("a[href^=magnet]").attr("href")
            val infoHash = infoHashRegex.find(magnetHref)?.groupValues?.getOrNull(1).orEmpty()
            val downloadHref = row.select("a[href*=/download/]").attr("href")
            val category = row.selectFirst("td a[href*=c=]")?.attr("title")
                ?.ifBlank { row.selectFirst("td img")?.attr("alt") }
                .orEmpty()

            val actionIndex = tds.indexOfFirst {
                it.select("a[href^=magnet]").isNotEmpty() || it.select("a[href*=/download/]").isNotEmpty()
            }
            val sizeTd = tds.getOrNull(if (actionIndex >= 0) actionIndex + 1 else tds.size - 5)
            val dateTd = tds.getOrNull(if (actionIndex >= 0) actionIndex + 2 else tds.size - 4)
            val seedersTd = tds.getOrNull(if (actionIndex >= 0) actionIndex + 3 else tds.size - 3)
            val leechersTd = tds.getOrNull(if (actionIndex >= 0) actionIndex + 4 else tds.size - 2)
            val downloadsTd = tds.getOrNull(if (actionIndex >= 0) actionIndex + 5 else tds.size - 1)

            val commentsText = row.select("a[href*=#comments]").text()
            val comments = Regex("(\\d+)").find(commentsText)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val classes = row.className()

            results.add(
                Torrent(
                    id = id,
                    title = titleLink.attr("title").ifBlank { titleLink.text() }.trim(),
                    link = absolute(baseUrl, downloadHref.ifBlank { "/download/$id.torrent" }),
                    guid = "$baseUrl/view/$id",
                    pubDate = dateFromTimestamp(dateTd?.attr("data-timestamp"))
                        .ifBlank { dateTd?.text()?.trim().orEmpty() },
                    seeders = seedersTd?.text()?.trim()?.toIntOrNull() ?: 0,
                    leechers = leechersTd?.text()?.trim()?.toIntOrNull() ?: 0,
                    downloads = downloadsTd?.text()?.trim()?.toIntOrNull() ?: 0,
                    infoHash = infoHash,
                    category = category,
                    size = sizeTd?.text()?.trim().orEmpty(),
                    comments = comments,
                    trusted = classes.contains("success"),
                    remake = classes.contains("danger"),
                    magnetLink = magnetHref
                )
            )
        }
        return results
    }

    internal fun dateFromTimestamp(raw: String?): String {
        val seconds = raw?.toLongOrNull() ?: return ""
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(seconds * 1000L))
    }

    private fun absolute(baseUrl: String, path: String): String {
        val value = path.trim()
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        if (value.startsWith("//")) return "https:$value"
        if (value.startsWith("/")) return baseUrl + value
        return "$baseUrl/$value"
    }
}
