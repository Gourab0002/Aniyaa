package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.work.SavedSearchWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AniyaaApplication
    private val historyRepository = app.historyRepository
    private val savedSearchRepository = app.savedSearchRepository

    val history: StateFlow<List<SearchHistoryEntry>> = historyRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedSearches: StateFlow<List<SavedSearch>> = savedSearchRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addEntry(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            historyRepository.add(SearchParams(query = query))
        }
    }

    fun addEntry(params: SearchParams) {
        viewModelScope.launch { historyRepository.add(params) }
    }

    fun removeEntry(entry: SearchHistoryEntry) {
        viewModelScope.launch { historyRepository.remove(entry) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    fun deleteSavedSearch(id: Long) {
        viewModelScope.launch { savedSearchRepository.delete(id) }
    }

    fun toggleNotify(search: SavedSearch) {
        viewModelScope.launch {
            savedSearchRepository.update(search.copy(notify = !search.notify))
            if (!search.notify) {
                SavedSearchWorker.enqueue(getApplication())
            }
        }
    }

    suspend fun savedSearchById(id: Long): SavedSearch? = savedSearchRepository.getById(id)
}
