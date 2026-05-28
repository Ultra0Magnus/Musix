package com.louis.musix.data.spotify

import android.util.Log
import com.louis.musix.data.spotify.model.SpotifyPlaylistItem
import com.louis.musix.data.spotify.model.SpotifyPlaylistTracksResponse
import com.louis.musix.data.spotify.model.SpotifyPlaylistsResponse
import com.louis.musix.data.spotify.model.SpotifySavedTracksResponse
import com.louis.musix.data.spotify.model.SpotifyTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG      = "Musix.SpotifyRepo"
private const val BASE_URL = "https://api.spotify.com/v1"

/**
 * Accès à l'API Web Spotify (lecture seule).
 * Gère la pagination automatiquement — retourne des listes complètes.
 */
class SpotifyRepository(private val authManager: SpotifyAuthManager) {

    private val json       = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient()

    // ── Titres aimés ──────────────────────────────────────────────────────────

    suspend fun getSavedTracks(): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        val all = mutableListOf<SpotifyTrack>()
        var url: String? = "$BASE_URL/me/tracks?limit=50"
        while (url != null) {
            val page = json.decodeFromString<SpotifySavedTracksResponse>(get(url))
            all += page.items.mapNotNull { it.track }
            url  = page.next
        }
        Log.d(TAG, "${all.size} titres aimés récupérés")
        all
    }

    // ── Playlists de l'utilisateur ────────────────────────────────────────────

    suspend fun getPlaylists(): List<SpotifyPlaylistItem> = withContext(Dispatchers.IO) {
        val all = mutableListOf<SpotifyPlaylistItem>()
        var url: String? = "$BASE_URL/me/playlists?limit=50"
        while (url != null) {
            val page = json.decodeFromString<SpotifyPlaylistsResponse>(get(url))
            all += page.items
            url  = page.next
        }
        Log.d(TAG, "${all.size} playlists récupérées")
        all
    }

    // ── Titres d'une playlist ─────────────────────────────────────────────────

    suspend fun getPlaylistTracks(playlistId: String): List<SpotifyTrack> =
        withContext(Dispatchers.IO) {
            val all = mutableListOf<SpotifyTrack>()
            var url: String? = "$BASE_URL/playlists/$playlistId/tracks?limit=100"
            while (url != null) {
                val page = json.decodeFromString<SpotifyPlaylistTracksResponse>(get(url))
                all += page.items.mapNotNull { it.track }
                url  = page.next
            }
            all
        }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private suspend fun get(url: String): String {
        val token = authManager.getValidToken()
            ?: throw Exception("Token Spotify invalide — reconnecte-toi")

        Log.d(TAG, "GET ${url.take(80)}")

        val response = httpClient.newCall(
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .build(),
        ).execute()

        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            Log.e(TAG, "Erreur ${response.code} : ${body.take(300)}")
            throw Exception("API Spotify ${response.code} : ${body.take(200)}")
        }
        return body
    }
}
