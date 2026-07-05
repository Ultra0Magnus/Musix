package com.louis.musix.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.data.download.DownloadManager
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

// ─── Lyrics UI states ─────────────────────────────────────────────────────────

sealed interface LyricsUiState {
    data object Idle         : LyricsUiState
    data object Loading      : LyricsUiState
    data object Instrumental : LyricsUiState
    data object NotFound     : LyricsUiState
    data class  Plain(val text: String)             : LyricsUiState
    data class  Synced(val lines: List<LyricLine>)  : LyricsUiState
}

// ─── Player screen state ──────────────────────────────────────────────────────

data class PlayerUiState(
    val song: Song? = null,
    val isLoadingAudio: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val error: String? = null,
    // Queue
    val queue: List<Song> = emptyList(),
    val queueSize: Int = 0,
    val currentQueueIndex: Int = 0,
    // Playback modes
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    // Sleep timer
    val sleepTimerEndMs: Long? = null,
    val sleepTimerEndOfTrack: Boolean = false,
    // Lyrics
    val lyricsState: LyricsUiState = LyricsUiState.Idle,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class PlayerViewModel(
    private val repository: YouTubeRepository,
    private val songHolder: SelectedSongHolder,
    private val playerController: PlayerController,
    private val libraryRepo: LibraryRepository,
    private val lyricsRepo: LyricsRepository,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var favoriteJob: Job? = null
    private var lyricsJob: Job?   = null
    private var downloadObserverJob: Job? = null

    init {
        // ── Sync controller state → UI ────────────────────────────────────────
        viewModelScope.launch {
            playerController.state.collect { s ->
                val prevSong = _uiState.value.song
                val newSong  = s.currentSong

                _uiState.update { currentState ->
                    // v0.9.1 fix: Only update the song object if it's actually a DIFFERENT song.
                    // This prevents the PlayerController (which holds an old "non-downloaded" Song object)
                    // from overwriting the reactively updated song status from the DB.
                    val songToDisplay = if (newSong != null) {
                        if (currentState.song?.id == newSong.id) currentState.song else newSong
                    } else {
                        currentState.song
                    }

                    currentState.copy(
                        isPlaying         = s.isPlaying,
                        currentPositionMs = s.currentPositionMs,
                        durationMs        = s.durationMs,
                        queue             = s.queue,
                        queueSize         = s.queueSize,
                        currentQueueIndex = s.currentQueueIndex,
                        shuffleEnabled    = s.shuffleEnabled,
                        repeatMode        = s.repeatMode,
                        sleepTimerEndMs      = s.sleepTimerEndMs,
                        sleepTimerEndOfTrack = s.sleepTimerEndOfTrack,
                        song              = songToDisplay,
                    )
                }

                // Track changed (auto-advance included) → update favorites + lyrics + download observer
                if (newSong != null && newSong.id != prevSong?.id) {
                    observeFavorite(newSong)
                    loadLyrics(newSong)
                    observeDownloadStatus(newSong)
                }
            }
        }

        // ── Load initial download status for existing song ────────────────────
        _uiState.value.song?.let { current ->
            observeDownloadStatus(current)
        }

        // ── Load the song deposited by navigation ─────────────────────────────
        val pending = songHolder.current
        if (pending != null) {
            songHolder.current = null
            val pendingQueue      = songHolder.pendingQueue
            val pendingQueueIndex = songHolder.pendingQueueIndex
            songHolder.pendingQueue      = null
            songHolder.pendingQueueIndex = 0

            if (pendingQueue != null) {
                // From a playlist / album → imposed queue, no artist auto-queue
                loadAndPlayQueue(pendingQueue, pendingQueueIndex)
            } else {
                // Standalone play → artist auto-queue
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

    // ─── Public actions ───────────────────────────────────────────────────────

    fun loadAndPlay(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(song = song, isLoadingAudio = true, error = null) }

            try {
                val audioUrl = if (song.isDownloaded && song.localFilePath != null) {
                    song.localFilePath
                } else {
                    repository.getAudioStreamUrl(song.videoUrl)
                }
                playerController.setAndPlay(song, audioUrl)
                libraryRepo.logHistory(song)
                _uiState.update { it.copy(isLoadingAudio = false) }

                // Build queue in background: prefer the current album's tracklist,
                // fall back to similar tracks by the same artist if none is found.
                launch {
                    try {
                        val albums = repository.searchAlbums(song.artist)
                        val queueSongs = albums.firstOrNull()
                            ?.let { repository.getAlbumTracks(it.playlistUrl) }
                            ?.takeIf { it.isNotEmpty() }
                            ?: fallbackQueue(song)
                        val startIdx = queueSongs.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
                        val finalQueue = if (startIdx == 0) {
                            listOf(song) + queueSongs.filter { it.id != song.id }
                        } else {
                            queueSongs
                        }
                        playerController.setQueue(finalQueue, startIndex = startIdx)
                    } catch (_: Exception) {}
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingAudio = false,
                        error = "Failed to load: ${e.localizedMessage}",
                    )
                }
                // Auto-skip on initial manual load failure
                launch {
                    kotlinx.coroutines.delay(2000)
                    playerController.skipToNext()
                }
            }
        }
    }

    /** Fallback when the artist has no album on YouTube Music: generic search by artist name. */
    private suspend fun fallbackQueue(song: Song): List<Song> =
        repository.search(song.artist).filter { it.id != song.id }.take(20)

    /**
     * Plays [songs[startIndex]] and loads the full list as the queue.
     * Unlike [loadAndPlay], does NOT trigger the artist auto-queue.
     * Used for playlists and albums.
     */
    fun loadAndPlayQueue(songs: List<Song>, startIndex: Int = 0) {
        val song = songs.getOrNull(startIndex) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(song = song, isLoadingAudio = true, error = null) }
            try {
                val audioUrl = if (song.isDownloaded && song.localFilePath != null) {
                    song.localFilePath
                } else {
                    repository.getAudioStreamUrl(song.videoUrl)
                }
                playerController.setAndPlay(song, audioUrl)
                libraryRepo.logHistory(song)
                playerController.setQueue(songs, startIndex = startIndex)
                _uiState.update { it.copy(isLoadingAudio = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingAudio = false,
                        error = "Failed to load: ${e.localizedMessage}",
                    )
                }
                // Auto-skip on manual queue load failure
                launch {
                    kotlinx.coroutines.delay(2000)
                    playerController.skipToNext()
                }
            }
        }
    }

    // Playback
    fun togglePlayPause()  = playerController.togglePlayPause()
    fun seekTo(ms: Long)   = playerController.seekTo(ms)
    fun skipToNext()       = playerController.skipToNext()
    fun skipToPrevious()   = playerController.skipToPrevious()

    // Queue
    fun addToQueue(song: Song)       = playerController.addToQueue(song)
    fun removeFromQueue(index: Int)  = playerController.removeFromQueue(index)

    // Shuffle / Repeat
    fun toggleShuffle()   = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()

    // Sleep timer
    fun setSleepTimer(minutes: Int)   = playerController.setSleepTimer(minutes)
    fun setSleepTimerEndOfTrack()     = playerController.setSleepTimerEndOfTrack()
    fun cancelSleepTimer()            = playerController.cancelSleepTimer()

    // Favorites
    fun toggleFavorite() {
        val song = _uiState.value.song ?: return
        viewModelScope.launch { libraryRepo.toggleFavorite(song) }
    }

    // Downloads
    fun toggleDownload() {
        val song = _uiState.value.song ?: return
        viewModelScope.launch {
            downloadManager.toggleDownload(song)
        }
    }

    // ─── Lyrics ───────────────────────────────────────────────────────────────

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

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun observeFavorite(song: Song) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            libraryRepo.isFavorite(song.id).collect { fav ->
                _uiState.update { it.copy(isFavorite = fav) }
            }
        }
    }

    private fun observeDownloadStatus(song: Song) {
        downloadObserverJob?.cancel()
        downloadObserverJob = viewModelScope.launch {
            libraryRepo.getSongFlow(song.id).collect { updatedSong ->
                if (updatedSong != null) {
                    _uiState.update { it.copy(song = updatedSong) }
                }
            }
        }
    }
}
