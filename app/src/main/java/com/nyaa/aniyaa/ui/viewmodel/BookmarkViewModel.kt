package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.data.api.withMagnet
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.repository.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookmarkRepository(application)

    private val _bookmarks = MutableStateFlow<List<Torrent>>(emptyList())
    val bookmarks: StateFlow<List<Torrent>> = _bookmarks.asStateFlow()

    init {
        loadBookmarks()
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            _bookmarks.value = withContext(Dispatchers.IO) { repository.getBookmarks() }
        }
    }

    fun toggleBookmark(torrent: Torrent) {
        val identity = torrent.identity()
        val stored = torrent.withMagnet()
        val currentlyBookmarked = _bookmarks.value.any { it.identity() == identity }
        val next = if (currentlyBookmarked) {
            _bookmarks.value.filter { it.identity() != identity }
        } else {
            listOf(stored) + _bookmarks.value
        }
        _bookmarks.value = next
        viewModelScope.launch(Dispatchers.IO) {
            if (currentlyBookmarked) {
                repository.removeBookmark(torrent.id.ifEmpty { torrent.infoHash })
            } else {
                repository.addBookmark(stored)
            }
        }
    }

    fun removeBookmark(torrentId: String) {
        _bookmarks.value = _bookmarks.value.filter { it.id != torrentId && it.infoHash != torrentId }
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeBookmark(torrentId)
        }
    }

    fun isBookmarked(torrentId: String): Boolean {
        return _bookmarks.value.any { it.id == torrentId || it.infoHash == torrentId }
    }

    fun torrentByNavId(navId: String): Torrent? =
        _bookmarks.value.find { it.matchesNavId(navId) }
}
