package com.louis.musix.data

import com.louis.musix.domain.model.Song

/**
 * Holder singleton (via Koin) qui stocke la chanson sélectionnée par l'utilisateur.
 *
 * Pourquoi ? Compose Navigation ne permet pas de passer un objet complexe (Song)
 * directement comme argument. On contourne en stockant la chanson ici, puis en
 * naviguant vers l'écran player qui la lit depuis ce holder.
 *
 * Ce sera remplacé en Phase 4 par un PlayerController MediaSession plus robuste.
 */
class SelectedSongHolder {
    var current: Song? = null
}
