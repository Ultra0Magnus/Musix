package com.louis.musix.data.lyrics

import android.util.Log
import com.louis.musix.domain.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

private const val TAG      = "Musix.Lyrics"
private const val BASE_URL = "https://lrclib.net/api/get"

// ── Résultat de la recherche de paroles ───────────────────────────────────────

sealed interface LyricsResult {
    /** Paroles synchronisées avec timestamps. */
    data class Synced(val lines: List<LyricLine>)  : LyricsResult
    /** Paroles sans timestamps (texte brut). */
    data class Plain(val text: String)              : LyricsResult
    /** Morceau instrumental (pas de paroles). */
    data object Instrumental                        : LyricsResult
    /** Paroles introuvables pour ce morceau. */
    data object NotFound                            : LyricsResult
}

// ── Modèle de réponse LRCLIB ──────────────────────────────────────────────────

@Serializable
private data class LrcLibResponse(
    val syncedLyrics: String? = null,
    val plainLyrics:  String? = null,
    val instrumental: Boolean = false,
)

// ── Repository ────────────────────────────────────────────────────────────────

/**
 * Récupère les paroles depuis [lrclib.net](https://lrclib.net) — API gratuite, sans clé.
 *
 * Priorité : paroles synchronisées (LRC) > paroles brutes > instrumental > introuvable.
 */
class LyricsRepository {

    private val client = OkHttpClient()
    private val json   = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getLyrics(
        artist:          String,
        title:           String,
        durationSeconds: Long,
    ): LyricsResult = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(artist, title, durationSeconds)
            Log.d(TAG, "GET $url")

            val response = client.newCall(
                Request.Builder().url(url).build()
            ).execute()

            if (!response.isSuccessful) {
                Log.d(TAG, "Paroles introuvables (${response.code})")
                return@withContext LyricsResult.NotFound
            }

            val body = response.body?.string()
                ?: return@withContext LyricsResult.NotFound

            val data = json.decodeFromString<LrcLibResponse>(body)

            when {
                data.instrumental -> {
                    Log.d(TAG, "Morceau instrumental")
                    LyricsResult.Instrumental
                }
                !data.syncedLyrics.isNullOrBlank() -> {
                    val lines = parseLrc(data.syncedLyrics)
                    Log.d(TAG, "${lines.size} lignes synchronisées")
                    LyricsResult.Synced(lines)
                }
                !data.plainLyrics.isNullOrBlank() -> {
                    Log.d(TAG, "Paroles brutes disponibles")
                    LyricsResult.Plain(data.plainLyrics)
                }
                else -> {
                    Log.d(TAG, "Réponse vide")
                    LyricsResult.NotFound
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getLyrics KO : ${e.message}")
            LyricsResult.NotFound
        }
    }

    // ── Parsing LRC ───────────────────────────────────────────────────────────

    /**
     * Parse le format LRC :
     * ```
     * [02:34.56] Texte de la ligne
     * ```
     * Les champs horaires peuvent avoir 2 ou 3 chiffres pour les centièmes/millièmes.
     */
    private fun parseLrc(lrc: String): List<LyricLine> {
        val regex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\]\s?(.*)$""")
        return lrc.lines()
            .mapNotNull { line ->
                regex.find(line.trim())?.let { match ->
                    val (min, sec, sub, text) = match.destructured
                    // Normalise en millisecondes (2 chiffres → centièmes, 3 → millièmes)
                    val subMs = sub.padEnd(3, '0').toLong()
                    val timeMs = (min.toLong() * 60 + sec.toLong()) * 1_000L + subMs
                    LyricLine(timeMs, text.trim())
                }
            }
            .filter { it.text.isNotBlank() }
            .sortedBy { it.timeMs }
    }

    // ── URL builder ───────────────────────────────────────────────────────────

    private fun buildUrl(artist: String, title: String, durationSeconds: Long): String {
        val enc = { s: String -> URLEncoder.encode(s, "UTF-8") }
        return "$BASE_URL?artist_name=${enc(artist)}&track_name=${enc(title)}&duration=$durationSeconds"
    }
}
