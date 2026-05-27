package com.louis.musix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.louis.musix.data.local.entity.FavoriteEntity
import com.louis.musix.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)

    /** Réactif : émet true/false à chaque changement du favori. */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavorite(songId: String): Flow<Boolean>

    /** Tous les morceaux favoris, triés du plus récent. */
    @Query("""
        SELECT s.id, s.title, s.artist, s.thumbnailUrl, s.durationSeconds, s.videoUrl
        FROM favorites f
        INNER JOIN songs s ON f.songId = s.id
        ORDER BY f.favoritedAt DESC
    """)
    fun getFavoriteSongs(): Flow<List<SongEntity>>
}
