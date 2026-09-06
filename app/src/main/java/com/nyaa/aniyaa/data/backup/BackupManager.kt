package com.nyaa.aniyaa.data.backup

import com.nyaa.aniyaa.data.db.torrentFromBackupJson
import com.nyaa.aniyaa.data.db.torrentToBackupJson
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.data.repository.BookmarkRepository
import com.nyaa.aniyaa.data.repository.SavedSearchRepository
import com.nyaa.aniyaa.data.repository.SearchHistoryRepository
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val bookmarks: BookmarkRepository,
    private val history: SearchHistoryRepository,
    private val savedSearches: SavedSearchRepository,
    private val prefs: AppPreferences
) {
    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        val bookmarkArray = JSONArray()
        bookmarks.getAll().forEach { bookmarkArray.put(torrentToBackupJson(it)) }
        root.put("bookmarks", bookmarkArray)

        val historyArray = JSONArray()
        history.getAll().forEach { entry ->
            historyArray.put(JSONObject().apply {
                put("query", entry.query)
                put("timestamp", entry.timestamp)
                put("site", entry.site.id)
                put("categoryValue", entry.categoryValue)
                put("filterValue", entry.filterValue)
                put("sortFieldValue", entry.sortFieldValue)
                put("sortOrderValue", entry.sortOrderValue)
            })
        }
        root.put("history", historyArray)

        val savedArray = JSONArray()
        savedSearches.getAll().forEach { search ->
            savedArray.put(JSONObject().apply {
                put("name", search.name)
                put("query", search.query)
                put("site", search.site.id)
                put("categoryValue", search.categoryValue)
                put("filterValue", search.filterValue)
                put("sortFieldValue", search.sortFieldValue)
                put("sortOrderValue", search.sortOrderValue)
                put("notify", search.notify)
                put("lastSeenIds", search.lastSeenIds)
                put("lastCheckedAt", search.lastCheckedAt)
            })
        }
        root.put("savedSearches", savedArray)

        root.put(
            "settings",
            JSONObject().apply {
                put("themeIndex", prefs.themeIndex)
                put("darkMode", prefs.darkMode.value)
                put("currentSite", prefs.currentSite.id)
                put("sukebeiAcknowledged", prefs.sukebeiAcknowledged)
                put("sukebeiEnabled", prefs.sukebeiEnabled)
                put("onboardingComplete", prefs.onboardingComplete)
                put("nyaaBaseUrl", prefs.baseUrl(CatalogSite.NYAA))
                put("sukebeiBaseUrl", prefs.baseUrl(CatalogSite.SUKEBEI))
                put("preferredTorrentPackage", prefs.preferredTorrentPackage)
                put("hideScreenshots", prefs.hideScreenshots)
                put("hideFromRecents", prefs.hideFromRecents)
            }
        )
        return root.toString(2)
    }

    suspend fun importJson(json: String) {
        val root = JSONObject(json)
        val settings = root.optJSONObject("settings")
        val fallbackSite = inferFallbackSite(settings)

        val bookmarkItems = mutableListOf<Torrent>()
        val bookmarkArray = root.optJSONArray("bookmarks") ?: JSONArray()
        for (i in 0 until bookmarkArray.length()) {
            bookmarkItems += torrentFromBackupJson(bookmarkArray.getJSONObject(i), fallbackSite)
        }
        bookmarks.replaceAll(bookmarkItems)

        val historyItems = mutableListOf<SearchHistoryEntry>()
        val historyArray = root.optJSONArray("history") ?: JSONArray()
        for (i in 0 until historyArray.length()) {
            val obj = historyArray.getJSONObject(i)
            historyItems += SearchHistoryEntry(
                query = obj.optString("query"),
                timestamp = obj.optLong("timestamp"),
                site = if (obj.has("site")) CatalogSite.fromId(obj.optString("site")) else fallbackSite,
                categoryValue = obj.optString("categoryValue", "0_0"),
                filterValue = obj.optInt("filterValue", 0),
                sortFieldValue = obj.optString("sortFieldValue", "id"),
                sortOrderValue = obj.optString("sortOrderValue", "desc")
            )
        }
        history.replaceAll(historyItems)

        val savedItems = mutableListOf<SavedSearch>()
        val savedArray = root.optJSONArray("savedSearches") ?: JSONArray()
        for (i in 0 until savedArray.length()) {
            val obj = savedArray.getJSONObject(i)
            savedItems += SavedSearch(
                name = obj.optString("name"),
                query = obj.optString("query"),
                site = if (obj.has("site")) CatalogSite.fromId(obj.optString("site")) else fallbackSite,
                categoryValue = obj.optString("categoryValue", "0_0"),
                filterValue = obj.optInt("filterValue", 0),
                sortFieldValue = obj.optString("sortFieldValue", "id"),
                sortOrderValue = obj.optString("sortOrderValue", "desc"),
                notify = obj.optBoolean("notify"),
                lastSeenIds = obj.optString("lastSeenIds"),
                lastCheckedAt = obj.optLong("lastCheckedAt")
            )
        }
        savedSearches.replaceAll(savedItems)

        if (settings != null) {
            if (settings.has("themeIndex")) prefs.themeIndex = settings.optInt("themeIndex")
            if (settings.has("darkMode")) {
                prefs.darkMode = DarkMode.entries.find { it.value == settings.optString("darkMode") } ?: DarkMode.SYSTEM
            }
            if (settings.has("sukebeiAcknowledged")) {
                prefs.sukebeiAcknowledged = settings.optBoolean("sukebeiAcknowledged")
            }
            if (settings.has("sukebeiEnabled")) {
                prefs.sukebeiEnabled = settings.optBoolean("sukebeiEnabled")
            }
            if (settings.has("onboardingComplete")) {
                prefs.onboardingComplete = settings.optBoolean("onboardingComplete")
            }
            if (settings.has("currentSite")) prefs.currentSite = CatalogSite.fromId(settings.optString("currentSite"))
            if (settings.has("nyaaBaseUrl")) prefs.setBaseUrl(CatalogSite.NYAA, settings.optString("nyaaBaseUrl"))
            if (settings.has("sukebeiBaseUrl")) prefs.setBaseUrl(CatalogSite.SUKEBEI, settings.optString("sukebeiBaseUrl"))
            if (settings.has("baseUrl") && !settings.has("sukebeiBaseUrl") && !settings.has("nyaaBaseUrl")) {
                val url = settings.optString("baseUrl")
                val site = CatalogSite.fromUrl(url) ?: fallbackSite
                prefs.setBaseUrl(site, url)
            }
            if (settings.has("preferredTorrentPackage")) {
                prefs.preferredTorrentPackage = settings.optString("preferredTorrentPackage")
            }
            if (settings.has("hideScreenshots")) prefs.hideScreenshots = settings.optBoolean("hideScreenshots")
            if (settings.has("hideFromRecents")) prefs.hideFromRecents = settings.optBoolean("hideFromRecents")
        }
    }

    private fun inferFallbackSite(settings: JSONObject?): CatalogSite {
        val base = settings?.optString("baseUrl").orEmpty()
        return CatalogSite.fromUrl(base) ?: CatalogSite.NYAA
    }
}
