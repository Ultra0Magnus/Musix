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
    /** Taille de la file d'attente (0 = pas encore construite). */
    val queueSize: Int = 0,
    /** Position dans la file (0-based). */
    val currentQueueIndex: Int = 0,
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
        // ── 1. Synchroniser isPlaying / position / durée / queue / morceau courant ──
        viewModelScope.launch {
            playerController.state.collect { s ->
                val prevSong = _uiState.value.song
                val newSong  = s.currentSong

                _uiState.update { it.copy(
                    isPlaying         = s.isPlaying,
                    currentPositionMs = s.currentPositionMs,
                    durationMs        = s.durationMs,
                    queueSize         = s.queueSize,
                    currentQueueIndex = s.currentQueueIndex,
                    // Si le controller a un morceau courant (y compris après auto-advance),
                    // on le reflète dans l'UI même si loadAndPlay n'a pas été appelé.
                    song = newSong ?: it.song,
                )}

                // Abonnement favori mis à jour quand le morceau change (auto-advance inclus)
                if (newSong != null && newSong.id != prevSong?.id) {
                    observeFavorite(newSong)
                }
            }
        }

        // ── 2. Charger la chanson déposée par la navigation ──────────────────────
        val pending = songHolder.current
        if (pending != null) {
            songHolder.current = null   // consommer pour éviter un double-load
            loadAndPlay(pending)
        } else {
            // Reconstituer l'état si l'utilisateur revient sur PlayerScreen
            // sans avoir sélectionné un nouveau morceau (e.g. tap sur MiniPlayer)
            playerController.state.value.currentSong?.let { current ->
                _uiState.update { it.copy(song = current) }
                observeFavorite(current)
            }
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    fun loadAndPlay(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(song = song, isLoadingAudio = true, error = null) }

            try {
                val audioUrl = repository.getAudioStreamUrl(song.videoUrl)
                playerController.setAndPlay(song, audioUrl)
                libraryRepo.logHistory(song)
                _uiState.update { it.copy(isLoadingAudio = false) }

                // ── Construction de la file d'attente en arrière-plan ────────────
                // Recherche des morceaux de l'artiste → queue = [song courant] + [résultats]
                launch {
                    try {
                        val related = repository.search(song.artist)
                            .filter { it.id != song.id }
                            .take(20)
                        playerController.setQueue(listOf(song) + related, startIndex = 0)
                    } catch (_: Exception) {
                        // File non construite → pas grave, l'écoute continue
                    }
                }

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

    // ─── Privé ────────────────────────────────────────────────────────────────

    private fun observeFavorite(song: Song) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            libraryRepo.isFavorite(song.id).collect { fav ->
                _uiState.update { it.copy(isFavorite = fav) }
            }
        }
    }
}
