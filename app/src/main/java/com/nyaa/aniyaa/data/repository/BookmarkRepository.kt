package com.nyaa.aniyaa.data.repository

import android.content.Context
import com.nyaa.aniyaa.data.model.Torrent
import org.json.JSONArray
import org.json.JSONObject

class BookmarkRepository(context: Context) {

    private val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)

    fun getBookmarks(): List<Torrent> {
        val json = prefs.getString("bookmarks_list", "[]") ?: "[]"
        return parseBookmarks(json)
    }

    fun addBookmark(torrent: Torrent) {
        val bookmarks = getBookmarks().toMutableList()
        if (bookmarks.none { it.id == torrent.id }) {
            bookmarks.add(0, torrent)
            saveBookmarks(bookmarks)
        }
    }

    fun removeBookmark(torrentId: String) {
        val bookmarks = getBookmarks().filter { it.id != torrentId }
        saveBookmarks(bookmarks)
    }

    fun isBookmarked(torrentId: String): Boolean {
        return getBookmarks().any { it.id == torrentId }
    }

    private fun torrentToJson(torrent: Torrent): JSONObject = JSONObject().apply {
        put("id", torrent.id)
        put("title", torrent.title)
        put("link", torrent.link)
        put("guid", torrent.guid)
        put("pubDate", torrent.pubDate)
        put("seeders", torrent.seeders)
        put("leechers", torrent.leechers)
        put("downloads", torrent.downloads)
        put("infoHash", torrent.infoHash)
        put("category", torrent.category)
        put("size", torrent.size)
        put("comments", torrent.comments)
        put("trusted", torrent.trusted)
        put("remake", torrent.remake)
        put("magnetLink", torrent.magnetLink)
    }

    private fun torrentFromJson(obj: JSONObject): Torrent = Torrent(
        id = obj.optString("id"),
        title = obj.optString("title"),
        link = obj.optString("link"),
        guid = obj.optString("guid"),
        pubDate = obj.optString("pubDate"),
        seeders = obj.optInt("seeders"),
        leechers = obj.optInt("leechers"),
        downloads = obj.optInt("downloads"),
        infoHash = obj.optString("infoHash"),
        category = obj.optString("category"),
        size = obj.optString("size"),
        comments = obj.optInt("comments"),
        trusted = obj.optBoolean("trusted"),
        remake = obj.optBoolean("remake"),
        magnetLink = obj.optString("magnetLink")
    )

    private fun parseBookmarks(json: String): List<Torrent> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { torrentFromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveBookmarks(bookmarks: List<Torrent>) {
        val array = JSONArray()
        bookmarks.forEach { array.put(torrentToJson(it)) }
        prefs.edit().putString("bookmarks_list", array.toString()).apply()
    }
}
