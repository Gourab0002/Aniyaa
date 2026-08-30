package com.nyaa.aniyaa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.model.TorrentFileEntry
import com.nyaa.aniyaa.data.repository.NyaaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentsUiState(
    val torrentId: String = "",
    val description: String = "",
    val fileList: List<TorrentFileEntry> = emptyList(),
    val comments: List<TorrentComment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasFetched: Boolean = false
)

class CommentsViewModel : ViewModel() {

    private val repository = NyaaRepository()

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    fun fetchComments(torrentId: String) {
        val state = _uiState.value
        if (state.hasFetched && state.torrentId == torrentId && state.error == null) return

        fetchJob?.cancel()
        _uiState.update {
            it.copy(
                torrentId = torrentId,
                isLoading = true,
                error = null,
                hasFetched = false
            )
        }

        fetchJob = viewModelScope.launch {
            val result = try {
                repository.fetchTorrentPageData(torrentId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            result.fold(
                onSuccess = { pageData ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            description = pageData.description,
                            fileList = pageData.fileList,
                            comments = pageData.comments,
                            hasFetched = true,
                            error = null,
                            torrentId = torrentId
                        )
                    }
                },
                onFailure = { e ->
                    if (e is CancellationException) return@fold
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load details",
                            hasFetched = false,
                            torrentId = torrentId
                        )
                    }
                }
            )
        }
    }

    fun retry(torrentId: String) {
        _uiState.update { it.copy(hasFetched = false, error = null) }
        fetchComments(torrentId)
    }
}
