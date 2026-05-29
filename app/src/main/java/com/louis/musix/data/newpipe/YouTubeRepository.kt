package com.louis.musix.data.newpipe

import android.util.Log
import com.louis.musix.domain.model.ArtistAlbum
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val TAG          = "Musix.YouTube"
private const val CACHE_TTL_MS = 5 * 60 * 60 * 1000L  // 5 hours (YouTube URLs expire in ~6h)

class YouTubeRepository {

    private val youtube = ServiceList.YouTube

    // ─── In-memory audio URL cache (TTL 5h) ───────────────────────────────────

    private data class CachedUrl(val url: String, val expiresAt: Long)
    private val urlCache = mutableMapOf<String, CachedUrl>()

    // ─── Search ────────────────────────────────────────────────────────────────

    /** Paginated result for a YouTube search. */
    data class SearchResult(
        val songs:    List<Song>,
        val nextPage: Page?,
        val query:    String,
    )

    /**
     * Paged search — returns the first page plus a [SearchResult.nextPage] token
     * to load more results via [searchMore].
     */
    suspend fun searchPaged(query: String): SearchResult = withContext(Dispatchers.IO) {
        val handler = youtube.searchQHFactory.fromQuery(query, listOf("music_songs"), "")
        val info    = SearchInfo.getInfo(youtube, handler)
        SearchResult(
            songs    = info.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toSong() },
            nextPage = info.nextPage,
            query    = query,
        )
    }

    /**
     * Loads the next page. NewPipe requires the same [SearchQueryHandler] as the
     * first page: we reconstruct it from the original query ([query]) then pass
     * the [nextPage] token returned by [searchPaged] / [searchMore].
     */
    suspend fun searchMore(query: String, nextPage: Page): SearchResult =
        withContext(Dispatchers.IO) {
            val handler = youtube.searchQHFactory.fromQuery(query, listOf("music_songs"), "")
            val more    = SearchInfo.getMoreItems(youtube, handler, nextPage)
            SearchResult(
                songs    = more.items.filterIsInstance<StreamInfoItem>().map { it.toSong() },
                nextPage = more.nextPage,
                query    = query,
            )
        }

    /**
     * Simple search (no pagination) — kept for the artist auto-queue in
     * [PlayerViewModel] and other internal uses.
     */
    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val searchHandler = youtube.searchQHFactory
            .fromQuery(query, listOf("music_songs"), "")
        val info = SearchInfo.getInfo(youtube, searchHandler)
        info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { it.toSong() }
    }

    // ─── Artist albums (YouTube Music) ────────────────────────────────────────

    /**
     * Searches for an artist's albums on YouTube Music.
     * Uses the "music_albums" filter → returns [PlaylistInfoItem] results.
     */
    suspend fun searchAlbums(artistName: String): List<ArtistAlbum> =
        withContext(Dispatchers.IO) {
            try {
                val handler = youtube.searchQHFactory
                    .fromQuery(artistName, listOf("music_albums"), "")
                val info = SearchInfo.getInfo(youtube, handler)
                info.relatedItems
                    .filterIsInstance<PlaylistInfoItem>()
                    .take(12)
                    .mapNotNull { it.toAlbum() }
                    .also { Log.d(TAG, "${it.size} albums for \"$artistName\"") }
            } catch (e: Exception) {
                Log.w(TAG, "searchAlbums(\"$artistName\") failed: ${e.message}")
                emptyList()
            }
        }

    /**
     * Fetches the tracks of an album from its YouTube Music playlist URL.
     * Returns only the first page (≈ 25 tracks) — sufficient for a standard album.
     */
    suspend fun getAlbumTracks(playlistUrl: String): List<Song> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "getAlbumTracks → $playlistUrl")
            val info = PlaylistInfo.getInfo(youtube, playlistUrl)
            info.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toSong() }
                .also { Log.d(TAG, "${it.size} tracks in album") }
        }

    // ─── YouTube Trending (kiosk) ──────────────────────────────────────────────

    /**
     * Returns YouTube trending videos via the default kiosk (Trending).
     * Uses the extractor directly to work around the "Could not get trending name"
     * bug in KioskInfo.getInfo() in v0.26.2.
     */
    suspend fun getTrending(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val kioskList = youtube.kioskList
            val kioskId = kioskList.defaultKioskId
            Log.d(TAG, "Trending kiosk id: $kioskId")
            val extractor = kioskList.getExtractorById(kioskId, null)
            extractor.fetchPage()
            val items = extractor.initialPage.items
            Log.d(TAG, "Trending: ${items.size} items")
            items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toSong() }
        } catch (e: Exception) {
            Log.e(TAG, "getTrending() failed: ${e.javaClass.simpleName} — ${e.message}")
            throw Exception("Failed to load trending: ${e.localizedMessage}")
        }
    }

    // ─── Audio extraction via NewPipeExtractor ─────────────────────────────────

    /**
     * Returns the direct audio stream URL for a YouTube video.
     * URLs are cached in memory with a 5h TTL (YouTube URLs expire in ~6h).
     * On a quick replay within the same session, no additional network call is made.
     */
    suspend fun getAudioStreamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeYouTubeUrl(videoUrl)
        val videoId       = extractVideoId(normalizedUrl) ?: normalizedUrl

        // ── Check cache ──────────────────────────────────────────────────────
        val cached = urlCache[videoId]
        if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
            Log.d(TAG, "✓ Audio URL from cache ($videoId)")
            return@withContext cached.url
        }

        // ── Fresh fetch ──────────────────────────────────────────────────────
        val url = fetchFreshAudioUrl(normalizedUrl)
        urlCache[videoId] = CachedUrl(url, System.currentTimeMillis() + CACHE_TTL_MS)
        Log.d(TAG, "URL cached ($videoId, expires in 5h)")
        url
    }

    /**
     * Network extraction — called only when the cache is empty or expired.
     *
     * Fully delegates to NewPipeExtractor v0.26.2 which handles internally:
     *  - InnerTube client selection (ANDROID, IOS, WEB…)
     *  - Signature decryption (cipher)
     *  - Anti-throttle `n` parameter
     */
    private fun fetchFreshAudioUrl(normalizedUrl: String): String {
        Log.d(TAG, "Audio extraction (network): $normalizedUrl")

        val streamInfo = try {
            StreamInfo.getInfo(youtube, normalizedUrl)
        } catch (e: Exception) {
            Log.e(TAG, "StreamInfo.getInfo() failed: ${e.javaClass.simpleName} — ${e.message}")
            throw Exception("Failed to load video: ${e.message}")
        }

        Log.d(TAG, "Title: ${streamInfo.name}")
        Log.d(TAG, "${streamInfo.audioStreams.size} audio streams, ${streamInfo.videoOnlyStreams.size} video-only streams, ${streamInfo.videoStreams.size} combined streams")
        if (streamInfo.errors.isNotEmpty()) {
            streamInfo.errors.forEach { Log.w(TAG, "Non-fatal error: ${it.javaClass.simpleName} — ${it.message}") }
        }

        // ── Priority 1: adaptive audio-only streams ──────────────────────────
        val audioOnlyStreams = streamInfo.audioStreams.filter { it.isUrl && it.content.isNotBlank() }

        if (audioOnlyStreams.isNotEmpty()) {
            val chosen = audioOnlyStreams.firstOrNull { it.format == MediaFormat.M4A }
                ?: audioOnlyStreams.firstOrNull { it.format == MediaFormat.WEBMA_OPUS || it.format == MediaFormat.WEBMA }
                ?: audioOnlyStreams.first()
            Log.d(TAG, "✓ Audio-only stream: ${chosen.format}, url=${chosen.content.take(80)}…")
            return chosen.content
        }

        // ── Priority 2: combined stream (audio + video in the same container) ─
        val combinedStreams = streamInfo.videoStreams.filter { it.isUrl && it.content.isNotBlank() }

        if (combinedStreams.isNotEmpty()) {
            val chosen = combinedStreams.first()
            Log.d(TAG, "✓ Combined stream (audio+video): ${chosen.format}, url=${chosen.content.take(80)}…")
            return chosen.content
        }

        Log.e(TAG, "No streams found: audioOnly=${streamInfo.audioStreams.size}, " +
                "combined=${streamInfo.videoStreams.size}, videoOnly=${streamInfo.videoOnlyStreams.size}")
        throw Exception("No streams available for this video")
    }

    // ─── NewPipe → Song conversion ─────────────────────────────────────────────

    private fun StreamInfoItem.toSong(): Song {
        val videoId = extractVideoId(url) ?: url
        val thumb = thumbnails.maxByOrNull { it.width }?.url ?: ""
        // Normalize the URL at search time — prevents music.youtube.com issues at playback
        return Song(
            id              = videoId,
            title           = name ?: "Unknown title",
            artist          = uploaderName ?: "Unknown artist",
            thumbnailUrl    = thumb,
            durationSeconds = duration,
            videoUrl        = normalizeYouTubeUrl(url),
        )
    }

    /**
     * Converts music.youtube.com → www.youtube.com.
     * NewPipeExtractor's YouTube service only handles streams on youtube.com.
     */
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

    // ─── PlaylistInfoItem → ArtistAlbum conversion ────────────────────────────

    private fun PlaylistInfoItem.toAlbum(): ArtistAlbum? {
        val albumUrl = url?.takeIf { it.isNotBlank() } ?: return null
        val artwork  = thumbnails.maxByOrNull { it.width }?.url ?: ""
        return ArtistAlbum(
            id          = albumUrl,
            name        = name ?: "Unknown album",
            artworkUrl  = artwork,
            trackCount  = streamCount.toInt().coerceAtLeast(0),
            playlistUrl = albumUrl,
        )
    }
}
