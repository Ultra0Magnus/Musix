package com.louis.musix.data.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Réponse token (échange code / refresh) ────────────────────────────────────

@Serializable
data class SpotifyTokenResponse(
    @SerialName("access_token")  val accessToken: String,
    @SerialName("token_type")    val tokenType: String    = "Bearer",
    @SerialName("expires_in")    val expiresIn: Int       = 3600,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

// ── Titres aimés  /v1/me/tracks ───────────────────────────────────────────────

@Serializable
data class SpotifySavedTracksResponse(
    val items: List<SpotifySavedTrackItem> = emptyList(),
    val next:  String? = null,
)

@Serializable
data class SpotifySavedTrackItem(
    val track: SpotifyTrack? = null,
)

// ── Playlists  /v1/me/playlists ───────────────────────────────────────────────

@Serializable
data class SpotifyPlaylistsResponse(
    val items: List<SpotifyPlaylistItem> = emptyList(),
    val next:  String? = null,
)

@Serializable
data class SpotifyPlaylistItem(
    val id:     String,
    val name:   String,
    val tracks: SpotifyTracksRef? = null,
)

@Serializable
data class SpotifyTracksRef(val total: Int = 0)

// ── Titres d'une playlist  /v1/playlists/{id}/tracks ─────────────────────────

@Serializable
data class SpotifyPlaylistTracksResponse(
    val items: List<SpotifyPlaylistTrackItem> = emptyList(),
    val next:  String? = null,
)

@Serializable
data class SpotifyPlaylistTrackItem(
    val track: SpotifyTrack? = null,
)

// ── Track & Artist ────────────────────────────────────────────────────────────

@Serializable
data class SpotifyTrack(
    val id:      String           = "",
    val name:    String           = "",
    val artists: List<SpotifyArtist> = emptyList(),
    @SerialName("duration_ms") val durationMs: Long = 0L,
)

@Serializable
data class SpotifyArtist(val name: String = "")

// ── Format d'export RGPD Spotify (ZIP reçu par mail) ─────────────────────────
// YourLibrary.json

@Serializable
data class SpotifyLibraryExport(
    val tracks: List<SpotifyExportTrack> = emptyList(),
)

@Serializable
data class SpotifyExportTrack(
    val artist: String = "",
    val album:  String = "",
    val track:  String = "",
)

// Playlist1.json … PlaylistN.json

@Serializable
data class SpotifyPlaylistExport(
    val name:  String                  = "",
    val items: List<SpotifyExportItem> = emptyList(),
)

@Serializable
data class SpotifyExportItem(
    val track: SpotifyExportItemTrack? = null,
)

@Serializable
data class SpotifyExportItemTrack(
    val trackName:  String = "",
    val artistName: String = "",
)

// ── Historique d'écoute étendu  Streaming_History_Audio_YYYY.json ─────────────

@Serializable
data class SpotifyStreamingEntry(
    val ts:                                    String  = "",
    @SerialName("master_metadata_track_name")
    val trackName:                             String? = null,
    @SerialName("master_metadata_album_artist_name")
    val artistName:                            String? = null,
    @SerialName("ms_played")
    val msPlayed:                              Long    = 0L,
    val skipped:                               Boolean? = null,
)
