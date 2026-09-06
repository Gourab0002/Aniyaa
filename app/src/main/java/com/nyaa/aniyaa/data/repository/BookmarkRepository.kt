package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.api.withMagnet
import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.db.toBookmarkEntity
import com.nyaa.aniyaa.data.model.BookmarkSort
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.util.parseSizeBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(private val database: AppDatabase) {

    private val dao = database.bookmarkDao()

    fun observe(): Flow<List<Torrent>> = dao.observe().map { list -> list.map { it.toTorrent() } }

    suspend fun getAll(): List<Torrent> = dao.getAll().map { it.toTorrent() }

    suspend fun add(torrent: Torrent) {
        dao.upsert(torrent.withMagnet().toBookmarkEntity())
    }

    suspend fun remove(torrent: Torrent) {
        dao.delete(torrent.bookmarkKey(), torrent.site.id)
    }

    suspend fun update(torrent: Torrent) {
        val existing = dao.getAll().find { it.identity == torrent.bookmarkKey() }
        dao.upsert(torrent.withMagnet().toBookmarkEntity(addedAt = existing?.addedAt ?: torrent.addedAt))
    }

    suspend fun replaceAll(torrents: List<Torrent>) {
        dao.deleteAll()
        if (torrents.isNotEmpty()) {
            dao.upsertAll(
                torrents.mapIndexed { index, torrent ->
                    torrent.withMagnet().toBookmarkEntity(
                        addedAt = if (torrent.addedAt > 0L) torrent.addedAt else System.currentTimeMillis() - index
                    )
                }
            )
        }
    }

    suspend fun merge(torrents: List<Torrent>) {
        if (torrents.isEmpty()) return
        val existing = dao.getAll().map { it.identity }.toSet()
        val incoming = torrents
            .filter { it.bookmarkKey() !in existing }
            .mapIndexed { index, torrent ->
                torrent.withMagnet().toBookmarkEntity(
                    addedAt = if (torrent.addedAt > 0L) torrent.addedAt else System.currentTimeMillis() - index
                )
            }
        if (incoming.isNotEmpty()) dao.upsertAll(incoming)
    }
}

fun List<Torrent>.filteredAndSorted(query: String, sort: BookmarkSort): List<Torrent> {
    val needle = query.trim()
    val filtered = if (needle.isEmpty()) {
        this
    } else {
        filter {
            it.title.contains(needle, ignoreCase = true) ||
                it.category.contains(needle, ignoreCase = true) ||
                it.submitter.contains(needle, ignoreCase = true) ||
                it.site.displayName.contains(needle, ignoreCase = true)
        }
    }
    return when (sort) {
        BookmarkSort.DATE_ADDED -> filtered.sortedByDescending { it.addedAt }
        BookmarkSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
        BookmarkSort.SIZE -> filtered.sortedByDescending { parseSizeBytes(it.size) ?: 0L }
        BookmarkSort.SEEDERS -> filtered.sortedByDescending { it.seeders }
    }
}
