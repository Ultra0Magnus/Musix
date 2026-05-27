package com.louis.musix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.louis.musix.domain.model.Song

/**
 * Cache local des métadonnées d'un morceau.
 * Clé primaire = videoId YouTube ("dQw4w9WgXcQ").
 * Référencé en FK par favorites, history et playlist_songs.
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val videoUrl: String,
) {
    fun toDomain() = Song(id, title, artist, thumbnailUrl, durationSeconds, videoUrl)

    companion object {
        fun fromDomain(s: Song) = SongEntity(
            id              = s.id,
            title           = s.title,
            artist          = s.artist,
            thumbnailUrl    = s.thumbnailUrl,
            durationSeconds = s.durationSeconds,
            videoUrl        = s.videoUrl,
        )
    }
}
