package com.nyaa.aniyaa.data.api

import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.model.TorrentPageData
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

object NyaaCommentParser {

    fun parse(html: String): TorrentPageData {
        val doc = Jsoup.parse(html)

        // Parse description from div#torrent-description
        val descriptionEl = doc.selectFirst("div#torrent-description")
        val description = descriptionEl?.html()?.trim()
            ?.let { raw -> if (raw.isEmpty()) "" else Jsoup.clean(raw, "https://nyaa.si", Safelist.relaxed()) }
            ?: ""

        // nyaa.si real comment structure (verified against Nyaa-Api-Go/Nyaa-Api-Ts):
        // <div class="comment-panel">
        //   <a href="/user/username">username</a>
        //   <a href="#com-XXXXX"><time datetime="...">timestamp</time></a>
        //   <img class="avatar" src="..." />
        //   <div class="comment-body">
        //     <div class="comment-content markdown-rendered">...</div>
        //   </div>
        // </div>
        val comments = mutableListOf<TorrentComment>()
        val commentElements = doc.select("div#comments div.comment-panel")
        for (element in commentElements) {
            val links = element.select("a")
            val username = links.firstOrNull()?.text()?.trim() ?: "Anonymous"
            val avatarSrc = element.selectFirst("img.avatar")?.attr("src") ?: ""
            val avatarUrl = when {
                avatarSrc.startsWith("//") -> "https:$avatarSrc"
                avatarSrc.startsWith("/") -> "https://nyaa.si$avatarSrc"
                else -> avatarSrc
            }
            // Timestamp lives inside a <time> child of one of the <a> elements
            val date = links.asSequence()
                .flatMap { it.children().asSequence() }
                .firstOrNull()
                ?.text()?.trim() ?: ""
            val contentEl = element.selectFirst("div.comment-body div.comment-content")
            val content = contentEl?.html()?.trim() ?: ""
            if (content.isNotEmpty()) {
                comments.add(TorrentComment(id = "", username = username, avatarUrl = avatarUrl, date = date, content = content))
            }
        }
        return TorrentPageData(description = description, comments = comments)
    }
}
