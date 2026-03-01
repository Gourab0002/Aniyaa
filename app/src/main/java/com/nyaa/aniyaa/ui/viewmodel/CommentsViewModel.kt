package com.nyaa.aniyaa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.repository.NyaaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentsUiState(
    val comments: List<TorrentComment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasFetched: Boolean = false
)

class CommentsViewModel : ViewModel() {

    private val repository = NyaaRepository()

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    fun fetchComments(torrentId: String) {
        if (_uiState.value.hasFetched) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.fetchComments(torrentId)
            result.fold(
                onSuccess = { comments ->
                    _uiState.update { it.copy(isLoading = false, comments = comments, hasFetched = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load comments", hasFetched = true) }
                }
            )
        }
    }
}
