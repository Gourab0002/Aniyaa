package com.nyaa.aniyaa.data.api

import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.model.TorrentFileEntry
import com.nyaa.aniyaa.data.model.TorrentPageData
import com.nyaa.aniyaa.data.network.SiteConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object NyaaCommentParser {

    fun parse(html: String, baseUrl: String = SiteConfig.baseUrl): TorrentPageData {
        val doc = Jsoup.parse(html, baseUrl)

        val descriptionEl = doc.selectFirst("div#torrent-description")
        val description = descriptionEl?.let { readDescription(it, baseUrl) }.orEmpty()

        val comments = mutableListOf<TorrentComment>()
        val commentElements = doc.select("div#comments div.comment-panel")
        for (element in commentElements) {
            val userLink = element.selectFirst("a[href*=/user/]")
            val username = userLink?.text()?.trim().orEmpty()
                .ifEmpty { element.select("a").firstOrNull()?.text()?.trim().orEmpty() }
                .ifEmpty { "Anonymous" }
            val avatarSrc = element.selectFirst("img.avatar")?.attr("src").orEmpty()
            val avatarUrl = when {
                avatarSrc.startsWith("//") -> "https:$avatarSrc"
                avatarSrc.startsWith("/") -> "$baseUrl$avatarSrc"
                avatarSrc.startsWith("http") -> avatarSrc
                avatarSrc.isBlank() -> ""
                else -> "$baseUrl/$avatarSrc"
            }
            val date = element.selectFirst("small, time, a[href*=#com-] small")?.text()?.trim()
                .orEmpty()
                .ifEmpty {
                    element.select("a").asSequence()
                        .flatMap { it.children().asSequence() }
                        .firstOrNull()
                        ?.text()?.trim().orEmpty()
                }
            val contentEl = element.selectFirst("div.comment-body div.comment-content")
            val content = contentEl?.wholeText()?.trim().orEmpty()
            val id = element.attr("id").removePrefix("com-")
            if (content.isNotEmpty()) {
                comments.add(
                    TorrentComment(
                        id = id,
                        username = username,
                        avatarUrl = avatarUrl,
                        date = date,
                        content = content
                    )
                )
            }
        }

        val fileEntries = mutableListOf<TorrentFileEntry>()
        val fileListEl = doc.selectFirst("div.torrent-file-list")
        val rootUl = fileListEl?.selectFirst("ul")
        if (rootUl != null) {
            collectFiles(rootUl, prefix = "", out = fileEntries)
        }

        val panel = doc.selectFirst("div.panel-heading h3.panel-title")?.parent()?.parent()
            ?: doc.selectFirst("div.panel")
        val title = doc.selectFirst("h3.panel-title")?.ownText()?.trim()
            .orEmpty()
            .ifEmpty { doc.selectFirst("h3.panel-title")?.text()?.trim().orEmpty() }
        val submitter = labeledValue(doc, "Submitter")
            .ifBlank { doc.selectFirst("a[href*=/user/]")?.text()?.trim().orEmpty() }
            .let { if (it.equals("Anonymous", ignoreCase = true)) "" else it }
        val category = labeledValue(doc, "Category")
        val size = labeledValue(doc, "File size")
        val infoHash = doc.selectFirst("kbd")?.text()?.trim().orEmpty()
            .ifBlank { labeledValue(doc, "Information hash") }
        val seeders = labeledValue(doc, "Seeders").filter { it.isDigit() }.toIntOrNull() ?: 0
        val leechers = labeledValue(doc, "Leechers").filter { it.isDigit() }.toIntOrNull() ?: 0
        val downloads = labeledValue(doc, "Completed").filter { it.isDigit() }.toIntOrNull()
            ?: labeledValue(doc, "Downloads").filter { it.isDigit() }.toIntOrNull()
            ?: 0
        val magnetLink = doc.selectFirst("a[href^=magnet]")?.attr("href").orEmpty()
        val downloadUrl = doc.selectFirst("a[href*=/download/]")?.attr("abs:href")
            .orEmpty()
            .ifBlank {
                val href = doc.selectFirst("a[href*=/download/]")?.attr("href").orEmpty()
                if (href.isNotBlank()) {
                    SiteConfig.resolveUrl(href, CatalogSite.fromUrl(baseUrl) ?: SiteConfig.currentSite)
                } else {
                    ""
                }
            }
        val pubDate = labeledValue(doc, "Date")
        val panelClass = panel?.className().orEmpty() + " " + doc.selectFirst("div.panel")?.className().orEmpty()
        val trusted = panelClass.contains("success")
        val remake = panelClass.contains("danger")
        val commentsCount = comments.size.takeIf { it > 0 }
            ?: labeledValue(doc, "Comments").filter { it.isDigit() }.toIntOrNull()
            ?: 0

        return TorrentPageData(
            description = description,
            fileList = fileEntries,
            comments = comments,
            submitter = submitter,
            title = title,
            category = category,
            size = size,
            infoHash = infoHash,
            seeders = seeders,
            leechers = leechers,
            downloads = downloads,
            commentsCount = commentsCount,
            trusted = trusted,
            remake = remake,
            magnetLink = magnetLink,
            downloadUrl = downloadUrl,
            pubDate = pubDate
        )
    }

    fun toTorrent(
        page: TorrentPageData,
        torrentId: String,
        fallback: Torrent? = null,
        site: CatalogSite = fallback?.site ?: SiteConfig.currentSite
    ): Torrent {
        val base = SiteConfig.baseUrl(site)
        return Torrent(
            id = torrentId.ifBlank { fallback?.id.orEmpty() },
            title = page.title.ifBlank { fallback?.title.orEmpty() },
            link = page.downloadUrl.ifBlank { fallback?.link.orEmpty() }.ifBlank { "$base/download/$torrentId.torrent" },
            guid = fallback?.guid?.ifBlank { null } ?: "$base/view/$torrentId",
            pubDate = page.pubDate.ifBlank { fallback?.pubDate.orEmpty() },
            seeders = page.seeders.takeIf { it > 0 } ?: fallback?.seeders ?: 0,
            leechers = page.leechers.takeIf { it > 0 } ?: fallback?.leechers ?: 0,
            downloads = page.downloads.takeIf { it > 0 } ?: fallback?.downloads ?: 0,
            infoHash = page.infoHash.ifBlank { fallback?.infoHash.orEmpty() },
            category = page.category.ifBlank { fallback?.category.orEmpty() },
            size = page.size.ifBlank { fallback?.size.orEmpty() },
            comments = page.commentsCount.takeIf { it > 0 } ?: fallback?.comments ?: page.comments.size,
            trusted = page.trusted || (fallback?.trusted == true),
            remake = page.remake || (fallback?.remake == true),
            magnetLink = page.magnetLink.ifBlank { fallback?.magnetLink.orEmpty() },
            submitter = page.submitter.ifBlank { fallback?.submitter.orEmpty() },
            addedAt = fallback?.addedAt ?: 0L,
            site = site
        )
    }

    internal fun collectFiles(parent: Element, prefix: String, out: MutableList<TorrentFileEntry>) {
        for (li in parent.children()) {
            if (!li.tagName().equals("li", ignoreCase = true)) continue
            val childUl = li.children().firstOrNull { it.tagName().equals("ul", ignoreCase = true) }
            if (childUl != null) {
                val folder = li.ownText().trim()
                val nextPrefix = when {
                    prefix.isEmpty() -> folder
                    folder.isEmpty() -> prefix
                    else -> "$prefix/$folder"
                }
                collectFiles(childUl, nextPrefix, out)
            } else {
                val size = li.selectFirst("span.pull-right")?.text()?.trim().orEmpty()
                li.selectFirst("span.pull-right")?.remove()
                val name = li.text().trim()
                if (name.isNotEmpty()) {
                    val fullName = if (prefix.isEmpty()) name else "$prefix/$name"
                    out.add(TorrentFileEntry(name = fullName, size = size))
                }
            }
        }
    }

    private fun labeledValue(doc: org.jsoup.nodes.Document, label: String): String {
        val rows = doc.select("div.row")
        for (row in rows) {
            val labelEl = row.selectFirst("div.col-md-5") ?: continue
            if (labelEl.text().trim().trimEnd(':').equals(label, ignoreCase = true)) {
                return row.selectFirst("div.col-md-7")?.text()?.trim().orEmpty()
            }
        }
        return ""
    }

    internal fun readDescription(element: Element, baseUrl: String): String {
        val rendered = element.select("img, table, p, h1, h2, h3, h4, pre, ul, ol, blockquote").isNotEmpty()
        val markdown = if (rendered) htmlToMarkdown(element, baseUrl) else element.wholeText()
        return markdown.trim()
    }

    private fun htmlToMarkdown(element: Element, baseUrl: String): String {
        val out = StringBuilder()
        writeMarkdown(element, out, baseUrl)
        return out.toString()
    }

    private fun writeMarkdown(node: Node, out: StringBuilder, baseUrl: String) {
        when (node) {
            is TextNode -> out.append(node.text())
            is Element -> {
                val tag = node.tagName().lowercase()
                when (tag) {
                    "br" -> out.append('\n')
                    "p", "div" -> {
                        node.childNodes().forEach { writeMarkdown(it, out, baseUrl) }
                        out.append("\n\n")
                    }
                    "h1" -> wrapLine(node, out, baseUrl, "# ")
                    "h2" -> wrapLine(node, out, baseUrl, "## ")
                    "h3" -> wrapLine(node, out, baseUrl, "### ")
                    "h4", "h5", "h6" -> wrapLine(node, out, baseUrl, "#### ")
                    "strong", "b" -> wrap(node, out, baseUrl, "**", "**")
                    "em", "i" -> wrap(node, out, baseUrl, "*", "*")
                    "s", "del" -> wrap(node, out, baseUrl, "~~", "~~")
                    "code" -> if (node.parent()?.tagName() == "pre") {
                        node.childNodes().forEach { writeMarkdown(it, out, baseUrl) }
                    } else {
                        wrap(node, out, baseUrl, "`", "`")
                    }
                    "pre" -> {
                        out.append("\n```\n")
                        out.append(node.wholeText().trimEnd())
                        out.append("\n```\n")
                    }
                    "blockquote" -> {
                        val inner = StringBuilder()
                        node.childNodes().forEach { writeMarkdown(it, inner, baseUrl) }
                        inner.toString().trim().lines().forEach { out.append("> ").append(it).append('\n') }
                        out.append('\n')
                    }
                    "ul", "ol" -> {
                        node.children().forEach { child ->
                            if (child.tagName().equals("li", ignoreCase = true)) {
                                out.append("- ")
                                child.childNodes().forEach { writeMarkdown(it, out, baseUrl) }
                                out.append('\n')
                            }
                        }
                        out.append('\n')
                    }
                    "img" -> {
                        val src = absoluteUrl(node, "src", baseUrl)
                        if (src.isNotEmpty()) {
                            val alt = node.attr("alt")
                            out.append("![").append(alt).append("](").append(src).append(")\n")
                        }
                    }
                    "a" -> {
                        val href = absoluteUrl(node, "href", baseUrl)
                        out.append('[')
                        node.childNodes().forEach { writeMarkdown(it, out, baseUrl) }
                        out.append("](").append(href).append(')')
                    }
                    "hr" -> out.append("\n\n---\n\n")
                    "table" -> out.append(tableMarkdown(node)).append('\n')
                    else -> node.childNodes().forEach { writeMarkdown(it, out, baseUrl) }
                }
            }
        }
    }

    private fun wrap(node: Element, out: StringBuilder, baseUrl: String, prefix: String, suffix: String) {
        out.append(prefix)
        node.childNodes().forEach { writeMarkdown(it, out, baseUrl) }
        out.append(suffix)
    }

    private fun wrapLine(node: Element, out: StringBuilder, baseUrl: String, prefix: String) {
        out.append(prefix)
        node.childNodes().forEach { writeMarkdown(it, out, baseUrl) }
        out.append("\n\n")
    }

    private fun tableMarkdown(table: Element): String {
        val rows = table.select("tr")
        if (rows.isEmpty()) return ""
        val parsed = rows.map { row ->
            row.select("th, td").map { it.text().trim() }
        }.filter { it.isNotEmpty() }
        if (parsed.isEmpty()) return ""
        val width = parsed.maxOf { it.size }
        fun pad(row: List<String>) = row + List(width - row.size) { "" }
        val header = pad(parsed.first())
        val body = parsed.drop(1).map(::pad)
        val sb = StringBuilder()
        sb.append("| ").append(header.joinToString(" | ")).append(" |\n")
        sb.append("| ").append(List(width) { "---" }.joinToString(" | ")).append(" |\n")
        body.forEach { sb.append("| ").append(it.joinToString(" | ")).append(" |\n") }
        return sb.toString()
    }

    private fun absoluteUrl(element: Element, attr: String, baseUrl: String): String {
        val value = element.absUrl(attr).ifBlank { element.attr(attr) }
        return when {
            value.startsWith("//") -> "https:$value"
            value.startsWith("/") -> baseUrl.trimEnd('/') + value
            else -> value
        }
    }
}
