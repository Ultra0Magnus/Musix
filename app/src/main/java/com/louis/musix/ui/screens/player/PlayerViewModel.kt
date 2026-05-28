package com.louis.musix.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Song
import com.louis.musix.player.PlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── État de l'écran player ───────────────────────────────────────────────────

data class PlayerUiState(
    val song: Song? = null,
    val isLoadingAudio: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val error: String? = null,
    val queueSize: Int = 0,
    val currentQueueIndex: Int = 0,
)

class PlayerViewModel(
    private val repository: YouTubeRepository,
    private val songHolder: SelectedSongHolder,
    private val playerController: PlayerController,
    private val libraryRepo: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

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
                    song = newSong ?: it.song,
                )}

                if (newSong != null && newSong.id != prevSong?.id) {
                    observeFavorite(newSong)
                }
            }
        }

        // ── 2. Charger la chanson déposée par la navigation ────────────────────
        val pending = songHolder.current
        if (pending != null) {
            songHolder.current = null
            loadAndPlay(pending)
        } else {
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

            // ── Lancer en parallèle : URL audio + historique + queue (album) ──
            val audioUrlDeferred = async {
                repository.getAudioStreamUrl(song.videoUrl)
            }
            // Historique loggé immédiatement, pas besoin d'attendre le player
            launch { libraryRepo.logHistory(song) }

            try {
                val audioUrl = audioUrlDeferred.await()
                playerController.setAndPlay(song, audioUrl)
                _uiState.update { it.copy(isLoadingAudio = false) }

                // ── Construction de la file d'attente via les pistes de l'artiste ──
                // Priorité : album en cours si disponible (via searchAlbums + getAlbumTracks),
                // sinon fallback sur une recherche générique par nom d'artiste.
                launch {
                    try {
                        val albums = repository.searchAlbums(song.artist)
                        val queueSongs: List<Song> = if (albums.isNotEmpty()) {
                            // Utilise le premier album trouvé pour la queue
                            val albumTracks = repository.getAlbumTracks(albums.first().playlistUrl)
                            if (albumTracks.isNotEmpty()) albumTracks else fallbackQueue(song)
                        } else {
                            fallbackQueue(song)
                        }
                        // Positionner le morceau courant en tête (ou à sa position dans l'album)
                        val startIdx = queueSongs.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
                        val finalQueue = if (startIdx == 0) listOf(song) + queueSongs.filter { it.id != song.id }
                                         else queueSongs
                        playerController.setQueue(finalQueue, startIndex = startIdx)
                    } catch (_: Exception) {
                        // File non construite → pas grave, la lecture continue
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

    /** Fallback : recherche générique par nom d'artiste si aucun album trouvé. */
    private suspend fun fallbackQueue(song: Song): List<Song> =
        repository.search(song.artist).filter { it.id != song.id }.take(20)

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun skipToNext() = playerController.skipToNext()
    fun skipToPrevious() = playerController.skipToPrevious()

    fun toggleFavorite() {
        val song = _uiState.value.song ?: return
        viewModelScope.launch { libraryRepo.toggleFavorite(song) }
    }

    private fun observeFavorite(song: Song) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            libraryRepo.isFavorite(song.id).collect { fav ->
                _uiState.update { it.copy(isFavorite = fav) }
            }
        }
    }
}
