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

// ── État de l'import ──────────────────────────────────────────────────────────

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

    // JSON parser commun (lenient pour tolérer les variations du format export)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        viewModelScope.launch {
            authManager.pendingCode.filterNotNull().collect { code ->
                authManager.clearPendingCode()
                _importState.value = ImportState.Exchanging
                val ok = authManager.exchangeCode(code)
                if (ok) startApiImport()
                else _importState.value = ImportState.Error(
                    "Echec de l'authentification Spotify. Réessaie.",
                )
            }
        }

        viewModelScope.launch {
            authManager.pendingError.filterNotNull().collect { error ->
                authManager.clearPendingError()
                _importState.value = ImportState.Error(
                    when (error) {
                        "access_denied" -> "Tu as refusé l'accès Spotify. Réessaie quand tu veux."
                        else            -> "Erreur Spotify : $error"
                    },
                )
            }
        }
    }

    // ── Actions publiques ──────────────────────────────────────────────────────

    /** Lance le flux OAuth (pour les comptes Premium). */
    fun connectAndImport() {
        if (authManager.isConnected()) {
            startApiImport()
        } else {
            _importState.value = ImportState.WaitingAuth
            authManager.launchAuth()
        }
    }

    /**
     * Import depuis les fichiers JSON de l'export RGPD Spotify.
     * [files] = Map<nom_du_fichier, contenu_json>
     */
    fun importFromJsonFiles(files: Map<String, String>) {
        importJob = viewModelScope.launch {
            try {
                var tracksImported   = 0
                var playlistsCreated = 0

                // ── Séparer les fichiers par type ─────────────────────────────
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

                // ── A. Historique d'écoute étendu ─────────────────────────────
                if (streamingFiles.isNotEmpty()) {
                    _importState.value = ImportState.Importing(
                        0, 0, "Analyse de ${streamingFiles.size} fichier(s) d'historique…",
                    )

                    // Parser tous les fichiers
                    val allEntries = mutableListOf<SpotifyStreamingEntry>()
                    streamingFiles.forEach { (filename, content) ->
                        try {
                            val entries = json.decodeFromString<List<SpotifyStreamingEntry>>(content)
                            allEntries.addAll(entries)
                            Log.d(TAG, "$filename : ${entries.size} entrées")
                        } catch (e: Exception) {
                            Log.w(TAG, "Erreur parsing $filename : ${e.message}")
                        }
                    }
                    Log.d(TAG, "Total entrées brutes : ${allEntries.size}")

                    // Garder uniquement les vraies écoutes (≥ 30s, titre connu)
                    val realPlays = allEntries.filter { e ->
                        !e.trackName.isNullOrBlank() &&
                        !e.artistName.isNullOrBlank() &&
                        e.msPlayed >= 30_000L
                    }
                    Log.d(TAG, "Écoutes réelles (≥30s) : ${realPlays.size}")

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

                    // ── Top 20 par année (3 dernières années avec données) ────
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

                // ── B. YourLibrary.json → playlist "Spotify — Titres aimés" ──
                if (libraryEntry != null) {
                    _importState.value = ImportState.Importing(0, 0, "Lecture de YourLibrary.json…")
                    try {
                        val library = json.decodeFromString<SpotifyLibraryExport>(libraryEntry.value)
                        val tracks  = library.tracks.filter { it.track.isNotBlank() }
                        Log.d(TAG, "${tracks.size} titres dans YourLibrary.json")

                        if (tracks.isNotEmpty()) {
                            val playlistId = libraryRepo.createPlaylist("Spotify — Titres aimés")
                            playlistsCreated++
                            tracks.forEachIndexed { i, t ->
                                _importState.value = ImportState.Importing(
                                    i + 1, tracks.size, "Titres aimés",
                                )
                                searchAndSave("${t.track} ${t.artist}") { song ->
                                    libraryRepo.addSongToPlaylist(playlistId, song)
                                    tracksImported++
                                }
                                delay(300)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Erreur YourLibrary.json : ${e.message}")
                    }
                }

                // ── C. Playlist*.json → playlists individuelles ───────────────
                playlistFiles.forEach { (filename, content) ->
                    try {
                        val playlist   = json.decodeFromString<SpotifyPlaylistExport>(content)
                        val validItems = playlist.items.mapNotNull { it.track }
                            .filter { it.trackName.isNotBlank() }
                        Log.d(TAG, "\"${playlist.name}\" : ${validItems.size} titres ($filename)")

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
                        Log.w(TAG, "Erreur $filename : ${e.message}")
                    }
                }

                _importState.value = ImportState.Done(tracksImported, playlistsCreated)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Import JSON KO : ${e.message}")
                _importState.value = ImportState.Error(e.message ?: "Erreur lors de l'import")
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

    // ── Import via API Spotify (Premium requis) ───────────────────────────────

    private fun startApiImport() {
        importJob = viewModelScope.launch {
            try {
                var tracksImported   = 0
                var playlistsCreated = 0

                _importState.value = ImportState.Importing(0, 0, "Récupération des titres aimés...")
                val savedTracks = spotifyRepo.getSavedTracks()
                Log.d(TAG, "${savedTracks.size} titres aimés reçus de l'API")

                if (savedTracks.isNotEmpty()) {
                    val likedId = libraryRepo.createPlaylist("Spotify - Titres aimés")
                    playlistsCreated++
                    savedTracks.forEachIndexed { i, track ->
                        _importState.value = ImportState.Importing(
                            i + 1, savedTracks.size, "Titres aimés",
                        )
                        importApiTrack(track) { song ->
                            libraryRepo.addSongToPlaylist(likedId, song)
                            tracksImported++
                        }
                        delay(300)
                    }
                }

                _importState.value = ImportState.Importing(0, 0, "Récupération des playlists...")
                val spotifyPlaylists = spotifyRepo.getPlaylists()
                Log.d(TAG, "${spotifyPlaylists.size} playlists reçues de l'API")

                spotifyPlaylists.forEach { spotifyPlaylist ->
                    val musixId = libraryRepo.createPlaylist(spotifyPlaylist.name)
                    playlistsCreated++

                    val tracks = try {
                        spotifyRepo.getPlaylistTracks(spotifyPlaylist.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Erreur playlist \"${spotifyPlaylist.name}\": ${e.message}")
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
                Log.e(TAG, "Import API KO : ${e.javaClass.simpleName} — ${e.message}")
                _importState.value = ImportState.Error(
                    e.message ?: "Erreur inconnue lors de l'import",
                )
            }
        }
    }

    private suspend fun importApiTrack(track: SpotifyTrack, onSuccess: suspend (Song) -> Unit) {
        val query = "${track.name} ${track.artists.firstOrNull()?.name ?: ""}".trim()
        searchAndSave(query, onSuccess)
    }

    // ── Recherche YouTube commune ─────────────────────────────────────────────

    private suspend fun searchAndSave(query: String, onSuccess: suspend (Song) -> Unit) {
        if (query.isBlank()) return
        try {
            val results = youtubeRepo.search(query)
            val song    = results.firstOrNull() ?: return
            onSuccess(song)
        } catch (_: Exception) { /* Titre ignoré — on continue */ }
    }
}
