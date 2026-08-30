package com.nyaa.aniyaa.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SearchHistoryEntry(
    val query: String,
    val timestamp: Long
)

class SearchHistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HISTORY = "history_list"
        private const val MAX_HISTORY_SIZE = 50
    }

    fun getHistory(): List<SearchHistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return parseHistory(json)
    }

    fun addEntry(query: String) {
        if (query.isBlank()) return
        val trimmedQuery = query.trim()
        val history = getHistory().toMutableList()
        history.removeAll { it.query.equals(trimmedQuery, ignoreCase = true) }
        history.add(0, SearchHistoryEntry(query = trimmedQuery, timestamp = System.currentTimeMillis()))
        val trimmed = if (history.size > MAX_HISTORY_SIZE) history.take(MAX_HISTORY_SIZE) else history
        saveHistory(trimmed)
    }

    fun removeEntry(query: String) {
        val trimmedQuery = query.trim()
        val history = getHistory().filter { !it.query.equals(trimmedQuery, ignoreCase = true) }
        saveHistory(history)
    }

    fun clearHistory() {
        prefs.edit().putString(KEY_HISTORY, "[]").apply()
    }

    private fun parseHistory(json: String): List<SearchHistoryEntry> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                when (val item = array.get(i)) {
                    is JSONObject -> SearchHistoryEntry(
                        query = item.optString("query", ""),
                        timestamp = item.optLong("timestamp", 0L)
                    ).takeIf { it.query.isNotBlank() }
                    is String -> item.takeIf { it.isNotBlank() }?.let {
                        SearchHistoryEntry(query = it, timestamp = 0L)
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(history: List<SearchHistoryEntry>) {
        val array = JSONArray()
        history.forEach { entry ->
            array.put(JSONObject().apply {
                put("query", entry.query)
                put("timestamp", entry.timestamp)
            })
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }
}
