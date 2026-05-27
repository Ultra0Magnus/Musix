package com.louis.musix.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    foreignKeys = [ForeignKey(
        entity        = SongEntity::class,
        parentColumns = ["id"],
        childColumns  = ["songId"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("songId")],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val playedAt: Long = System.currentTimeMillis(),
)
