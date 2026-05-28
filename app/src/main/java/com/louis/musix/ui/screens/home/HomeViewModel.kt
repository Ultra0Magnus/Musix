package com.louis.musix.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Etat du carrousel "Tendances" — chargement asynchrone via NewPipeExtractor.
 */
sealed interface TrendingState {
    data object Loading : TrendingState
    data class Success(val songs: List<Song>) : TrendingState
    data class Error(val message: String) : TrendingState
}

class HomeViewModel(
    private val youtubeRepo: YouTubeRepository,
    private val libraryRepo: LibraryRepository,
) : ViewModel() {

    // Carrousels locaux : viennent de Room, instantanes, limites a 10
    val recentlyPlayed: StateFlow<List<Song>> = libraryRepo.history
        .map { it.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<Song>> = libraryRepo.favoriteSongs
        .map { it.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Carrousel reseau : tendances YouTube, fetch one-shot au demarrage
    private val _trending = MutableStateFlow<TrendingState>(TrendingState.Loading)
    val trending: StateFlow<TrendingState> = _trending.asStateFlow()

    init {
        loadTrending()
    }

    fun loadTrending() {
        _trending.value = TrendingState.Loading
        viewModelScope.launch {
            _trending.value = try {
                TrendingState.Success(youtubeRepo.getTrending())
            } catch (e: Exception) {
                TrendingState.Error(e.localizedMessage ?: "Erreur reseau")
            }
        }
    }
}
