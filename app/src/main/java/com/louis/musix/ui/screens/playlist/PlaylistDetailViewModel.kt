package com.louis.musix.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // While true, the DB Flow is ignored so it doesn't overwrite a drag in progress
    private var reordering = false

    init {
        // Load the playlist name
        viewModelScope.launch {
            _uiState.update { it.copy(playlistName = repo.getPlaylistName(playlistId) ?: "") }
        }
        // Observe the playlist songs (paused during reorder to avoid visual glitches)
        viewModelScope.launch {
            repo.getPlaylistSongs(playlistId).collect { songs ->
                if (!reordering) _uiState.update { it.copy(songs = songs) }
            }
        }
    }

    fun removeSong(songId: String) {
        viewModelScope.launch { repo.removeSongFromPlaylist(playlistId, songId) }
    }

    /**
     * Moves a song from [fromIndex] to [toIndex].
     * Updates the UI immediately (optimistic) then persists the new order to the DB.
     */
    fun moveSong(fromIndex: Int, toIndex: Int) {
        val newSongs = _uiState.value.songs.toMutableList()
        if (fromIndex !in newSongs.indices || toIndex !in newSongs.indices) return
        newSongs.add(toIndex, newSongs.removeAt(fromIndex))
        reordering = true
        _uiState.update { it.copy(songs = newSongs) }
        viewModelScope.launch {
            repo.reorderSongs(playlistId, newSongs)
            reordering = false
        }
    }
}
