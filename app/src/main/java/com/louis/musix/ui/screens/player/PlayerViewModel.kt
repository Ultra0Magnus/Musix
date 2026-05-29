package com.louis.musix.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.data.lyrics.LyricsRepository
import com.louis.musix.data.lyrics.LyricsResult
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.LyricLine
import com.louis.musix.domain.model.Song
import com.louis.musix.player.PlayerController
import com.louis.musix.player.RepeatMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── États de l'UI des paroles ────────────────────────────────────────────────

sealed interface LyricsUiState {
    data object Idle         : LyricsUiState
    data object Loading      : LyricsUiState
    data object Instrumental : LyricsUiState
    data object NotFound     : LyricsUiState
    data class  Plain(val text: String)             : LyricsUiState
    data class  Synced(val lines: List<LyricLine>)  : LyricsUiState
}

// ─── État de l'écran player ────────────────────────────────────────────────────

data class PlayerUiState(
    val song: Song? = null,
    val isLoadingAudio: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val error: String? = null,
    // File d'attente
    val queue: List<Song> = emptyList(),
    val queueSize: Int = 0,
    val currentQueueIndex: Int = 0,
    // Modes lecture
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    // Paroles
    val lyricsState: LyricsUiState = LyricsUiState.Idle,
)

// ─── ViewModel ─────────────────────────────────────────────────────────────────

class PlayerViewModel(
    private val repository: YouTubeRepository,
    private val songHolder: SelectedSongHolder,
    private val playerController: PlayerController,
    private val libraryRepo: LibraryRepository,
    private val lyricsRepo: LyricsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var favoriteJob: Job? = null
    private var lyricsJob: Job?   = null

    init {
        // ── Synchroniser l'état du controller → UI ───────────────────────────
        viewModelScope.launch {
            playerController.state.collect { s ->
                val prevSong = _uiState.value.song
                val newSong  = s.currentSong

                _uiState.update { it.copy(
                    isPlaying         = s.isPlaying,
                    currentPositionMs = s.currentPositionMs,
                    durationMs        = s.durationMs,
                    queue             = s.queue,
                    queueSize         = s.queueSize,
                    currentQueueIndex = s.currentQueueIndex,
                    shuffleEnabled    = s.shuffleEnabled,
                    repeatMode        = s.repeatMode,
                    song              = newSong ?: it.song,
                )}

                // Morceau changé (auto-advance inclus) → favoris + paroles
                if (newSong != null && newSong.id != prevSong?.id) {
                    observeFavorite(newSong)
                    loadLyrics(newSong)
                }
            }
        }

        // ── Charger la chanson déposée par la navigation ─────────────────────
        val pending = songHolder.current
        if (pending != null) {
            songHolder.current = null
            val pendingQueue      = songHolder.pendingQueue
            val pendingQueueIndex = songHolder.pendingQueueIndex
            songHolder.pendingQueue      = null
            songHolder.pendingQueueIndex = 0

            if (pendingQueue != null) {
                // Vient d'une playlist / album → file d'attente imposée, pas d'auto-queue
                loadAndPlayQueue(pendingQueue, pendingQueueIndex)
            } else {
                // Lecture isolée → auto-queue par artiste
                loadAndPlay(pending)
            }
        } else {
            playerController.state.value.currentSong?.let { current ->
                _uiState.update { it.copy(song = current) }
                observeFavorite(current)
                loadLyrics(current)
            }
        }
    }

    // ─── Actions publiques ────────────────────────────────────────────────────

    fun loadAndPlay(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(song = song, isLoadingAudio = true, error = null) }

            try {
                val audioUrl = repository.getAudioStreamUrl(song.videoUrl)
                playerController.setAndPlay(song, audioUrl)
                libraryRepo.logHistory(song)
                _uiState.update { it.copy(isLoadingAudio = false) }

                // Queue en arrière-plan (morceaux similaires)
                launch {
                    try {
                        val related = repository.search(song.artist)
                            .filter { it.id != song.id }
                            .take(20)
                        playerController.setQueue(listOf(song) + related, startIndex = 0)
                    } catch (_: Exception) {}
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

    /**
     * Joue [songs[startIndex]] et charge toute la liste comme file d'attente.
     * Contrairement à [loadAndPlay], ne déclenche PAS l'auto-queue par artiste.
     * Utilisé pour les playlists et les albums.
     */
    fun loadAndPlayQueue(songs: List<Song>, startIndex: Int = 0) {
        val song = songs.getOrNull(startIndex) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(song = song, isLoadingAudio = true, error = null) }
            try {
                val audioUrl = repository.getAudioStreamUrl(song.videoUrl)
                playerController.setAndPlay(song, audioUrl)
                libraryRepo.logHistory(song)
                playerController.setQueue(songs, startIndex = startIndex)
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

    // Lecture
    fun togglePlayPause()  = playerController.togglePlayPause()
    fun seekTo(ms: Long)   = playerController.seekTo(ms)
    fun skipToNext()       = playerController.skipToNext()
    fun skipToPrevious()   = playerController.skipToPrevious()

    // File d'attente
    fun addToQueue(song: Song)       = playerController.addToQueue(song)
    fun removeFromQueue(index: Int)  = playerController.removeFromQueue(index)

    // Shuffle / Repeat
    fun toggleShuffle()   = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()

    // Favoris
    fun toggleFavorite() {
        val song = _uiState.value.song ?: return
        viewModelScope.launch { libraryRepo.toggleFavorite(song) }
    }

    // ─── Paroles ─────────────────────────────────────────────────────────────

    private fun loadLyrics(song: Song) {
        lyricsJob?.cancel()
        _uiState.update { it.copy(lyricsState = LyricsUiState.Loading) }
        lyricsJob = viewModelScope.launch {
            val result = lyricsRepo.getLyrics(song.artist, song.title, song.durationSeconds)
            _uiState.update {
                it.copy(lyricsState = when (result) {
                    is LyricsResult.Synced       -> LyricsUiState.Synced(result.lines)
                    is LyricsResult.Plain        -> LyricsUiState.Plain(result.text)
                    LyricsResult.Instrumental    -> LyricsUiState.Instrumental
                    LyricsResult.NotFound        -> LyricsUiState.NotFound
                })
            }
        }
    }

    // ─── Privé ───────────────────────────────────────────────────────────────

    private fun observeFavorite(song: Song) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            libraryRepo.isFavorite(song.id).collect { fav ->
                _uiState.update { it.copy(isFavorite = fav) }
            }
        }
    }
}
