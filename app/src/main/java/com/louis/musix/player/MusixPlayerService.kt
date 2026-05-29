package com.louis.musix.player

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background audio playback service.
 *
 * MediaSessionService (Media3) automatically handles:
 *  - Persistent notification with title / artist / controls
 *  - Lock-screen playback controls
 *  - Bluetooth headset compatibility (play/pause/skip)
 *  - WAKE_LOCK (keeps the CPU active during playback)
 *
 * ExoPlayer lives here (not in a ViewModel) so that playback continues
 * when the app goes to the background or the screen locks.
 */
class MusixPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().also {
            // REPEAT_MODE_OFF: ExoPlayer fires STATE_ENDED when a track finishes.
            // The queue is managed at the app level (PlayerController._queue):
            // STATE_ENDED → PlayerController detects the end and loads the next track.
            it.repeatMode = Player.REPEAT_MODE_OFF
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    /**
     * Called by the UI-side MediaController to obtain the active session.
     * Returns null if the session is not yet ready.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    /**
     * Stop the service when the app is dismissed from the recents screen
     * AND nothing is currently playing (expected Spotify-like behavior).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
