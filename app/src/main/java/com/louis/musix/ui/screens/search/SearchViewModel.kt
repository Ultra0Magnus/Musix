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

// ─── État de l'écran de recherche ──────────────────────────────────────────────

sealed class SearchUiState {
    /** Aucune recherche en cours (état initial) */
    data object Idle : SearchUiState()
    /** Recherche en cours : on montre un spinner */
    data object Loading : SearchUiState()
    /** Résultats reçus */
    data class Success(val songs: List<Song>) : SearchUiState()
    /** Erreur réseau ou YouTube */
    data class Error(val message: String) : SearchUiState()
}

// ─── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel de l'écran de recherche.
 *
 * - Expose [uiState] (StateFlow) : la UI l'observe et se recompose automatiquement.
 * - Expose [query] : le texte saisi dans le TextField.
 * - [onQueryChange] : appelé à chaque frappe.
 * - [onSearch] : appelé quand l'utilisateur valide (touche Entrée ou bouton).
 */
class SearchViewModel(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Référence au Job en cours — permet d'annuler si l'utilisateur relance une recherche
    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onSearch() {
        val q = _query.value.trim()
        if (q.isEmpty()) return

        // Annule la recherche précédente si elle tourne encore
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val results = repository.search(q)
                _uiState.value = if (results.isEmpty()) {
                    SearchUiState.Error("Aucun résultat pour « $q »")
                } else {
                    SearchUiState.Success(results)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    "Erreur : ${e.localizedMessage ?: "problème réseau"}"
                )
            }
        }
    }
}
