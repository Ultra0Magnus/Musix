package com.louis.musix.domain.model

/**
 * Représente un morceau de musique — modèle pur, sans dépendance Room ni NewPipe.
 *
 * Utilisé dans toute l'UI et les ViewModels. Les couches data convertissent
 * leurs types (StreamInfoItem, SongEntity) vers ce modèle.
 */
data class Song(
    /** Identifiant YouTube (ex: "dQw4w9WgXcQ") — clé primaire Room en Phase 5 */
    val id: String,
    val title: String,
    /** Nom de la chaîne / artiste */
    val artist: String,
    /** URL de la miniature JPEG */
    val thumbnailUrl: String,
    /** Durée en secondes (0 si inconnue) */
    val durationSeconds: Long,
    /** URL complète YouTube (ex: "https://www.youtube.com/watch?v=dQw4w9WgXcQ") */
    val videoUrl: String,
)

/** Formate une durée en secondes → "3:45" ou "1:02:30" */
fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
