package com.louis.musix.ui.screens.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── États de l'écran album ───────────────────────────────────────────────────

sealed interface AlbumDetailUiState {
    data object Loading : AlbumDetailUiState
    data class  Success(val albumName: String, val songs: List<Song>) : AlbumDetailUiState
    data class  Error(val message: String)                            : AlbumDetailUiState
}

// ─── ViewModel ─────────────────────────────────────────────────────────────────

class AlbumDetailViewModel(
    private val youtubeRepo: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    /**
     * Récupère les pistes de l'album identifié par [playlistUrl].
     * Appelé depuis un [LaunchedEffect] dans [AlbumDetailScreen].
     */
    fun loadAlbum(albumName: String, playlistUrl: String) {
        viewModelScope.launch {
            _uiState.value = AlbumDetailUiState.Loading
            try {
                val songs = youtubeRepo.getAlbumTracks(playlistUrl)
                _uiState.value = if (songs.isEmpty())
                    AlbumDetailUiState.Error("Aucune piste trouvée dans cet album")
                else
                    AlbumDetailUiState.Success(albumName, songs)
            } catch (e: Exception) {
                _uiState.value = AlbumDetailUiState.Error(
                    e.localizedMessage ?: "Impossible de charger l'album"
                )
            }
        }
    }
}
