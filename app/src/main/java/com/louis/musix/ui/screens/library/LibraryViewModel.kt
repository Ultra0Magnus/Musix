package com.louis.musix.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Playlist
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<Song>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val downloaded: StateFlow<List<Song>> = repository.downloadedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }
}


    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repo.deletePlaylist(playlistId) }
    }

    // ─── Favorites ────────────────────────────────────────────────────────────

    fun removeFavorite(song: Song) {
        viewModelScope.launch { repo.toggleFavorite(song) }
    }

    // ─── History ──────────────────────────────────────────────────────────────

    fun clearHistory() {
        viewModelScope.launch { repo.clearHistory() }
    }
}
