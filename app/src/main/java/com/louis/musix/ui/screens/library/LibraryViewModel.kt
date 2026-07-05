package com.louis.musix.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.download.DownloadManager
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Playlist
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<Song>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val downloaded: StateFlow<List<Song>> = repository.downloadedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Library search ─────────────────────────────────────────────────────────
    // A single shared query filters the Favorites and History tabs.

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(newQuery: String) { _query.value = newQuery }

    private fun List<Song>.matching(q: String): List<Song> {
        if (q.isBlank()) return this
        val needle = q.trim().lowercase()
        return filter {
            it.title.lowercase().contains(needle) || it.artist.lowercase().contains(needle)
        }
    }

    val filteredFavorites: StateFlow<List<Song>> =
        combine(favorites, _query) { list, q -> list.matching(q) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredHistory: StateFlow<List<Song>> =
        combine(history, _query) { list, q -> list.matching(q) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renamePlaylist(playlistId, name) }
    }

    // ─── Favorites ────────────────────────────────────────────────────────────

    fun removeFavorite(song: Song) {
        viewModelScope.launch { repository.toggleFavorite(song) }
    }

    // ─── History ──────────────────────────────────────────────────────────────

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    // ─── Downloads ────────────────────────────────────────────────────────────

    fun removeDownload(song: Song) {
        viewModelScope.launch { downloadManager.toggleDownload(song) }
    }
}

