package com.nyaa.aniyaa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.data.model.CATEGORIES
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.repository.NyaaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val torrents: List<Torrent> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val searchParams: SearchParams = SearchParams(),
    val hasSearched: Boolean = false
)

class SearchViewModel : ViewModel() {

    private val repository = NyaaRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, searchParams = it.searchParams.copy(query = query)) }
    }

    fun updateCategory(category: Category) {
        _uiState.update { it.copy(searchParams = it.searchParams.copy(category = category)) }
    }

    fun updateFilter(filter: FilterOption) {
        _uiState.update { it.copy(searchParams = it.searchParams.copy(filter = filter)) }
    }

    fun updateSortField(sortField: SortField) {
        _uiState.update { it.copy(searchParams = it.searchParams.copy(sortField = sortField)) }
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        _uiState.update { it.copy(searchParams = it.searchParams.copy(sortOrder = sortOrder)) }
    }

    fun resetFilters() {
        _uiState.update { state ->
            state.copy(
                searchParams = SearchParams(query = state.query)
            )
        }
    }

    fun search() {
        val params = _uiState.value.searchParams.copy(page = 1)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, searchParams = params, canLoadMore = true) }
            val result = repository.search(params)
            result.fold(
                onSuccess = { torrents ->
                    _uiState.update { it.copy(isLoading = false, torrents = torrents, hasSearched = true, canLoadMore = torrents.isNotEmpty()) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error", hasSearched = true) }
                }
            )
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        val nextPage = state.searchParams.page + 1
        val params = state.searchParams.copy(page = nextPage)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            val result = repository.search(params)
            result.fold(
                onSuccess = { torrents ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            torrents = it.torrents + torrents,
                            searchParams = params,
                            canLoadMore = torrents.isNotEmpty()
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoadingMore = false, error = e.message ?: "Unknown error") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
