package com.louis.musix.ui.screens.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.domain.model.ArtistAlbum
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── Artist page state ───────────────────────────────────────────────────────

data class ArtistUiState(
    // Top songs
    val topSongs: List<Song>        = emptyList(),
    val isLoadingSongs: Boolean     = true,
    val songsError: String?         = null,
    // Albums
    val albums: List<ArtistAlbum>   = emptyList(),
    val isLoadingAlbums: Boolean    = true,
    val albumsError: String?        = null,
)

// ─── ViewModel ─────────────────────────────────────────────────────────────────

class ArtistViewModel(
    private val youtubeRepo: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    /**
     * Loads top songs AND albums in parallel.
     * Called from a [LaunchedEffect] in [ArtistScreen].
     */
    fun loadArtist(artistName: String) {
        // Reset on artist change
        _uiState.value = ArtistUiState()

        viewModelScope.launch {
            // ── Top songs ────────────────────────────────────────────────────
            val songsJob = async {
                try {
                    youtubeRepo.search(artistName).take(20)
                } catch (e: Exception) {
                    throw e
                }
            }

            // ── Albums ───────────────────────────────────────────────────────
            val albumsJob = async {
                youtubeRepo.searchAlbums(artistName)
            }

            // Results collected independently (neither blocks the other)
            try {
                val songs = songsJob.await()
                _uiState.update { it.copy(topSongs = songs, isLoadingSongs = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingSongs = false,
                        songsError = e.localizedMessage ?: "Search error",
                    )
                }
            }

            try {
                val albums = albumsJob.await()
                _uiState.update { it.copy(albums = albums, isLoadingAlbums = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingAlbums = false) }
                // No visible error if albums fail to load — section is simply empty
            }
        }
    }
}
