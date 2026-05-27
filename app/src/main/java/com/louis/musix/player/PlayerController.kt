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
class PlayerController(private val context: Context) {

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerControllerState())
    val state: StateFlow<PlayerControllerState> = _state.asStateFlow()

    // Scope de vie app (singleton Koin → même durée de vie que le process)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: Job? = null

    // ─── Listener Media3 ───────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionPolling() else positionJob?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = controller?.duration?.coerceAtLeast(0L) ?: 0L
            _state.update { it.copy(durationMs = dur) }
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
            )}
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }
}
