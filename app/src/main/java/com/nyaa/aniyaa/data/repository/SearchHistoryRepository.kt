package com.nyaa.aniyaa.data.repository

import android.content.Context
import org.json.JSONArray

class SearchHistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    fun getHistory(): List<String> {
        val json = prefs.getString("history_list", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        val history = getHistory().filter { it != query }.toMutableList()
        history.add(0, query)
        saveHistory(history.take(MAX_HISTORY))
    }

    fun removeFromHistory(query: String) {
        saveHistory(getHistory().filter { it != query })
    }

    fun clearHistory() {
        prefs.edit().remove("history_list").apply()
    }

    private fun saveHistory(history: List<String>) {
        val array = JSONArray()
        history.forEach { array.put(it) }
        prefs.edit().putString("history_list", array.toString()).apply()
    }

    companion object {
        private const val MAX_HISTORY = 20
    }
}
