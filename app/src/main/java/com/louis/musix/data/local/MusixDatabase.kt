package com.louis.musix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.louis.musix.data.local.dao.FavoriteDao
import com.louis.musix.data.local.dao.HistoryDao
import com.louis.musix.data.local.dao.PlaylistDao
import com.louis.musix.data.local.dao.SongDao
import com.louis.musix.data.local.entity.FavoriteEntity
import com.louis.musix.data.local.entity.HistoryEntity
import com.louis.musix.data.local.entity.PlaylistEntity
import com.louis.musix.data.local.entity.PlaylistSongEntity
import com.louis.musix.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MusixDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        fun create(context: Context): MusixDatabase =
            Room.databaseBuilder(context, MusixDatabase::class.java, "musix.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
