package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.db.toEntity
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.toHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchHistoryRepository(private val database: AppDatabase) {

    private val dao = database.historyDao()

    fun observe(): Flow<List<SearchHistoryEntry>> = dao.observe().map { list -> list.map { it.toEntry() } }

    suspend fun getAll(): List<SearchHistoryEntry> = dao.getAll().map { it.toEntry() }

    suspend fun add(params: SearchParams) {
        val query = params.query.trim()
        if (query.isBlank()) return
        dao.deleteByQuery(params.site.id, query)
        dao.insert(params.toHistoryEntry().toEntity())
        dao.trim(params.site.id, MAX_HISTORY_SIZE)
    }

    suspend fun add(entry: SearchHistoryEntry) {
        if (entry.query.isBlank()) return
        dao.deleteByQuery(entry.site.id, entry.query.trim())
        dao.insert(entry.copy(query = entry.query.trim()).toEntity())
        dao.trim(entry.site.id, MAX_HISTORY_SIZE)
    }

    suspend fun remove(entry: SearchHistoryEntry) {
        dao.deleteByQuery(entry.site.id, entry.query.trim())
    }

    suspend fun remove(query: String, siteId: String) {
        dao.deleteByQuery(siteId, query.trim())
    }

    suspend fun clear() {
        dao.deleteAll()
    }

    suspend fun replaceAll(entries: List<SearchHistoryEntry>) {
        dao.deleteAll()
        entries.forEach { dao.insert(it.toEntity()) }
        entries.map { it.site.id }.distinct().forEach { siteId ->
            dao.trim(siteId, MAX_HISTORY_SIZE)
        }
    }

    suspend fun merge(entries: List<SearchHistoryEntry>) {
        entries.sortedBy { it.timestamp }.forEach { add(it) }
    }

    companion object {
        const val MAX_HISTORY_SIZE = 50
    }
}
