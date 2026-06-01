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

// ── Lyrics search result ──────────────────────────────────────────────────────

sealed interface LyricsResult {
    /** Synced lyrics with timestamps. */
    data class Synced(val lines: List<LyricLine>)  : LyricsResult
    /** Plain lyrics without timestamps. */
    data class Plain(val text: String)              : LyricsResult
    /** Instrumental track (no lyrics). */
    data object Instrumental                        : LyricsResult
    /** Lyrics not found for this track. */
    data object NotFound                            : LyricsResult
}

// ── LRCLIB response model ─────────────────────────────────────────────────────

@Serializable
private data class LrcLibResponse(
    val syncedLyrics: String? = null,
    val plainLyrics:  String? = null,
    val instrumental: Boolean = false,
)

// ── Repository ────────────────────────────────────────────────────────────────

/**
 * Fetches lyrics from [lrclib.net](https://lrclib.net) — free API, no key required.
 *
 * Priority: synced lyrics (LRC) > plain text > instrumental > not found.
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
                Log.d(TAG, "Lyrics not found (${response.code})")
                return@withContext LyricsResult.NotFound
            }

            val body = response.body?.string()
                ?: return@withContext LyricsResult.NotFound

            val data = json.decodeFromString<LrcLibResponse>(body)

            when {
                data.instrumental -> {
                    Log.d(TAG, "Instrumental track")
                    LyricsResult.Instrumental
                }
                !data.syncedLyrics.isNullOrBlank() -> {
                    val lines = parseLrc(data.syncedLyrics)
                    Log.d(TAG, "${lines.size} synced lines")
                    LyricsResult.Synced(lines)
                }
                !data.plainLyrics.isNullOrBlank() -> {
                    Log.d(TAG, "Plain lyrics available")
                    LyricsResult.Plain(data.plainLyrics)
                }
                else -> {
                    Log.d(TAG, "Empty response")
                    LyricsResult.NotFound
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getLyrics failed: ${e.message}")
            LyricsResult.NotFound
        }
    }

    // ── LRC parsing ───────────────────────────────────────────────────────────

    /**
     * Parses the LRC format:
     * ```
     * [02:34.56] Line text
     * ```
     * Timestamp fields may have 2 or 3 digits for centiseconds/milliseconds.
     * `internal` for unit testing.
     */
    internal fun parseLrc(lrc: String): List<LyricLine> {
        val regex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\]\s?(.*)$""")
        return lrc.lines()
            .mapNotNull { line ->
                regex.find(line.trim())?.let { match ->
                    val (min, sec, sub, text) = match.destructured
                    // Normalize to milliseconds (2 digits → centiseconds, 3 → milliseconds)
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
