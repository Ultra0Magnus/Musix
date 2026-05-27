package com.louis.musix.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    foreignKeys = [ForeignKey(
        entity      = SongEntity::class,
        parentColumns = ["id"],
        childColumns  = ["songId"],
        onDelete    = ForeignKey.CASCADE,
    )],
    indices = [Index("songId")],
)
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val favoritedAt: Long = System.currentTimeMillis(),
)
