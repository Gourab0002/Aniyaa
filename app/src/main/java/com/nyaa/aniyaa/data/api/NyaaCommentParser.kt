package com.nyaa.aniyaa.data.api

import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.model.TorrentPageData
import org.jsoup.Jsoup

object NyaaCommentParser {

    fun parse(html: String): TorrentPageData {
        val doc = Jsoup.parse(html)

        // Parse description from div#torrent-description
        val descriptionEl = doc.selectFirst("div#torrent-description")
        val description = descriptionEl?.text()?.trim() ?: ""

        // nyaa.si comment structure:
        // <div class="comment" id="com-XXXXX">
        //   <div class="comment-details">
        //     <img src="..." />      (optional avatar)
        //     <a href="/user/...">username</a>
        //     - 2024-01-01 00:00:00 UTC
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
            val avatarUrl = details.selectFirst("img")?.attr("src") ?: ""
            val detailsText = details.text()
            // Date is typically after " - " in the details text
            val date = detailsText.substringAfter("- ").trim()
            val contentEl = element.selectFirst("div.comment-content")
            val content = contentEl?.text()?.trim() ?: ""
            if (content.isNotEmpty()) {
                comments.add(TorrentComment(id = id, username = username, avatarUrl = avatarUrl, date = date, content = content))
            }
        }
        return TorrentPageData(description = description, comments = comments)
    }
}
