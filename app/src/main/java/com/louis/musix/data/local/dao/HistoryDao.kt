package com.louis.musix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.louis.musix.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/** Historique enrichi des détails du morceau (JOIN). */
data class HistoryWithSong(
    val songId: String,
    val playedAt: Long,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val videoUrl: String,
    val isDownloaded: Boolean,
    val localFilePath: String?,
)

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("""
        SELECT h.songId, h.playedAt, s.title, s.artist, s.thumbnailUrl, s.durationSeconds, s.videoUrl, s.isDownloaded, s.localFilePath
        FROM history h
        INNER JOIN songs s ON h.songId = s.id
        ORDER BY h.playedAt DESC
    """)
    fun getHistory(): Flow<List<HistoryWithSong>>

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

