package com.louis.musix.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page

// ─── Search screen state ──────────────────────────────────────────────────────

sealed class SearchUiState {
    data object Idle    : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(
        val songs:         List<Song>,
        val nextPage:      Page?   = null,
        val query:         String  = "",
        val isLoadingMore: Boolean = false,
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class SearchViewModel(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Session history — last 10 searches, no duplicates. */
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    /**
     * Clears the query and returns to [SearchUiState.Idle],
     * which shows the recent search history list.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _query.value   = ""
        _uiState.value = SearchUiState.Idle
    }

    fun onSearch() {
        val q = _query.value.trim()
        if (q.isEmpty()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val result = repository.searchPaged(q)
                if (result.songs.isEmpty()) {
                    _uiState.value = SearchUiState.Error("No results for \"$q\"")
                } else {
                    // Add to history (no duplicates, max 10)
                    _searchHistory.value = listOf(q) +
                        _searchHistory.value.filter { it != q }.take(9)
                    _uiState.value = SearchUiState.Success(
                        songs    = result.songs,
                        nextPage = result.nextPage,
                        query    = result.query,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    "Error: ${e.localizedMessage ?: "network issue"}"
                )
            }
        }
    }

    /** Loads the next page — does nothing if already loading or no next page. */
    fun loadMore() {
        val current = _uiState.value as? SearchUiState.Success ?: return
        val nextPage = current.nextPage ?: return
        if (current.isLoadingMore) return

        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            try {
                val more = repository.searchMore(current.query, nextPage)
                _uiState.value = SearchUiState.Success(
                    songs         = current.songs + more.songs,
                    nextPage      = more.nextPage,
                    query         = more.query,
                    isLoadingMore = false,
                )
            } catch (_: Exception) {
                // Restore previous state without the spinner
                _uiState.value = current.copy(isLoadingMore = false)
            }
        }
    }

    /** Re-runs a search from history. */
    fun searchFromHistory(query: String) {
        _query.value = query
        onSearch()
    }
}
