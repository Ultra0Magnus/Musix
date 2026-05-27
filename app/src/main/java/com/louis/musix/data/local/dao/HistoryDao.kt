package com.louis.musix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.louis.musix.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/** Résultat du JOIN history ⋈ songs. */
data class HistoryWithSong(
    val historyId: Long,
    val playedAt: Long,
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val videoUrl: String,
)

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    /**
     * Historique trié du plus récent, limité à 100 entrées.
     * Dédupliqué : une seule entrée par chanson (la plus récente).
     */
    @Query("""
        SELECT h.id   AS historyId,
               MAX(h.playedAt) AS playedAt,
               s.id   AS songId,
               s.title, s.artist, s.thumbnailUrl, s.durationSeconds, s.videoUrl
        FROM history h
        INNER JOIN songs s ON h.songId = s.id
        GROUP BY h.songId
        ORDER BY MAX(h.playedAt) DESC
        LIMIT 100
    """)
    fun getHistory(): Flow<List<HistoryWithSong>>

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
