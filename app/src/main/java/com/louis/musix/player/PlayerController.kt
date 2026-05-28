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
    /** true dès qu'un MediaItem a été chargé (pour afficher le MiniPlayer) */
    val hasActiveMedia: Boolean = false,
    /** Morceau en cours de lecture (mis à jour par setAndPlay). */
    val currentSong: Song? = null,
    /** Nombre total de morceaux dans la file d'attente. */
    val queueSize: Int = 0,
    /** Index (0-based) du morceau en cours dans la file d'attente. */
    val currentQueueIndex: Int = 0,
)

// ─── Controller ─────────────────────────────────────────────────────────────────

/**
 * Singleton qui wrape [MediaController] — le client Media3 qui parle à [MusixPlayerService].
 *
 * Rôle :
 *  - Connecte à MusixPlayerService de façon asynchrone et transparente
 *  - Expose un [StateFlow<PlayerControllerState>] réactif (isPlaying, position…)
 *  - Fournit les actions play/pause/seek utilisées par PlayerViewModel et MiniPlayer
 */
class PlayerController(
    private val context: Context,
    private val youtubeRepo: YouTubeRepository,
) {

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerControllerState())
    val state: StateFlow<PlayerControllerState> = _state.asStateFlow()

    // Scope de vie app (singleton Koin → même durée de vie que le process)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: Job? = null

    // ─── File d'attente interne ───────────────────────────────────────────────
    // La queue est gérée au niveau app car les URLs YouTube expirent (~6h) :
    // on ne peut pas les charger toutes d'avance dans ExoPlayer.

    private val _queue = mutableListOf<Song>()
    private var queueIdx = 0

    // ─── Listener Media3 ───────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionPolling() else positionJob?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = controller?.duration?.coerceAtLeast(0L) ?: 0L
            _state.update { it.copy(durationMs = dur) }

            // Fin du morceau → avance automatiquement vers le suivant dans la file
            if (playbackState == Player.STATE_ENDED) {
                val next = _queue.getOrNull(queueIdx + 1)
                if (next != null) {
                    queueIdx++
                    _state.update { it.copy(currentQueueIndex = queueIdx) }
                    Log.d(TAG, "STATE_ENDED → auto-advance vers \"${next.title}\" (${queueIdx}/${_queue.size})")
                    scope.launch { autoAdvanceTo(next) }
                } else {
                    Log.d(TAG, "STATE_ENDED → fin de la file d'attente")
                }
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            // Synchronise le titre/artiste depuis les metadata du MediaItem
            // (utile si la lecture continue en arrière-plan et que l'UI se reconstruit)
            _state.update { it.copy(
                title     = mediaMetadata.title?.toString() ?: _state.value.title,
                artist    = mediaMetadata.artist?.toString() ?: _state.value.artist,
                artworkUri= mediaMetadata.artworkUri?.toString() ?: _state.value.artworkUri,
            )}
        }
    }

    // ─── Polling de position ──────────────────────────────────────────────────

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

    /**
     * Connecte à [MusixPlayerService] si ce n'est pas déjà fait.
     * Idempotent — safe à appeler plusieurs fois.
     * Doit s'exécuter sur le Main thread (obligation MediaController).
     */
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
            Log.e(TAG, "Connexion au service KO : ${e.message}")
            false
        }
    }

    // ─── Contrôles de lecture ─────────────────────────────────────────────────

    /**
     * Charge [audioUrl] dans ExoPlayer (via le service) et démarre la lecture.
     * Les metadata de [song] alimentent la notification et le MiniPlayer.
     */
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
     * Définit la file d'attente sans changer la lecture en cours.
     * Appelé par PlayerViewModel après qu'il a chargé les morceaux similaires.
     *
     * @param songs      Liste complète de la file (le morceau courant doit être en position [startIndex]).
     * @param startIndex Index du morceau actuellement en lecture.
     */
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _queue.clear()
        _queue.addAll(songs)
        queueIdx = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        _state.update { it.copy(
            queueSize         = songs.size,
            currentQueueIndex = queueIdx,
        )}
        Log.d(TAG, "File d'attente mise à jour : ${songs.size} morceaux, idx=$queueIdx")
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    /**
     * Passe au morceau suivant dans la file d'attente.
     * Récupère l'URL audio à la volée (les URLs YouTube expirent — pas de cache).
     */
    fun skipToNext() {
        val next = _queue.getOrNull(queueIdx + 1) ?: return
        queueIdx++
        _state.update { it.copy(currentQueueIndex = queueIdx) }
        Log.d(TAG, "skipToNext → \"${next.title}\" ($queueIdx/${_queue.size})")
        scope.launch { autoAdvanceTo(next) }
    }

    /**
     * Revient au début du morceau si la position dépasse 3 s,
     * sinon passe au morceau précédent dans la file d'attente.
     */
    fun skipToPrevious() {
        val ctrl = controller ?: return
        if (ctrl.currentPosition > 3_000L) {
            ctrl.seekTo(0L)
            return
        }
        val prev = _queue.getOrNull(queueIdx - 1)
        if (prev == null) {
            ctrl.seekTo(0L)
            return
        }
        queueIdx--
        _state.update { it.copy(currentQueueIndex = queueIdx) }
        Log.d(TAG, "skipToPrevious → \"${prev.title}\" ($queueIdx/${_queue.size})")
        scope.launch { autoAdvanceTo(prev) }
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
}
