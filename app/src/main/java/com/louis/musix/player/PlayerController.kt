package com.louis.musix.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.louis.musix.data.newpipe.YouTubeRepository
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

// ─── État exposé au reste de l'app ─────────────────────────────────────────────

data class PlayerControllerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String = "",
    /** true dès qu'un MediaItem a été chargé (pour afficher le MiniPlayer). */
    val hasActiveMedia: Boolean = false,
    /** Morceau en cours de lecture. */
    val currentSong: Song? = null,
    /** File d'attente complète (copie immuable pour l'UI). */
    val queue: List<Song> = emptyList(),
    /** Index (0-based) du morceau courant dans [queue]. */
    val currentQueueIndex: Int = 0,
    /** Nombre total de morceaux dans la file. */
    val queueSize: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
)

// ─── Controller ─────────────────────────────────────────────────────────────────

/**
 * Singleton qui encapsule [MediaController] et gère la file d'attente côté app.
 *
 * Les URLs YouTube expirent (~6h) — impossible de les charger à l'avance dans ExoPlayer.
 * La file d'attente est donc gérée ici : [STATE_ENDED] → fetch URL suivante → play.
 */
class PlayerController(
    private val context: Context,
    private val youtubeRepo: YouTubeRepository,
) {

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerControllerState())
    val state: StateFlow<PlayerControllerState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: Job? = null

    // ─── File d'attente interne ────────────────────────────────────────────────
    // _originalQueue = ordre d'insertion (non mélangé)
    // _queue         = ordre actif (peut être mélangé si shuffle)

    private val _originalQueue = mutableListOf<Song>()
    private val _queue         = mutableListOf<Song>()
    private var queueIdx       = 0
    private var _shuffleEnabled = false
    private var _repeatMode     = RepeatMode.OFF

    // ─── Listener Media3 ─────────────────────────────────────────────────────

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

    // ─── Fin de morceau ───────────────────────────────────────────────────────

    private fun onTrackEnded() {
        when (_repeatMode) {
            RepeatMode.ONE -> {
                // Rejouer le morceau courant
                val current = _queue.getOrNull(queueIdx) ?: return
                Log.d(TAG, "STATE_ENDED → repeat ONE : \"${current.title}\"")
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
                    Log.d(TAG, "STATE_ENDED → fin de file")
                    return
                }
                queueIdx++
                Log.d(TAG, "STATE_ENDED → suivant \"${next.title}\" ($queueIdx/${_queue.size})")
                pushQueueState()
                scope.launch { autoAdvanceTo(next) }
            }
        }
    }

    // ─── Polling de position ─────────────────────────────────────────────────

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

    // ─── Connexion au service ─────────────────────────────────────────────────

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
            Log.d(TAG, "MediaController connecté")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connexion KO : ${e.message}")
            false
        }
    }

    // ─── Contrôles de lecture ─────────────────────────────────────────────────

    suspend fun setAndPlay(song: Song, audioUrl: String) {
        if (!ensureConnected()) throw Exception("Service de lecture non disponible")

        withContext(Dispatchers.Main) {
            val ctrl = controller ?: throw Exception("MediaController non initialisé")

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
     * Définit la file d'attente sans interrompre la lecture en cours.
     * [startIndex] est l'index du morceau déjà en lecture dans [songs].
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
        Log.d(TAG, "Queue : ${_queue.size} morceaux, idx=$queueIdx, shuffle=$_shuffleEnabled")
    }

    /** Ajoute un morceau à la fin de la file (et de l'original). */
    fun addToQueue(song: Song) {
        _queue.add(song)
        _originalQueue.add(song)
        pushQueueState()
        Log.d(TAG, "Ajouté à la file : \"${song.title}\" (total ${_queue.size})")
    }

    /**
     * Supprime le morceau à [index] de la file.
     * Impossible de supprimer le morceau en cours ([queueIdx]).
     */
    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= _queue.size || index == queueIdx) return
        val removed = _queue.removeAt(index)
        _originalQueue.remove(removed)
        if (index < queueIdx) queueIdx--
        pushQueueState()
        Log.d(TAG, "Supprimé de la file : \"${removed.title}\"")
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

    // ─── Shuffle ─────────────────────────────────────────────────────────────

    fun toggleShuffle() {
        _shuffleEnabled = !_shuffleEnabled
        val currentSong = _queue.getOrNull(queueIdx)

        if (_shuffleEnabled) {
            rebuildShuffledQueue(currentSong)
        } else {
            // Restaurer l'ordre original
            _queue.clear()
            _queue.addAll(_originalQueue)
            queueIdx = currentSong?.let { song ->
                _queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            } ?: 0
        }

        pushQueueState()
        Log.d(TAG, "Shuffle : $_shuffleEnabled")
    }

    private fun rebuildShuffledQueue(currentSong: Song?) {
        val remaining = _originalQueue.toMutableList()
        currentSong?.let { remaining.removeAll { it.id == currentSong.id } }

        _queue.clear()
        currentSong?.let { _queue.add(it) }
        _queue.addAll(remaining.shuffled())
        queueIdx = 0
    }

    // ─── Repeat ──────────────────────────────────────────────────────────────

    fun cycleRepeatMode() {
        _repeatMode = when (_repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        pushQueueState()
        Log.d(TAG, "Repeat : $_repeatMode")
    }

    // ─── Avance automatique ───────────────────────────────────────────────────

    private suspend fun autoAdvanceTo(song: Song) {
        try {
            Log.d(TAG, "Récupération URL audio pour \"${song.title}\"…")
            val url = youtubeRepo.getAudioStreamUrl(song.videoUrl)
            setAndPlay(song, url)
        } catch (e: Exception) {
            Log.e(TAG, "autoAdvanceTo(\"${song.title}\") KO : ${e.message}")
        }
    }

    // ─── Push état ───────────────────────────────────────────────────────────

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
