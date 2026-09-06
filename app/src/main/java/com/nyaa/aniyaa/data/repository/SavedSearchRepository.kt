package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.db.toEntity
import com.nyaa.aniyaa.data.model.SavedSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedSearchRepository(private val database: AppDatabase) {

    private val dao = database.savedSearchDao()

    fun observe(): Flow<List<SavedSearch>> = dao.observe().map { list -> list.map { it.toModel() } }

    suspend fun getAll(): List<SavedSearch> = dao.getAll().map { it.toModel() }

    suspend fun getNotifying(): List<SavedSearch> = dao.getNotifying().map { it.toModel() }

    suspend fun getById(id: Long): SavedSearch? = dao.getById(id)?.toModel()

    suspend fun add(search: SavedSearch): Long {
        val entity = search.copy(id = 0).toEntity()
        return dao.insert(entity)
    }

    suspend fun update(search: SavedSearch) {
        dao.update(search.toEntity())
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }

    suspend fun replaceAll(searches: List<SavedSearch>) {
        dao.deleteAll()
        searches.forEach { dao.insert(it.copy(id = 0).toEntity()) }
    }
}
