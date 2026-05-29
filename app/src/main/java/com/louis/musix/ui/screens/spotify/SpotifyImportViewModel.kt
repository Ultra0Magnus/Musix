package com.louis.musix.ui.screens.spotify

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.data.spotify.SpotifyAuthManager
import com.louis.musix.data.spotify.SpotifyRepository
import com.louis.musix.data.spotify.model.SpotifyExportItem
import com.louis.musix.data.spotify.model.SpotifyExportTrack
import com.louis.musix.data.spotify.model.SpotifyLibraryExport
import com.louis.musix.data.spotify.model.SpotifyPlaylistExport
import com.louis.musix.data.spotify.model.SpotifyStreamingEntry
import com.louis.musix.data.spotify.model.SpotifyTrack
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val TAG = "Musix.SpotifyImport"

// ── Import state ──────────────────────────────────────────────────────────────

sealed interface ImportState {
    data object Idle        : ImportState
    data object WaitingAuth : ImportState
    data object Exchanging  : ImportState
    data class  Importing(
        val current: Int,
        val total:   Int,
        val phase:   String,
    ) : ImportState
    data class  Done(
        val tracksImported:   Int,
        val playlistsCreated: Int,
    ) : ImportState
    data class  Error(val message: String) : ImportState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SpotifyImportViewModel(
    private val authManager: SpotifyAuthManager,
    private val spotifyRepo: SpotifyRepository,
    private val youtubeRepo: YouTubeRepository,
    private val libraryRepo: LibraryRepository,
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    val isConnected: Boolean get() = authManager.isConnected()

    private var importJob: Job? = null

    // Shared JSON parser (lenient to tolerate export format variations)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        viewModelScope.launch {
            authManager.pendingCode.filterNotNull().collect { code ->
                authManager.clearPendingCode()
                _importState.value = ImportState.Exchanging
                val ok = authManager.exchangeCode(code)
                if (ok) startApiImport()
                else _importState.value = ImportState.Error(
                    "Spotify authentication failed. Please try again.",
                )
            }
        }

        viewModelScope.launch {
            authManager.pendingError.filterNotNull().collect { error ->
                authManager.clearPendingError()
                _importState.value = ImportState.Error(
                    when (error) {
                        "access_denied" -> "You denied Spotify access. Try again whenever you like."
                        else            -> "Spotify error: $error"
                    },
                )
            }
        }
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /** Starts the OAuth flow (for Premium accounts). */
    fun connectAndImport() {
        if (authManager.isConnected()) {
            startApiImport()
        } else {
            _importState.value = ImportState.WaitingAuth
            authManager.launchAuth()
        }
    }

    /**
     * Import from Spotify GDPR export JSON files.
     * [files] = Map<filename, json_content>
     */
    fun importFromJsonFiles(files: Map<String, String>) {
        importJob = viewModelScope.launch {
            try {
                var tracksImported   = 0
                var playlistsCreated = 0

                // ── Separate files by type ────────────────────────────────────
                val streamingFiles = files.entries.filter {
                    it.key.contains("Streaming_History_Audio", ignoreCase = true)
                }
                val libraryEntry = files.entries.firstOrNull {
                    it.key.contains("YourLibrary", ignoreCase = true)
                }
                val playlistFiles = files.entries.filter { (name, _) ->
                    !name.contains("Streaming_History", ignoreCase = true) &&
                    !name.contains("YourLibrary", ignoreCase = true) &&
                    !name.contains("Streaming_History_Video", ignoreCase = true)
                }

                // ── A. Extended streaming history ─────────────────────────────
                if (streamingFiles.isNotEmpty()) {
                    _importState.value = ImportState.Importing(
                        0, 0, "Analysing ${streamingFiles.size} history file(s)…",
                    )

                    // Parse all files
                    val allEntries = mutableListOf<SpotifyStreamingEntry>()
                    streamingFiles.forEach { (filename, content) ->
                        try {
                            val entries = json.decodeFromString<List<SpotifyStreamingEntry>>(content)
                            allEntries.addAll(entries)
                            Log.d(TAG, "$filename: ${entries.size} entries")
                        } catch (e: Exception) {
                            Log.w(TAG, "Parse error $filename: ${e.message}")
                        }
                    }
                    Log.d(TAG, "Total raw entries: ${allEntries.size}")

                    // Keep only real plays (≥ 30s, known track)
                    val realPlays = allEntries.filter { e ->
                        !e.trackName.isNullOrBlank() &&
                        !e.artistName.isNullOrBlank() &&
                        e.msPlayed >= 30_000L
                    }
                    Log.d(TAG, "Real plays (≥30s): ${realPlays.size}")

                    // ── Top 50 all-time ───────────────────────────────────────
                    data class TrackStats(val track: String, val artist: String, val totalMs: Long)

                    val topAllTime = realPlays
                        .groupBy { "${it.trackName}|||${it.artistName}" }
                        .map { (_, plays) ->
                            TrackStats(
                                plays.first().trackName!!,
                                plays.first().artistName!!,
                                plays.sumOf { it.msPlayed },
                            )
                        }
                        .sortedByDescending { it.totalMs }
                        .take(50)

                    if (topAllTime.isNotEmpty()) {
                        val playlistId = libraryRepo.createPlaylist("Spotify — Top 50 All Time")
                        playlistsCreated++
                        topAllTime.forEachIndexed { i, s ->
                            _importState.value = ImportState.Importing(
                                i + 1, topAllTime.size, "Top 50 all time…",
                            )
                            searchAndSave("${s.track} ${s.artist}") { song ->
                                libraryRepo.addSongToPlaylist(playlistId, song)
                                tracksImported++
                            }
                            delay(300)
                        }
                    }

                    // ── Top 20 by year (3 most recent years with data) ────────
                    val yearRegex = Regex("""^(\d{4})-""")
                    val recentYears = realPlays
                        .mapNotNull { yearRegex.find(it.ts)?.groupValues?.get(1) }
                        .toSet()
                        .sortedDescending()
                        .take(3)

                    recentYears.forEach { year ->
                        val yearPlays = realPlays.filter { it.ts.startsWith(year) }
                        val top20 = yearPlays
                            .groupBy { "${it.trackName}|||${it.artistName}" }
                            .map { (_, plays) ->
                                TrackStats(
                                    plays.first().trackName!!,
                                    plays.first().artistName!!,
                                    plays.sumOf { it.msPlayed },
                                )
                            }
                            .sortedByDescending { it.totalMs }
                            .take(20)

                        if (top20.isNotEmpty()) {
                            val playlistId = libraryRepo.createPlaylist("Spotify — Top 20 $year")
                            playlistsCreated++
                            top20.forEachIndexed { i, s ->
                                _importState.value = ImportState.Importing(
                                    i + 1, top20.size, "Top 20 $year…",
                                )
                                searchAndSave("${s.track} ${s.artist}") { song ->
                                    libraryRepo.addSongToPlaylist(playlistId, song)
                                    tracksImported++
                                }
                                delay(300)
                            }
                        }
                    }
                }

                // ── B. YourLibrary.json → "Spotify — Liked Tracks" playlist ──
                if (libraryEntry != null) {
                    _importState.value = ImportState.Importing(0, 0, "Reading YourLibrary.json…")
                    try {
                        val library = json.decodeFromString<SpotifyLibraryExport>(libraryEntry.value)
                        val tracks  = library.tracks.filter { it.track.isNotBlank() }
                        Log.d(TAG, "${tracks.size} tracks in YourLibrary.json")

                        if (tracks.isNotEmpty()) {
                            val playlistId = libraryRepo.createPlaylist("Spotify — Liked Tracks")
                            playlistsCreated++
                            tracks.forEachIndexed { i, t ->
                                _importState.value = ImportState.Importing(
                                    i + 1, tracks.size, "Liked tracks",
                                )
                                searchAndSave("${t.track} ${t.artist}") { song ->
                                    libraryRepo.addSongToPlaylist(playlistId, song)
                                    tracksImported++
                                }
                                delay(300)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "YourLibrary.json error: ${e.message}")
                    }
                }

                // ── C. Playlist*.json → individual playlists ──────────────────
                playlistFiles.forEach { (filename, content) ->
                    try {
                        val playlist   = json.decodeFromString<SpotifyPlaylistExport>(content)
                        val validItems = playlist.items.mapNotNull { it.track }
                            .filter { it.trackName.isNotBlank() }
                        Log.d(TAG, "\"${playlist.name}\": ${validItems.size} tracks ($filename)")

                        if (validItems.isEmpty()) return@forEach

                        val playlistId = libraryRepo.createPlaylist(
                            playlist.name.ifBlank { filename.removeSuffix(".json") },
                        )
                        playlistsCreated++

                        validItems.forEachIndexed { i, t ->
                            _importState.value = ImportState.Importing(
                                i + 1, validItems.size, "\"${playlist.name}\"",
                            )
                            searchAndSave("${t.trackName} ${t.artistName}") { song ->
                                libraryRepo.addSongToPlaylist(playlistId, song)
                                tracksImported++
                            }
                            delay(300)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error $filename: ${e.message}")
                    }
                }

                _importState.value = ImportState.Done(tracksImported, playlistsCreated)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "JSON import failed: ${e.message}")
                _importState.value = ImportState.Error(e.message ?: "Import error")
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        _importState.value = ImportState.Idle
    }

    fun retry() { _importState.value = ImportState.Idle }

    fun disconnect() {
        authManager.disconnect()
        _importState.value = ImportState.Idle
    }

    // ── API import (Premium required) ─────────────────────────────────────────

    private fun startApiImport() {
        importJob = viewModelScope.launch {
            try {
                var tracksImported   = 0
                var playlistsCreated = 0

                _importState.value = ImportState.Importing(0, 0, "Fetching liked tracks...")
                val savedTracks = spotifyRepo.getSavedTracks()
                Log.d(TAG, "${savedTracks.size} liked tracks received from API")

                if (savedTracks.isNotEmpty()) {
                    val likedId = libraryRepo.createPlaylist("Spotify - Liked Tracks")
                    playlistsCreated++
                    savedTracks.forEachIndexed { i, track ->
                        _importState.value = ImportState.Importing(
                            i + 1, savedTracks.size, "Liked tracks",
                        )
                        importApiTrack(track) { song ->
                            libraryRepo.addSongToPlaylist(likedId, song)
                            tracksImported++
                        }
                        delay(300)
                    }
                }

                _importState.value = ImportState.Importing(0, 0, "Fetching playlists...")
                val spotifyPlaylists = spotifyRepo.getPlaylists()
                Log.d(TAG, "${spotifyPlaylists.size} playlists received from API")

                spotifyPlaylists.forEach { spotifyPlaylist ->
                    val musixId = libraryRepo.createPlaylist(spotifyPlaylist.name)
                    playlistsCreated++

                    val tracks = try {
                        spotifyRepo.getPlaylistTracks(spotifyPlaylist.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error playlist \"${spotifyPlaylist.name}\": ${e.message}")
                        emptyList()
                    }

                    tracks.forEachIndexed { i, track ->
                        _importState.value = ImportState.Importing(
                            i + 1, tracks.size, "\"${spotifyPlaylist.name}\"",
                        )
                        importApiTrack(track) { song ->
                            libraryRepo.addSongToPlaylist(musixId, song)
                            tracksImported++
                        }
                        delay(300)
                    }
                }

                _importState.value = ImportState.Done(tracksImported, playlistsCreated)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "API import failed: ${e.javaClass.simpleName} — ${e.message}")
                _importState.value = ImportState.Error(
                    e.message ?: "Unknown import error",
                )
            }
        }
    }

    private suspend fun importApiTrack(track: SpotifyTrack, onSuccess: suspend (Song) -> Unit) {
        val query = "${track.name} ${track.artists.firstOrNull()?.name ?: ""}".trim()
        searchAndSave(query, onSuccess)
    }

    // ── Common YouTube search ─────────────────────────────────────────────────

    private suspend fun searchAndSave(query: String, onSuccess: suspend (Song) -> Unit) {
        if (query.isBlank()) return
        try {
            val results = youtubeRepo.search(query)
            val song    = results.firstOrNull() ?: return
            onSuccess(song)
        } catch (_: Exception) { /* Track skipped — continue */ }
    }
}
