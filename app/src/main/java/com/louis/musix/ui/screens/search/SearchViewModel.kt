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

// ─── État de l'écran de recherche ──────────────────────────────────────────────

sealed class SearchUiState {
    data object Idle    : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(
        val songs:         List<Song>,
        val nextPage:      Page?   = null,
        val searchUrl:     String  = "",
        val isLoadingMore: Boolean = false,
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

// ─── ViewModel ─────────────────────────────────────────────────────────────────

class SearchViewModel(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Historique de session — 10 dernières recherches, pas de doublons. */
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
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
                    _uiState.value = SearchUiState.Error("Aucun résultat pour « $q »")
                } else {
                    // Ajouter à l'historique (pas de doublon, max 10)
                    _searchHistory.value = listOf(q) +
                        _searchHistory.value.filter { it != q }.take(9)
                    _uiState.value = SearchUiState.Success(
                        songs     = result.songs,
                        nextPage  = result.nextPage,
                        searchUrl = result.searchUrl,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    "Erreur : ${e.localizedMessage ?: "problème réseau"}"
                )
            }
        }
    }

    /** Charge la page suivante — ne fait rien si déjà en chargement ou pas de page suivante. */
    fun loadMore() {
        val current = _uiState.value as? SearchUiState.Success ?: return
        val nextPage = current.nextPage ?: return
        if (current.isLoadingMore) return

        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            try {
                val more = repository.searchMore(current.searchUrl, nextPage)
                _uiState.value = SearchUiState.Success(
                    songs         = current.songs + more.songs,
                    nextPage      = more.nextPage,
                    searchUrl     = more.searchUrl,
                    isLoadingMore = false,
                )
            } catch (_: Exception) {
                // On remet l'état précédent sans le spinner
                _uiState.value = current.copy(isLoadingMore = false)
            }
        }
    }

    /** Relance une recherche depuis l'historique. */
    fun searchFromHistory(query: String) {
        _query.value = query
        onSearch()
    }
}
