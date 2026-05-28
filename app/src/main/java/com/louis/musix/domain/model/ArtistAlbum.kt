package com.louis.musix.domain.model

/**
 * Représente un album (playlist YouTube Music) affiché sur la page artiste.
 */
data class ArtistAlbum(
    /** Identifiant stable = URL de la playlist YouTube. */
    val id: String,
    val name: String,
    val artworkUrl: String,
    /** Nombre de pistes (0 si inconnu). */
    val trackCount: Int,
    /** URL complète de la playlist YouTube Music. */
    val playlistUrl: String,
)
