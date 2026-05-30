package com.louis.musix.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "Musix.PlayerController"

// ─── State exposed to the rest of the app ──────────────────────────────────────

data class PlayerControllerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String = "",
    /** true once a MediaItem has been loaded (used to show the MiniPlayer). */
    val hasActiveMedia: Boolean = false,
    /** Currently playing song. */
    val currentSong: Song? = null,
    /** Full queue (immutable copy for the UI). */
    val queue: List<Song> = emptyList(),
    /** 0-based index of the current song in [queue]. */
    val currentQueueIndex: Int = 0,
    /** Total number of songs in the queue. */
    val queueSize: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
)

// ─── Controller ─────────────────────────────────────────────────────────────────

/**
 * Singleton wrapping [MediaController] and managing the playback queue on the app side.
 *
 * YouTube URLs expire (~6h) — they cannot be pre-loaded into ExoPlayer.
 * The queue is therefore managed here: [STATE_ENDED] → fetch next URL → play.
 */
class PlayerController(
    private val context: Context,
    private val youtubeRepo: YouTubeRepository,
    private val libraryRepo: LibraryRepository,
) {

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerControllerState())
    val state: StateFlow<PlayerControllerState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: Job? = null

    // ─── Internal queue ───────────────────────────────────────────────────────
    // _originalQueue = insertion order (unshuffled)
    // _queue         = active order (may be shuffled)

    private val _originalQueue = mutableListOf<Song>()
    private val _queue         = mutableListOf<Song>()
    private var queueIdx       = 0
    private var _shuffleEnabled = false
    private var _repeatMode     = RepeatMode.OFF

    // ─── Media3 listener ─────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionPolling() else positionJob?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = controller?.duration?.coerceAtLeast(0L) ?: 0L
            _state.update { it.copy(durationMs = dur) }

            if (playbackState == Player.STATE_ENDED) {
                onTrackEnded()
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            _state.update { it.copy(
                title      = mediaMetadata.title?.toString()      ?: _state.value.title,
                artist     = mediaMetadata.artist?.toString()     ?: _state.value.artist,
                artworkUri = mediaMetadata.artworkUri?.toString() ?: _state.value.artworkUri,
            )}
        }
    }

    // ─── Track ended ─────────────────────────────────────────────────────────

    private fun onTrackEnded() {
        when (_repeatMode) {
            RepeatMode.ONE -> {
                // Replay current track
                val current = _queue.getOrNull(queueIdx) ?: return
                Log.d(TAG, "STATE_ENDED → repeat ONE: \"${current.title}\"")
                scope.launch { autoAdvanceTo(current) }
            }
            RepeatMode.ALL -> {
                val nextIdx = if (queueIdx + 1 < _queue.size) queueIdx + 1 else 0
                queueIdx = nextIdx
                val next = _queue.getOrNull(queueIdx) ?: return
                Log.d(TAG, "STATE_ENDED → repeat ALL → \"${next.title}\" ($queueIdx/${_queue.size})")
                pushQueueState()
                scope.launch { autoAdvanceTo(next) }
            }
            RepeatMode.OFF -> {
                val next = _queue.getOrNull(queueIdx + 1) ?: run {
                    Log.d(TAG, "STATE_ENDED → end of queue")
                    return
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "ExoPlayer Error: ${error.message} (code ${error.errorCode})")
            _state.update { it.copy(isPlaying = false) }
            
            // v0.9.3 - Auto-skip: if playback fails, skip to next after 2 seconds
            scope.launch {
                delay(2000)
                skipToNext()
            }
        }
    }
                queueIdx++
                Log.d(TAG, "STATE_ENDED → next \"${next.title}\" ($queueIdx/${_queue.size})")
                pushQueueState()
                scope.launch { autoAdvanceTo(next) }
            }
        }
    }

    // ─── Position polling ─────────────────────────────────────────────────────

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val ctrl = controller ?: break
                if (!ctrl.isPlaying) break
                _state.update { it.copy(currentPositionMs = ctrl.currentPosition) }
                delay(500)
            }
        }
    }

    // ─── Service connection ───────────────────────────────────────────────────

    suspend fun ensureConnected(): Boolean = withContext(Dispatchers.Main) {
        controller?.takeIf { it.isConnected }?.let { return@withContext true }

        return@withContext try {
            val token = SessionToken(
                context,
                ComponentName(context, MusixPlayerService::class.java)
            )
            val ctrl = suspendCancellableCoroutine<MediaController> { cont ->
                val future = MediaController.Builder(context, token).buildAsync()
                future.addListener({
                    try   { cont.resume(future.get()) }
                    catch (e: Exception) { cont.resumeWithException(e) }
                }, ContextCompat.getMainExecutor(context))
                cont.invokeOnCancellation { future.cancel(true) }
            }
            ctrl.addListener(playerListener)
            controller = ctrl
            Log.d(TAG, "MediaController connected")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            false
        }
    }

    // ─── Playback controls ────────────────────────────────────────────────────

    suspend fun setAndPlay(song: Song, audioUrl: String) {
        if (!ensureConnected()) throw Exception("Playback service unavailable")

        withContext(Dispatchers.Main) {
            val ctrl = controller ?: throw Exception("MediaController not initialized")

            val mediaItem = MediaItem.Builder()
                .setUri(audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(Uri.parse(song.thumbnailUrl))
                        .build()
                )
                .build()

            ctrl.setMediaItem(mediaItem)
            ctrl.prepare()
            ctrl.play()

            _state.update { it.copy(
                title          = song.title,
                artist         = song.artist,
                artworkUri     = song.thumbnailUrl,
                hasActiveMedia = true,
                isPlaying      = true,
                currentSong    = song,
            )}
        }
    }

    /**
     * Sets the playback queue without interrupting the current track.
     * [startIndex] is the index of the already-playing song within [songs].
     */
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _originalQueue.clear()
        _originalQueue.addAll(songs)

        val clampedIdx = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))

        if (_shuffleEnabled) {
            rebuildShuffledQueue(currentSong = songs.getOrNull(clampedIdx))
        } else {
            _queue.clear()
            _queue.addAll(songs)
            queueIdx = clampedIdx
        }

        pushQueueState()
        Log.d(TAG, "Queue: ${_queue.size} songs, idx=$queueIdx, shuffle=$_shuffleEnabled")
    }

    /** Adds a song to the end of the queue (and the original list). */
    fun addToQueue(song: Song) {
        _queue.add(song)
        _originalQueue.add(song)
        pushQueueState()
        Log.d(TAG, "Added to queue: \"${song.title}\" (total ${_queue.size})")
    }

    /**
     * Removes the song at [index] from the queue.
     * The currently playing song ([queueIdx]) cannot be removed.
     */
    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= _queue.size || index == queueIdx) return
        val removed = _queue.removeAt(index)
        _originalQueue.remove(removed)
        if (index < queueIdx) queueIdx--
        pushQueueState()
        Log.d(TAG, "Removed from queue: \"${removed.title}\"")
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipToNext() {
        val next = _queue.getOrNull(queueIdx + 1) ?: run {
            if (_repeatMode == RepeatMode.ALL && _queue.isNotEmpty()) {
                queueIdx = 0
                pushQueueState()
                _queue.firstOrNull()?.let { scope.launch { autoAdvanceTo(it) } }
            }
            return
        }
        queueIdx++
        pushQueueState()
        Log.d(TAG, "skipToNext → \"${next.title}\" ($queueIdx/${_queue.size})")
        scope.launch { autoAdvanceTo(next) }
    }

    fun skipToPrevious() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > 3_000L) {
            ctrl.seekTo(0L)
            return
        }
        val prev = _queue.getOrNull(queueIdx - 1)
        if (prev == null) { ctrl.seekTo(0L); return }
        queueIdx--
        pushQueueState()
        Log.d(TAG, "skipToPrevious → \"${prev.title}\" ($queueIdx/${_queue.size})")
        scope.launch { autoAdvanceTo(prev) }
    }

    // ─── Shuffle ──────────────────────────────────────────────────────────────

    fun toggleShuffle() {
        _shuffleEnabled = !_shuffleEnabled
        val currentSong = _queue.getOrNull(queueIdx)

        if (_shuffleEnabled) {
            rebuildShuffledQueue(currentSong)
        } else {
            // Restore original order
            _queue.clear()
            _queue.addAll(_originalQueue)
            queueIdx = currentSong?.let { song ->
                _queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            } ?: 0
        }

        pushQueueState()
        Log.d(TAG, "Shuffle: $_shuffleEnabled")
    }

    private fun rebuildShuffledQueue(currentSong: Song?) {
        val remaining = _originalQueue.toMutableList()
        currentSong?.let { remaining.removeAll { it.id == currentSong.id } }

        _queue.clear()
        currentSong?.let { _queue.add(it) }
        _queue.addAll(remaining.shuffled())
        queueIdx = 0
    }

    // ─── Repeat ───────────────────────────────────────────────────────────────

    fun cycleRepeatMode() {
        _repeatMode = when (_repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        pushQueueState()
        Log.d(TAG, "Repeat: $_repeatMode")
    }

    // ─── Auto-advance ─────────────────────────────────────────────────────────

    private suspend fun autoAdvanceTo(song: Song) {
        try {
            val audioUrl = if (song.isDownloaded && song.localFilePath != null) {
                Log.d(TAG, "Playing downloaded audio for \"${song.title}\"")
                song.localFilePath
            } else {
                Log.d(TAG, "Fetching audio URL for \"${song.title}\"…")
                youtubeRepo.getAudioStreamUrl(song.videoUrl)
            }
            setAndPlay(song, audioUrl)
        } catch (e: Exception) {
            Log.e(TAG, "autoAdvanceTo(\"${song.title}\") failed: ${e.message}")
            // v0.9.3 - Auto-skip: if streaming URL extraction fails, skip to next after 2 seconds
            scope.launch {
                delay(2000)
                skipToNext()
            }
        }
    }

    // ─── Push state ───────────────────────────────────────────────────────────

    private fun pushQueueState() {
        _state.update { it.copy(
            queue             = _queue.toList(),
            queueSize         = _queue.size,
            currentQueueIndex = queueIdx,
            shuffleEnabled    = _shuffleEnabled,
            repeatMode        = _repeatMode,
        )}
    }
}
