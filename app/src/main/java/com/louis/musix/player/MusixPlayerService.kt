package com.louis.musix.player

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import android.app.PendingIntent
import com.louis.musix.player.cache.CacheManager
import org.koin.android.ext.android.inject

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
 *
 * v0.9.2 — Stream caching: up to 300 MB of recently played audio is cached
 * on disk for instant replay without re-downloading.
 */
class MusixPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    
    // Inject CacheManager via Koin
    private val cacheManager: CacheManager by inject()

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        // v0.9.1 - Fix ForegroundServiceStartNotAllowedException on Android 12+
        val launchIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Set up ExoPlayer with our custom CacheDataSource
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheManager.cacheDataSourceFactory))
            .build().also {
                it.repeatMode = Player.REPEAT_MODE_OFF
            }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
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
