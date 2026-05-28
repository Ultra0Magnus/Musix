package com.louis.musix.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Song
import com.louis.musix.player.PlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── État de l'écran player ────────────────────────────────────────────────────

data class PlayerUiState(
    val song: Song? = null,
    val isLoadingAudio: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val error: String? = null,
)

// ─── ViewModel ─────────────────────────────────────────────────────────────────

class PlayerViewModel(
    private val repository: YouTubeRepository,
    private val songHolder: SelectedSongHolder,
    private val playerController: PlayerController,
    private val libraryRepo: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** Job annulé à chaque changement de chanson pour ne suivre que le favori courant. */
    private var favoriteJob: Job? = null

    init {
        // Synchroniser isPlaying / position / durée depuis le controller
        viewModelScope.launch {
            playerController.state.collect { s ->
                _uiState.update {
                    it.copy(
                        isPlaying         = s.isPlaying,
                        currentPositionMs = s.currentPositionMs,
                        durationMs        = s.durationMs,
                    )
                }
            }
        }
        // Charger immédiatement la chanson en attente (déposée par SearchScreen)
        songHolder.current?.let { loadAndPlay(it) }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    fun loadAndPlay(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(song = song, isLoadingAudio = true, error = null) }

            // Observer l'état favori pour ce morceau
            favoriteJob?.cancel()
            favoriteJob = viewModelScope.launch {
                libraryRepo.isFavorite(song.id).collect { fav ->
                    _uiState.update { it.copy(isFavorite = fav) }
                }
            }

            try {
                val audioUrl = repository.getAudioStreamUrl(song.videoUrl)
                playerController.setAndPlay(song, audioUrl)
                libraryRepo.logHistory(song)
                _uiState.update { it.copy(isLoadingAudio = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingAudio = false,
                        error = "Impossible de charger : ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun skipToNext() = playerController.skipToNext()

    fun skipToPrevious() = playerController.skipToPrevious()

    fun toggleFavorite() {
        val song = _uiState.value.song ?: return
        viewModelScope.launch { libraryRepo.toggleFavorite(song) }
    }
}
