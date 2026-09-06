package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.api.withMagnet
import com.nyaa.aniyaa.data.model.BookmarkSort
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.repository.filteredAndSorted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AniyaaApplication
    private val repository = app.bookmarkRepository
    private val nyaa = app.nyaaRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(BookmarkSort.DATE_ADDED)
    val sort: StateFlow<BookmarkSort> = _sort.asStateFlow()

    private val _siteFilter = MutableStateFlow<CatalogSite?>(null)
    val siteFilter: StateFlow<CatalogSite?> = _siteFilter.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val allBookmarks: StateFlow<List<Torrent>> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarks: StateFlow<List<Torrent>> = combine(
        repository.observe(),
        _query,
        _sort,
        _siteFilter
    ) { list, query, sort, site ->
        val scoped = if (site == null) list else list.filter { it.site == site }
        scoped.filteredAndSorted(query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(value: String) {
        _query.value = value
    }

    fun updateSort(value: BookmarkSort) {
        _sort.value = value
    }

    fun updateSiteFilter(site: CatalogSite?) {
        _siteFilter.value = site
    }

    fun toggleBookmark(torrent: Torrent) {
        viewModelScope.launch {
            val key = torrent.bookmarkKey()
            val currently = allBookmarks.value.any { it.bookmarkKey() == key }
            if (currently) {
                repository.remove(torrent)
            } else {
                repository.add(torrent.withMagnet())
            }
        }
    }

    fun removeBookmark(torrent: Torrent) {
        viewModelScope.launch { repository.remove(torrent) }
    }

    fun isBookmarked(torrent: Torrent): Boolean {
        val key = torrent.bookmarkKey()
        return allBookmarks.value.any { it.bookmarkKey() == key }
    }

    fun torrentByNavId(navId: String, site: CatalogSite? = null): Torrent? =
        allBookmarks.value.find {
            it.matchesNavId(navId) && (site == null || it.site == site)
        }

    fun refreshStats() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val updated = AtomicInteger(0)
            val failed = AtomicInteger(0)
            val semaphore = Semaphore(3)
            try {
                coroutineScope {
                    allBookmarks.value.map { torrent ->
                        async {
                            val id = torrent.id
                            if (id.isBlank()) return@async
                            semaphore.withPermit {
                                try {
                                    nyaa.fetchTorrent(id, torrent, torrent.site).onSuccess {
                                        repository.update(it.copy(addedAt = torrent.addedAt, site = torrent.site))
                                        updated.incrementAndGet()
                                    }.onFailure {
                                        failed.incrementAndGet()
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    failed.incrementAndGet()
                                }
                            }
                        }
                    }.awaitAll()
                }
            } finally {
                _refreshing.value = false
            }
            val updatedCount = updated.get()
            val failedCount = failed.get()
            _message.value = when {
                updatedCount > 0 && failedCount == 0 ->
                    "Updated $updatedCount bookmark${if (updatedCount == 1) "" else "s"}"
                updatedCount > 0 -> "Updated $updatedCount, $failedCount failed"
                failedCount > 0 -> "Could not refresh bookmarks"
                else -> "No bookmarks to refresh"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
