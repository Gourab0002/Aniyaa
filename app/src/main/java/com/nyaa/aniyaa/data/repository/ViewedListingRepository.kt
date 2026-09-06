package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.api.withMagnet
import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.db.toViewedListingEntity
import com.nyaa.aniyaa.data.model.Torrent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ViewedListingRepository(private val database: AppDatabase) {

    private val dao = database.viewedListingDao()

    fun observe(): Flow<List<Torrent>> = dao.observe().map { list -> list.map { it.toTorrent() } }

    suspend fun getAll(): List<Torrent> = dao.getAll().map { it.toTorrent() }

    suspend fun add(torrent: Torrent) {
        if (torrent.id.isBlank() && torrent.infoHash.isBlank()) return
        dao.upsert(torrent.withMagnet().toViewedListingEntity())
        dao.trim(MAX_VIEWED)
    }

    suspend fun remove(torrent: Torrent) {
        dao.delete(torrent.bookmarkKey())
    }

    suspend fun clear() {
        dao.deleteAll()
    }

    suspend fun replaceAll(torrents: List<Torrent>) {
        dao.deleteAll()
        if (torrents.isNotEmpty()) {
            dao.upsertAll(
                torrents.mapIndexed { index, torrent ->
                    torrent.withMagnet().toViewedListingEntity(
                        viewedAt = if (torrent.addedAt > 0L) torrent.addedAt else System.currentTimeMillis() - index
                    )
                }
            )
            dao.trim(MAX_VIEWED)
        }
    }

    suspend fun merge(torrents: List<Torrent>) {
        torrents.forEach { add(it) }
    }

    companion object {
        const val MAX_VIEWED = 40
    }
}
