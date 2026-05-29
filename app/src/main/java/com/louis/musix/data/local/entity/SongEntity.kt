package com.louis.musix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.louis.musix.domain.model.Song

/**
 * Entité Room représentant un morceau.
 * Mise en cache locale pour éviter les appels réseau répétés.
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val videoUrl: String,
    // v0.9.1 - Offline mode
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
) {
    fun toDomain() = Song(
        id = id,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        videoUrl = videoUrl,
        isDownloaded = isDownloaded,
        localFilePath = localFilePath,
    )

    companion object {
        fun fromDomain(song: Song) = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            thumbnailUrl = song.thumbnailUrl,
            durationSeconds = song.durationSeconds,
            videoUrl = song.videoUrl,
            isDownloaded = song.isDownloaded,
            localFilePath = song.localFilePath,
        )
    }
}
