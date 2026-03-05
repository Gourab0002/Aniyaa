package com.nyaa.aniyaa.data.api

import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.model.TorrentFileEntry
import com.nyaa.aniyaa.data.model.TorrentPageData
import org.jsoup.Jsoup

object NyaaCommentParser {

    fun parse(html: String): TorrentPageData {
        val doc = Jsoup.parse(html)

        // div#torrent-description stores raw markdown text (HTML-escaped, &#10; for newlines).
        // Use .wholeText() to preserve newlines required for markdown tables and paragraphs.
        val descriptionEl = doc.selectFirst("div#torrent-description")
        val description = descriptionEl?.wholeText()?.trim() ?: ""

        // nyaa.si real comment structure (from nyaa open-source view.html + Nyaa-Api-Go/Ts):
        // <div class="comment-panel panel-default" id="com-N">
        //   <div class="panel-body">
        //     <div class="col-md-2">
        //       <a href="/user/username">username</a>
        //       <img class="avatar" src="..." />
        //     </div>
        //     <div class="col-md-10 comment">
        //       <div class="row comment-details">
        //         <a href="#com-N"><small data-timestamp-swap>timestamp</small></a>
        //       </div>
        //       <div class="row comment-body">
        //         <div markdown-text class="comment-content">raw markdown text</div>
        //       </div>
        //     </div>
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
            // Timestamp is inside a <small> child of the anchor in div.comment-details.
            // Find("a").children().first() matches the <small> inside the timestamp <a>.
            val date = links.asSequence()
                .flatMap { it.children().asSequence() }
                .firstOrNull()
                ?.text()?.trim() ?: ""
            // comment-content also stores raw markdown text — use .wholeText() to preserve
            // newlines for tables and paragraphs, same as description
            val contentEl = element.selectFirst("div.comment-body div.comment-content")
            val content = contentEl?.wholeText()?.trim() ?: ""
            // comment-panel has id="com-N" (loop index assigned by nyaa)
            val id = element.attr("id").removePrefix("com-")
            if (content.isNotEmpty()) {
                comments.add(TorrentComment(id = id, username = username, avatarUrl = avatarUrl, date = date, content = content))
            }
        }

        // nyaa.si file list structure (from nyaa open-source view.html):
        // <div class="torrent-file-list panel panel-danger">
        //   <p class="panel-heading">File list</p>
        //   <ul>
        //     <li class="torrent-file-list-folder">
        //       folderName
        //       <ul>
        //         <li>filename.ext <span class="pull-right">800.0 MiB</span></li>
        //       </ul>
        //     </li>
        //     <li>filename.ext <span class="pull-right">200.0 MiB</span></li>
        //   </ul>
        // </div>
        // Use li:not(:has(ul)) to select only leaf (file) entries, skipping folder <li>s.
        val fileEntries = mutableListOf<TorrentFileEntry>()
        val fileListEl = doc.selectFirst("div.torrent-file-list")
        if (fileListEl != null) {
            val fileLis = fileListEl.select("li:not(:has(ul))")
            for (li in fileLis) {
                val size = li.selectFirst("span.pull-right")?.text()?.trim() ?: ""
                val liClone = li.clone()
                liClone.selectFirst("span.pull-right")?.remove()
                val name = liClone.text().trim()
                if (name.isNotEmpty()) {
                    fileEntries.add(TorrentFileEntry(name = name, size = size))
                }
            }
        }

        return TorrentPageData(description = description, fileList = fileEntries, comments = comments)
    }
}
