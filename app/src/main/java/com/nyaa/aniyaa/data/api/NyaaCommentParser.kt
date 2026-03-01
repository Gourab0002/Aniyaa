package com.nyaa.aniyaa.data.api

import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.model.TorrentPageData
import org.jsoup.Jsoup

object NyaaCommentParser {

    fun parse(html: String): TorrentPageData {
        val doc = Jsoup.parse(html)

        // Parse description from div#torrent-description
        val descriptionEl = doc.selectFirst("div#torrent-description")
        val description = descriptionEl?.html()?.trim() ?: ""

        // nyaa.si comment structure:
        // <div class="comment" id="com-XXXXX">
        //   <div class="comment-details">
        //     <img src="..." />      (optional avatar)
        //     <a href="/user/...">username</a>
        //     <small>2024-01-01 00:00:00 UTC</small>
        //   </div>
        //   <div class="comment-content markdown-rendered">
        //     <p>...</p>
        //   </div>
        // </div>
        val comments = mutableListOf<TorrentComment>()
        val commentElements = doc.select("div.comment[id^=com-]")
        for (element in commentElements) {
            val id = element.id().removePrefix("com-")
            val details = element.selectFirst("div.comment-details") ?: continue
            val username = details.selectFirst("a")?.text()?.trim() ?: "Anonymous"
            val avatarSrc = details.selectFirst("img")?.attr("src") ?: ""
            val avatarUrl = when {
                avatarSrc.startsWith("//") -> "https:$avatarSrc"
                avatarSrc.startsWith("/") -> "https://nyaa.si$avatarSrc"
                else -> avatarSrc
            }
            // Date is in a <small> tag; fall back to text after " - " for older layouts
            val date = details.selectFirst("small")?.text()?.trim()
                ?: details.text().substringAfter("- ").trim()
            val contentEl = element.selectFirst("div.comment-content")
            val content = contentEl?.html()?.trim() ?: ""
            if (content.isNotEmpty()) {
                comments.add(TorrentComment(id = id, username = username, avatarUrl = avatarUrl, date = date, content = content))
            }
        }
        return TorrentPageData(description = description, comments = comments)
    }
}
