package com.nyaa.aniyaa.data.api

import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.model.TorrentFileEntry
import com.nyaa.aniyaa.data.model.TorrentPageData
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object NyaaCommentParser {

    fun parse(html: String): TorrentPageData {
        val doc = Jsoup.parse(html)

        val descriptionEl = doc.selectFirst("div#torrent-description")
        val description = descriptionEl?.wholeText()?.trim().orEmpty()

        val comments = mutableListOf<TorrentComment>()
        val commentElements = doc.select("div#comments div.comment-panel")
        for (element in commentElements) {
            val links = element.select("a")
            val username = links.firstOrNull()?.text()?.trim().orEmpty().ifEmpty { "Anonymous" }
            val avatarSrc = element.selectFirst("img.avatar")?.attr("src").orEmpty()
            val avatarUrl = when {
                avatarSrc.startsWith("//") -> "https:$avatarSrc"
                avatarSrc.startsWith("/") -> "https://nyaa.si$avatarSrc"
                else -> avatarSrc
            }
            val date = links.asSequence()
                .flatMap { it.children().asSequence() }
                .firstOrNull()
                ?.text()?.trim().orEmpty()
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

        return TorrentPageData(description = description, fileList = fileEntries, comments = comments)
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
}
