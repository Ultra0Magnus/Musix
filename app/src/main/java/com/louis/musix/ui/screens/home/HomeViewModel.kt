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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SuggestionsState {
    data object Hidden : SuggestionsState
    data object Loading : SuggestionsState
    data class Success(val songs: List<Song>, val artists: List<String>) : SuggestionsState
    data class Error(val message: String) : SuggestionsState
}

class HomeViewModel(
    private val youtubeRepo: YouTubeRepository,
    private val libraryRepo: LibraryRepository,
) : ViewModel() {

    // Local carousels: come from Room, instant, capped at 10
    // v0.9.1 fix: use distinctBy to avoid duplicate keys crash if same song played multiple times
    val recentlyPlayed: StateFlow<List<Song>> = libraryRepo.history
        .map { it.distinctBy { song -> song.id }.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<Song>> = libraryRepo.favoriteSongs
        .map { it.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Network carousel: Suggestions based on Spotify Top 50
    private val _suggestions = MutableStateFlow<SuggestionsState>(SuggestionsState.Hidden)
    val suggestions: StateFlow<SuggestionsState> = _suggestions.asStateFlow()

    init {
        loadSuggestions()
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            _suggestions.value = SuggestionsState.Loading
            try {
                val playlistId = libraryRepo.getPlaylistIdByName("Spotify — Top 50 All Time")
                if (playlistId == null) {
                    _suggestions.value = SuggestionsState.Hidden
                    return@launch
                }

                val top50Songs = libraryRepo.getPlaylistSongs(playlistId).first()
                if (top50Songs.isEmpty()) {
                    _suggestions.value = SuggestionsState.Hidden
                    return@launch
                }

                val top50Ids = top50Songs.map { it.id }.toSet()
                
                // Get unique artists
                val artists = top50Songs.map { it.artist }.distinct()
                // Pick up to 5 random artists
                val selectedArtists = artists.shuffled().take(5)

                if (selectedArtists.isEmpty()) {
                    _suggestions.value = SuggestionsState.Hidden
                    return@launch
                }

                // Fetch suggestions from YouTube
                val suggestedSongs = mutableListOf<Song>()
                for (artist in selectedArtists) {
                    try {
                        val results = youtubeRepo.search(artist)
                        suggestedSongs.addAll(results)
                    } catch (e: Exception) {
                        // Ignore individual artist search failure
                    }
                }

                // Filter out songs already in Top 50, remove duplicates, and shuffle
                val finalSuggestions = suggestedSongs
                    .filter { !top50Ids.contains(it.id) }
                    .distinctBy { it.id }
                    .shuffled()
                    .take(20)

                if (finalSuggestions.isEmpty()) {
                    _suggestions.value = SuggestionsState.Hidden
                } else {
                    _suggestions.value = SuggestionsState.Success(finalSuggestions, selectedArtists)
                }

            } catch (e: Exception) {
                _suggestions.value = SuggestionsState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
