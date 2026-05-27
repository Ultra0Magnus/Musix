package com.louis.musix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.louis.musix.data.local.entity.SongEntity

@Dao
interface SongDao {
    /** Insère ou remplace un morceau (cache métadonnées). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)
}
