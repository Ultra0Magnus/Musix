package com.louis.musix.data.repo

import com.louis.musix.data.local.dao.FavoriteDao
import com.louis.musix.data.local.dao.HistoryDao
import com.louis.musix.data.local.dao.PlaylistDao
import com.louis.musix.data.local.dao.SongDao
import com.louis.musix.data.local.entity.FavoriteEntity
import com.louis.musix.data.local.entity.HistoryEntity
import com.louis.musix.data.local.entity.PlaylistEntity
import com.louis.musix.data.local.entity.PlaylistSongEntity
import com.louis.musix.data.local.entity.SongEntity
import com.louis.musix.domain.model.Playlist
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Point d'entrée unique pour toutes les opérations de bibliothèque locale.
 * Cache les chansons, gère les favoris, l'historique et les playlists.
 */
class LibraryRepository(
    private val songDao: SongDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao,
    private val playlistDao: PlaylistDao,
) {

    // ─── Cache chanson (nécessaire avant toute opération FK) ──────────────────

    suspend fun cacheSong(song: Song) = songDao.upsert(SongEntity.fromDomain(song))

    // ─── Favoris ──────────────────────────────────────────────────────────────

    val favoriteSongs: Flow<List<Song>> =
        favoriteDao.getFavoriteSongs().map { list -> list.map { it.toDomain() } }

    fun isFavorite(songId: String): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun toggleFavorite(song: Song) {
        cacheSong(song)
        if (favoriteDao.isFavorite(song.id).first()) {
            favoriteDao.removeFavorite(song.id)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(song.id))
        }
    }

    // ─── Historique ───────────────────────────────────────────────────────────

    val history: Flow<List<Song>> = historyDao.getHistory().map { list ->
        list.map { entry ->
            Song(
                id              = entry.songId,
                title           = entry.title,
                artist          = entry.artist,
                thumbnailUrl    = entry.thumbnailUrl,
                durationSeconds = entry.durationSeconds,
                videoUrl        = entry.videoUrl,
            )
        }
    }

    suspend fun logHistory(song: Song) {
        cacheSong(song)
        historyDao.insert(HistoryEntity(songId = song.id))
    }

    suspend fun clearHistory() = historyDao.clearAll()

    // ─── Playlists ────────────────────────────────────────────────────────────

    val playlists: Flow<List<Playlist>> =
        playlistDao.getAllPlaylistsWithCount().map { list ->
            list.map { Playlist(it.id, it.name, it.createdAt, it.songCount) }
        }

    suspend fun createPlaylist(name: String): Long =
        playlistDao.createPlaylist(PlaylistEntity(name = name.trim()))

    suspend fun deletePlaylist(playlistId: Long) =
        playlistDao.deletePlaylist(playlistId)

    suspend fun renamePlaylist(playlistId: Long, name: String) =
        playlistDao.renamePlaylist(playlistId, name.trim())

    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> =
        playlistDao.getPlaylistSongs(playlistId).map { list -> list.map { it.toDomain() } }

    suspend fun getPlaylistName(playlistId: Long): String? =
        playlistDao.getPlaylistName(playlistId)

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        cacheSong(song)
        val position = playlistDao.getSongCount(playlistId)
        playlistDao.addSong(PlaylistSongEntity(playlistId, song.id, position))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) =
        playlistDao.removeSong(playlistId, songId)
}
