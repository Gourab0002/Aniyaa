package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.model.sortFieldByValue
import com.nyaa.aniyaa.data.model.sortOrderByValue
import com.nyaa.aniyaa.data.model.toSavedSearch
import com.nyaa.aniyaa.data.network.SiteConfig
import com.nyaa.aniyaa.data.network.toUserMessage
import com.nyaa.aniyaa.data.repository.mergeSearchPages
import com.nyaa.aniyaa.work.SavedSearchWorker
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
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val searchParams: SearchParams = SearchParams(),
    val hasSearched: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AniyaaApplication
    private val repository = app.nyaaRepository
    private val prefs = app.prefs
    private val historyRepository = app.historyRepository
    private val savedSearchRepository = app.savedSearchRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState(searchParams = prefs.defaultSearchParams()))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var requestGeneration: Long = 0L
    private val siteSnapshots = mutableMapOf<CatalogSite, SiteSnapshot>()

    init {
        if (!prefs.sukebeiEnabled && _uiState.value.searchParams.site.nsfw) {
            val nyaa = prefs.defaultSearchParams(CatalogSite.NYAA)
            prefs.currentSite = CatalogSite.NYAA
            SiteConfig.currentSite = CatalogSite.NYAA
            _uiState.update { it.copy(searchParams = nyaa) }
        }
        restoreFromCache()
    }

    private data class SiteSnapshot(
        val query: String,
        val state: SearchUiState
    )

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

    fun switchSite(site: CatalogSite) {
        if (site.nsfw && !prefs.sukebeiEnabled) return
        val current = _uiState.value.searchParams.site
        if (current == site) return
        siteSnapshots[current] = SiteSnapshot(_query.value, _uiState.value)
        prefs.currentSite = site
        SiteConfig.currentSite = site
        val restored = siteSnapshots[site]
        if (restored != null) {
            _query.value = restored.query
            _uiState.value = restored.state.copy(
                searchParams = restored.state.searchParams.copy(site = site).withValidCategory()
            )
            if (restored.state.torrents.isEmpty() && (prefs.loadLatestOnStart || restored.query.isNotBlank())) {
                search(recordHistory = false)
            }
            return
        }
        val defaults = prefs.defaultSearchParams(site).copy(query = _query.value)
        _uiState.update {
            it.copy(
                searchParams = defaults,
                torrents = emptyList(),
                hasSearched = false,
                isLoading = false,
                isRefreshing = false,
                error = null,
                canLoadMore = true
            )
        }
        if (_query.value.isNotBlank()) {
            search()
        } else {
            restoreFromCache()
        }
    }

    fun applyParams(params: SearchParams, recordHistory: Boolean = true) {
        val valid = params.withValidCategory()
        val current = _uiState.value.searchParams.site
        if (valid.site != current) {
            siteSnapshots[current] = SiteSnapshot(_query.value, _uiState.value)
        }
        if (valid.site.nsfw && !prefs.sukebeiEnabled) {
            prefs.sukebeiEnabled = true
        }
        if (valid.site != prefs.currentSite) {
            prefs.currentSite = valid.site
            SiteConfig.currentSite = valid.site
        }
        _query.value = valid.query
        _uiState.update { it.copy(searchParams = valid.copy(page = 1)) }
        search(recordHistory = recordHistory)
    }

    fun applyHistory(entry: SearchHistoryEntry) {
        applyParams(entry.toSearchParams())
    }

    fun applySavedSearch(search: SavedSearch) {
        applyParams(search.toSearchParams())
    }

    fun resetFilters() {
        val site = _uiState.value.searchParams.site
        val defaults = SearchParams(
            query = _query.value,
            site = site,
            category = prefs.defaultCategory(site),
            filter = FilterOption.ALL,
            sortField = sortFieldByValue(prefs.defaultSortFieldValue(site)),
            sortOrder = sortOrderByValue(prefs.defaultSortOrderValue(site))
        )
        _uiState.update { it.copy(searchParams = defaults) }
    }

    private fun restoreFromCache() {
        val params = _uiState.value.searchParams.copy(query = _query.value, page = 1).withValidCategory()
        viewModelScope.launch {
            val cached = runCatching { repository.search(params, fromCache = true) }
                .getOrNull()
                ?.getOrNull()
                .orEmpty()
            if (cached.isNotEmpty()) {
                val (merged, canLoadMore) = mergeSearchPages(emptyList(), cached, replace = true)
                _uiState.update {
                    it.copy(
                        torrents = merged,
                        hasSearched = true,
                        isLoading = false,
                        canLoadMore = canLoadMore,
                        searchParams = params,
                        error = null
                    )
                }
            }
            if (prefs.loadLatestOnStart || cached.isNotEmpty() || params.query.isNotBlank()) {
                search(recordHistory = false)
            }
        }
    }

    fun search(recordHistory: Boolean = true, forceNetwork: Boolean = false) {
        loadMoreJob?.cancel()
        searchJob?.cancel()

        val params = _uiState.value.searchParams.copy(query = _query.value, page = 1).withValidCategory()
        val generation = ++requestGeneration
        prefs.persistFilters(params)

        val hadResults = _uiState.value.torrents.isNotEmpty() &&
            _uiState.value.searchParams.site == params.site
        _uiState.update {
            it.copy(
                isLoading = !hadResults,
                isRefreshing = hadResults,
                isLoadingMore = false,
                error = null,
                searchParams = params,
                canLoadMore = true
            )
        }

        if (recordHistory && params.query.isNotBlank()) {
            viewModelScope.launch {
                historyRepository.add(params)
            }
        }

        searchJob = viewModelScope.launch {
            if (!forceNetwork && !hadResults) {
                val cached = runCatching { repository.search(params, fromCache = true) }.getOrNull()
                val cachedList = cached?.getOrNull().orEmpty()
                if (generation != requestGeneration) return@launch
                if (cachedList.isNotEmpty()) {
                    val (merged, canLoadMore) = mergeSearchPages(emptyList(), cachedList, replace = true)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = true,
                            torrents = merged,
                            hasSearched = true,
                            canLoadMore = canLoadMore,
                            error = null
                        )
                    }
                }
            }

            val result = try {
                repository.search(params, forceNetwork = forceNetwork)
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
                            isRefreshing = false,
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
                            isRefreshing = false,
                            error = e.toUserMessage(),
                            hasSearched = true
                        )
                    }
                }
            )
        }
    }

    fun refresh() = search(recordHistory = false, forceNetwork = true)

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
                            error = e.toUserMessage()
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun torrentByNavId(navId: String, site: CatalogSite? = null): Torrent? =
        _uiState.value.torrents.find {
            it.matchesNavId(navId) && (site == null || it.site == site)
        }

    suspend fun saveCurrentSearch(name: String, notify: Boolean): Long {
        val params = _uiState.value.searchParams.copy(query = _query.value)
        return saveSearch(params, name, notify)
    }

    suspend fun saveSearch(params: SearchParams, name: String, notify: Boolean): Long {
        val id = savedSearchRepository.add(params.toSavedSearch(name, notify))
        if (notify) {
            SavedSearchWorker.enqueue(getApplication(), replace = true)
        }
        return id
    }
}
