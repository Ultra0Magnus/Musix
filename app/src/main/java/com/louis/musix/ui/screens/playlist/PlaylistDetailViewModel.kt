package com.louis.musix.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
    val playlistName: String = "",
    val songs: List<Song> = emptyList(),
)

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val repo: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        // Load the playlist name
        viewModelScope.launch {
            _uiState.update { it.copy(playlistName = repo.getPlaylistName(playlistId) ?: "") }
        }
        // Observe the playlist songs
        viewModelScope.launch {
            repo.getPlaylistSongs(playlistId).collect { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }
    }

    fun removeSong(songId: String) {
        viewModelScope.launch { repo.removeSongFromPlaylist(playlistId, songId) }
    }
}
