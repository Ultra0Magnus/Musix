package com.louis.musix.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity        = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns  = ["playlistId"],
            onDelete      = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity        = SongEntity::class,
            parentColumns = ["id"],
            childColumns  = ["songId"],
            onDelete      = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId"), Index("songId")],
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)
