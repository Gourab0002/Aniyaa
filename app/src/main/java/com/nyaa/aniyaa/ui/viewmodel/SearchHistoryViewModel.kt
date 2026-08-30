package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.data.repository.SearchHistoryEntry
import com.nyaa.aniyaa.data.repository.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SearchHistoryRepository(application)

    private val _history = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())
    val history: StateFlow<List<SearchHistoryEntry>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _history.value = withContext(Dispatchers.IO) { repository.getHistory() }
        }
    }

    fun addEntry(query: String) {
        viewModelScope.launch {
            _history.value = withContext(Dispatchers.IO) {
                repository.addEntry(query)
                repository.getHistory()
            }
        }
    }

    fun removeEntry(query: String) {
        viewModelScope.launch {
            _history.value = withContext(Dispatchers.IO) {
                repository.removeEntry(query)
                repository.getHistory()
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _history.value = withContext(Dispatchers.IO) {
                repository.clearHistory()
                repository.getHistory()
            }
        }
    }
}
