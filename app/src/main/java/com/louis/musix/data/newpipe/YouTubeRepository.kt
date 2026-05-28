package com.louis.musix.data.newpipe

import android.util.Log
import com.louis.musix.domain.model.ArtistAlbum
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val TAG = "Musix.YouTube"
private const val MAX_RETRIES = 2

// ─── Cache en mémoire (albums et résultats artiste) ─────────────────────────
// TTL de 10 minutes pour éviter des appels réseau lors des allers-retours de navigation.

private data class CacheEntry<T>(val value: T, val timestamp: Long = System.currentTimeMillis())
private const val CACHE_TTL_MS = 10 * 60 * 1_000L // 10 min

class YouTubeRepository {

    private val youtube = ServiceList.YouTube

    // Caches in-memory
    private val albumsCache  = mutableMapOf<String, CacheEntry<List<ArtistAlbum>>>()
    private val tracksCache  = mutableMapOf<String, CacheEntry<List<Song>>>()

    // ─── Recherche ──────────────────────────────────────────────────────────

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val searchHandler = youtube.searchQHFactory
            .fromQuery(query, listOf("music_songs"), "")
        val info = SearchInfo.getInfo(youtube, searchHandler)
        info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { it.toSong() }
    }

    // ─── Albums d'un artiste (YouTube Music) ────────────────────────────────

    suspend fun searchAlbums(artistName: String): List<ArtistAlbum> =
        withContext(Dispatchers.IO) {
            albumsCache[artistName]
                ?.takeIf { System.currentTimeMillis() - it.timestamp < CACHE_TTL_MS }
                ?.value
                ?.also { Log.d(TAG, "searchAlbums cache hit : $artistName") }
                ?: run {
                    try {
                        val handler = youtube.searchQHFactory
                            .fromQuery(artistName, listOf("music_albums"), "")
                        val info = SearchInfo.getInfo(youtube, handler)
                        val result = info.relatedItems
                            .filterIsInstance<PlaylistInfoItem>()
                            .take(12)
                            .mapNotNull { it.toAlbum() }
                            .also { Log.d(TAG, "${it.size} albums pour « $artistName »") }
                        albumsCache[artistName] = CacheEntry(result)
                        result
                    } catch (e: Exception) {
                        Log.w(TAG, "searchAlbums(\"$artistName\") KO : ${e.message}")
                        emptyList()
                    }
                }
        }

    /**
     * Récupère les pistes d'un album à partir de l'URL de sa playlist YouTube Music.
     * Résultat mis en cache 10 minutes.
     */
    suspend fun getAlbumTracks(playlistUrl: String): List<Song> =
        withContext(Dispatchers.IO) {
            tracksCache[playlistUrl]
                ?.takeIf { System.currentTimeMillis() - it.timestamp < CACHE_TTL_MS }
                ?.value
                ?.also { Log.d(TAG, "getAlbumTracks cache hit : $playlistUrl") }
                ?: run {
                    Log.d(TAG, "getAlbumTracks → $playlistUrl")
                    val info = PlaylistInfo.getInfo(youtube, playlistUrl)
                    val result = info.relatedItems
                        .filterIsInstance<StreamInfoItem>()
                        .map { it.toSong() }
                        .also { Log.d(TAG, "${it.size} pistes dans l'album") }
                    tracksCache[playlistUrl] = CacheEntry(result)
                    result
                }
        }

    // ─── Tendances YouTube (kiosk) ────────────────────────────────────────────

    suspend fun getTrending(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val kioskList = youtube.kioskList
            val kioskId = kioskList.defaultKioskId
            Log.d(TAG, "Trending kiosk id : $kioskId")
            val extractor = kioskList.getExtractorById(kioskId, null)
            extractor.fetchPage()
            val items = extractor.initialPage.items
            Log.d(TAG, "Trending : ${items.size} items")
            items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "getTrending() KO : ${e.javaClass.simpleName} — ${e.message}")
            throw Exception("Impossible de charger les tendances : ${e.localizedMessage}")
        }
    }

    // ─── Extraction audio avec retry ──────────────────────────────────────────

    /**
     * Retourne l'URL directe du flux audio pour une vidéo YouTube.
     * Retry automatique jusqu'à [MAX_RETRIES] fois en cas d'erreur transitoire
     * (timeout réseau, bot detection temporaire).
     *
     * ⚠️ URLs expirantes (~6h) — toujours appeler au moment du play, jamais cacher.
     */
    suspend fun getAudioStreamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeYouTubeUrl(videoUrl)
        var lastException: Exception? = null

        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                return@withContext extractAudioUrl(normalizedUrl)
            } catch (e: CancellationException) {
                throw e  // ne jamais retry sur une annulation de coroutine
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "getAudioStreamUrl tentative ${attempt + 1}/${MAX_RETRIES + 1} KO : ${e.message}")
            }
        }
        throw lastException ?: Exception("Aucun flux disponible")
    }

    private fun extractAudioUrl(normalizedUrl: String): String {
        Log.d(TAG, "Extraction audio : $normalizedUrl")

        val streamInfo = try {
            StreamInfo.getInfo(youtube, normalizedUrl)
        } catch (e: Exception) {
            Log.e(TAG, "StreamInfo.getInfo() KO : ${e.javaClass.simpleName} — ${e.message}")
            throw Exception("Impossible de charger la vidéo : ${e.message}")
        }

        Log.d(TAG, "Titre : ${streamInfo.name}")
        Log.d(TAG, "${streamInfo.audioStreams.size} flux audio, ${streamInfo.videoOnlyStreams.size} flux vidéo seul, ${streamInfo.videoStreams.size} flux combinés")
        if (streamInfo.errors.isNotEmpty()) {
            streamInfo.errors.forEach { Log.w(TAG, "Erreur non-fatale : ${it.javaClass.simpleName} — ${it.message}") }
        }

        val audioOnlyStreams = streamInfo.audioStreams.filter { it.isUrl && it.content.isNotBlank() }
        if (audioOnlyStreams.isNotEmpty()) {
            val chosen = audioOnlyStreams.firstOrNull { it.format == MediaFormat.M4A }
                ?: audioOnlyStreams.firstOrNull { it.format == MediaFormat.WEBMA_OPUS || it.format == MediaFormat.WEBMA }
                ?: audioOnlyStreams.first()
            Log.d(TAG, "✓ Flux audio-only : ${chosen.format}, url=${chosen.content.take(80)}…")
            return chosen.content
        }

        val combinedStreams = streamInfo.videoStreams.filter { it.isUrl && it.content.isNotBlank() }
        if (combinedStreams.isNotEmpty()) {
            val chosen = combinedStreams.first()
            Log.d(TAG, "✓ Flux combiné (audio+vidéo) : ${chosen.format}, url=${chosen.content.take(80)}…")
            return chosen.content
        }

        Log.e(TAG, "Aucun flux : audioOnly=${streamInfo.audioStreams.size}, combined=${streamInfo.videoStreams.size}")
        throw Exception("Aucun flux disponible pour cette vidéo")
    }

    // ─── Conversion NewPipe → Song ───────────────────────────────────────────

    private fun StreamInfoItem.toSong(): Song {
        val videoId = extractVideoId(url) ?: url
        val thumb = thumbnails.maxByOrNull { it.width }?.url ?: ""
        return Song(
            id              = videoId,
            title           = name ?: "Titre inconnu",
            artist          = uploaderName ?: "Artiste inconnu",
            thumbnailUrl    = thumb,
            durationSeconds = duration,
            videoUrl        = normalizeYouTubeUrl(url),
        )
    }

    private fun normalizeYouTubeUrl(url: String): String {
        if (!url.contains("music.youtube.com")) return url
        val videoId = extractVideoId(url) ?: return url
        return "https://www.youtube.com/watch?v=$videoId"
    }

    private fun extractVideoId(url: String): String? {
        Regex("""[?&]v=([^&\s]+)""").find(url)?.groupValues?.get(1)?.let { return it }
        Regex("""youtu\.be/([^?&\s]+)""").find(url)?.groupValues?.get(1)?.let { return it }
        Regex("""youtube\.com/shorts/([^?&\s]+)""").find(url)?.groupValues?.get(1)?.let { return it }
        return null
    }

    private fun PlaylistInfoItem.toAlbum(): ArtistAlbum? {
        val albumUrl = url?.takeIf { it.isNotBlank() } ?: return null
        val artwork  = thumbnails.maxByOrNull { it.width }?.url ?: ""
        return ArtistAlbum(
            id          = albumUrl,
            name        = name ?: "Album inconnu",
            artworkUrl  = artwork,
            trackCount  = streamCount.toInt().coerceAtLeast(0),
            playlistUrl = albumUrl,
        )
    }
}
