package com.louis.musix.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.domain.model.Song
import com.louis.musix.player.PlayerController
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
    val error: String? = null,
)

// ─── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel de l'écran lecteur (Phase 4).
 *
 * Ne contient plus d'ExoPlayer — délègue toute la lecture à [PlayerController]
 * qui communique avec [MusixPlayerService] via MediaController.
 * La lecture continue en arrière-plan même quand l'écran Player est fermé.
 */
class PlayerViewModel(
    private val repository: YouTubeRepository,
    private val songHolder: SelectedSongHolder,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        // Synchroniser isPlaying / position / durée depuis le controller
        viewModelScope.launch {
            playerController.state.collect { s ->
                _uiState.update { it.copy(
                    isPlaying        = s.isPlaying,
                    currentPositionMs= s.currentPositionMs,
                    durationMs       = s.durationMs,
                )}
            }
        }

        // Charger immédiatement la chanson en attente (déposée par SearchScreen)
        songHolder.current?.let { loadAndPlay(it) }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    /**
     * Lance le chargement de l'URL audio (NewPipeExtractor) puis démarre
     * la lecture via [PlayerController] → [MusixPlayerService].
     */
    fun loadAndPlay(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(song = song, isLoadingAudio = true, error = null) }
            try {
                val audioUrl = repository.getAudioStreamUrl(song.videoUrl)
                playerController.setAndPlay(song, audioUrl)
                _uiState.update { it.copy(isLoadingAudio = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoadingAudio = false,
                    error = "Impossible de charger : ${e.localizedMessage}",
                )}
            }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
}
