package com.nyaa.aniyaa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.repository.NyaaRepository
import com.nyaa.aniyaa.data.repository.mergeSearchPages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
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

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var requestGeneration: Long = 0L

    fun updateQuery(query: String) {
        _query.value = query
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
            state.copy(searchParams = SearchParams(query = _query.value))
        }
    }

    fun search() {
        loadMoreJob?.cancel()
        searchJob?.cancel()

        val params = _uiState.value.searchParams.copy(query = _query.value, page = 1)
        val generation = ++requestGeneration

        _uiState.update {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                error = null,
                searchParams = params,
                canLoadMore = true
            )
        }

        searchJob = viewModelScope.launch {
            val result = try {
                repository.search(params)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (generation != requestGeneration) return@launch
            result.fold(
                onSuccess = { torrents ->
                    val (merged, canLoadMore) = mergeSearchPages(emptyList(), torrents, replace = true)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            torrents = merged,
                            hasSearched = true,
                            canLoadMore = canLoadMore,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    if (e is CancellationException) return@fold
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error",
                            hasSearched = true
                        )
                    }
                }
            )
        }
    }

    fun loadNextPage() {
        if (searchJob?.isActive == true || loadMoreJob?.isActive == true) return

        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return

        val nextPage = state.searchParams.page + 1
        val params = state.searchParams.copy(page = nextPage)
        val generation = requestGeneration

        _uiState.update { it.copy(isLoadingMore = true, error = null) }

        loadMoreJob = viewModelScope.launch {
            val result = try {
                repository.search(params)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (generation != requestGeneration) return@launch
            result.fold(
                onSuccess = { torrents ->
                    _uiState.update { current ->
                        val (merged, canLoadMore) = mergeSearchPages(
                            existing = current.torrents,
                            incoming = torrents,
                            replace = false
                        )
                        current.copy(
                            isLoadingMore = false,
                            torrents = merged,
                            searchParams = params,
                            canLoadMore = canLoadMore
                        )
                    }
                },
                onFailure = { e ->
                    if (e is CancellationException) return@fold
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = e.message ?: "Unknown error"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun torrentByNavId(navId: String): Torrent? =
        _uiState.value.torrents.find { it.matchesNavId(navId) }
}
