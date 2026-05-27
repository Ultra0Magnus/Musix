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
    private val repo: LibraryRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repo.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<Song>> = repo.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<Song>> = repo.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ─── Playlists ─────────────────────────────────────────────────────────────

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.createPlaylist(name) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repo.deletePlaylist(playlistId) }
    }

    // ─── Favoris ──────────────────────────────────────────────────────────────

    fun removeFavorite(song: Song) {
        viewModelScope.launch { repo.toggleFavorite(song) }
    }

    // ─── Historique ───────────────────────────────────────────────────────────

    fun clearHistory() {
        viewModelScope.launch { repo.clearHistory() }
    }
}
