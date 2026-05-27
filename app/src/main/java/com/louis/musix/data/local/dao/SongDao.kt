package com.louis.musix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.louis.musix.data.local.entity.SongEntity

@Dao
interface SongDao {
    /**
     * Insère un morceau dans le cache. Si le titre existe deja (meme videoId),
     * on garde l'existant — evite de declencher les CASCADE qui effaceraient
     * les favoris, l'historique et les playlists lies a ce titre.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsert(song: SongEntity)
}
