package com.louis.musix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true,
)
abstract class MusixDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {

        /** v0.9.1 offline mode: adds the download columns to `songs`. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN isDownloaded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE songs ADD COLUMN localFilePath TEXT")
            }
        }

        fun create(context: Context): MusixDatabase =
            Room.databaseBuilder(context, MusixDatabase::class.java, "musix.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
