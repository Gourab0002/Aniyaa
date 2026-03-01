package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookmarkRepository(application)

    private val _bookmarks = MutableStateFlow<List<Torrent>>(emptyList())
    val bookmarks: StateFlow<List<Torrent>> = _bookmarks.asStateFlow()

    init {
        loadBookmarks()
    }

    private fun loadBookmarks() {
        _bookmarks.update { repository.getBookmarks() }
    }

    fun toggleBookmark(torrent: Torrent) {
        if (repository.isBookmarked(torrent.id)) {
            repository.removeBookmark(torrent.id)
        } else {
            repository.addBookmark(torrent)
        }
        loadBookmarks()
    }

    fun removeBookmark(torrentId: String) {
        repository.removeBookmark(torrentId)
        loadBookmarks()
    }

    fun isBookmarked(torrentId: String): Boolean {
        return _bookmarks.value.any { it.id == torrentId }
    }
}
