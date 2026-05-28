package com.louis.musix.player

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Service de lecture audio en arrière-plan.
 *
 * MediaSessionService (Media3) gère automatiquement :
 *  - La notification persistante avec titre / artiste / contrôles
 *  - Les contrôles sur l'écran de verrouillage
 *  - La compatibilité avec les casques Bluetooth (play/pause/skip)
 *  - Le WAKE_LOCK (CPU actif pendant la lecture)
 *
 * ExoPlayer vit ici (et non dans le ViewModel) afin que la lecture
 * continue quand l'app passe en arrière-plan ou que l'écran se verrouille.
 */
class MusixPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    // ─── Cycle de vie ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().also {
            // REPEAT_MODE_OFF : ExoPlayer émet STATE_ENDED en fin de morceau.
            // La file d'attente est gérée au niveau app (PlayerController._queue) :
            // STATE_ENDED → PlayerController détecte la fin et charge le morceau suivant.
            it.repeatMode = Player.REPEAT_MODE_OFF
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    /**
     * Appelé par le MediaController côté UI pour obtenir la session active.
     * Retourne null si la session n'est pas encore prête.
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
     * Arrêter le service quand l'app est fermée depuis le gestionnaire de tâches
     * ET que rien n'est en cours de lecture (comportement attendu type Spotify).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
