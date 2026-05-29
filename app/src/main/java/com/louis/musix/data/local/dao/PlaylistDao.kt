package com.louis.musix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.louis.musix.data.local.entity.PlaylistEntity
import com.louis.musix.data.local.entity.PlaylistSongEntity
import com.louis.musix.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/** Playlist enriched with the song count (aggregated JOIN). */
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int,
)

@Dao
interface PlaylistDao {

    // ─── Playlists ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    /** All playlists with their song count, sorted by creation date. */
    @Query("""
        SELECT p.id, p.name, p.createdAt, COUNT(ps.songId) AS songCount
        FROM playlists p
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    fun getAllPlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT name FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistName(playlistId: Long): String?

    // ─── Morceaux dans une playlist ───────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(entry: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: String)

    /** Songs from a playlist sorted by position. */
    @Query("""
        SELECT s.id, s.title, s.artist, s.thumbnailUrl, s.durationSeconds, s.videoUrl, s.isDownloaded, s.localFilePath
        FROM playlist_songs ps
        INNER JOIN songs s ON ps.songId = s.id
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC, ps.addedAt ASC
    """)
    fun getPlaylistSongs(playlistId: Long): Flow<List<SongEntity>>

    /** Number of songs in a playlist (to calculate the next position). */
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongCount(playlistId: Long): Int

    /** Find a playlist by its name (for the Spotify Top 50). */
    @Query("SELECT id FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistIdByName(name: String): Long?
}
