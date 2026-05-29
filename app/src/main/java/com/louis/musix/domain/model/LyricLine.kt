package com.louis.musix.domain.model

/**
 * Une ligne de paroles avec son timestamp.
 * @param timeMs  Position en millisecondes dans le morceau.
 * @param text    Texte de la ligne.
 */
data class LyricLine(val timeMs: Long, val text: String)
