package com.louis.musix.data

import com.louis.musix.domain.model.Song

/**
 * Holder singleton (via Koin) qui stocke la chanson sélectionnée par l'utilisateur.
 *
 * Pourquoi ? Compose Navigation ne permet pas de passer un objet complexe (Song)
 * directement comme argument. On contourne en stockant la chanson ici, puis en
 * naviguant vers l'écran player qui la lit depuis ce holder.
 *
 * [pendingQueue] / [pendingQueueIndex] permettent d'imposer une file d'attente
 * complète (ex. playlist) au lieu de laisser le player créer une auto-queue
 * basée sur l'artiste.
 */
class SelectedSongHolder {
    var current: Song? = null

    /** Si non-null, le PlayerViewModel utilisera cette liste comme file d'attente. */
    var pendingQueue: List<Song>? = null
    var pendingQueueIndex: Int = 0
}
