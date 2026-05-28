package com.louis.musix.data.newpipe

import android.util.Log
import com.louis.musix.domain.model.ArtistAlbum
import com.louis.musix.domain.model.Song
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

class YouTubeRepository {

    private val youtube = ServiceList.YouTube

    // ─── Recherche ──────────────────────────────────────────────────────────────

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val searchHandler = youtube.searchQHFactory
            .fromQuery(query, listOf("music_songs"), "")
        val info = SearchInfo.getInfo(youtube, searchHandler)
        info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { it.toSong() }
    }

    // ─── Albums d'un artiste (YouTube Music) ───────────────────────────────────

    /**
     * Recherche les albums d'un artiste sur YouTube Music.
     * Utilise le filtre "music_albums" → retourne des [PlaylistInfoItem].
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
                    .also { Log.d(TAG, "${it.size} albums pour « $artistName »") }
            } catch (e: Exception) {
                Log.w(TAG, "searchAlbums(\"$artistName\") KO : ${e.message}")
                emptyList()
            }
        }

    /**
     * Récupère les pistes d'un album à partir de l'URL de sa playlist YouTube Music.
     * Retourne seulement la première page (≈ 25 pistes) — suffisant pour un album classique.
     */
    suspend fun getAlbumTracks(playlistUrl: String): List<Song> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "getAlbumTracks → $playlistUrl")
            val info = PlaylistInfo.getInfo(youtube, playlistUrl)
            info.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { it.toSong() }
                .also { Log.d(TAG, "${it.size} pistes dans l'album") }
        }

    // ─── Tendances YouTube (kiosk) ──────────────────────────────────────────────

    /**
     * Retourne les vidéos tendance de YouTube via le kiosk par défaut (Trending).
     * Utilise l'extracteur directement pour éviter le bug "Could not get trending name"
     * de KioskInfo.getInfo() en v0.26.2.
     */
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

    // ─── Extraction audio via NewPipeExtractor ──────────────────────────────────

    /**
     * Retourne l'URL directe du flux audio pour une vidéo YouTube.
     *
     * Délègue entièrement à NewPipeExtractor v0.26.2 qui gère en interne :
     *  - Sélection du client InnerTube (ANDROID, IOS, WEB…)
     *  - Déchiffrement de signature (cipher)
     *  - Paramètre anti-throttle `n`
     *  - Détection de bot / poToken si disponible
     *
     * ⚠️ URLs expirantes (~6h) — toujours appeler au moment du play, jamais cacher.
     */
    suspend fun getAudioStreamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        // music.youtube.com/watch?v=ID → https://www.youtube.com/watch?v=ID
        // NewPipeExtractor ne gère pas music.youtube.com pour les streams
        val normalizedUrl = normalizeYouTubeUrl(videoUrl)
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

        // ── Priorité 1 : flux audio-only adaptatifs ─────────────────────────────
        // (nécessitent un appel InnerTube séparé — parfois bloqué par bot detection)
        val audioOnlyStreams = streamInfo.audioStreams.filter { it.isUrl && it.content.isNotBlank() }

        if (audioOnlyStreams.isNotEmpty()) {
            val chosen = audioOnlyStreams.firstOrNull { it.format == MediaFormat.M4A }
                ?: audioOnlyStreams.firstOrNull { it.format == MediaFormat.WEBMA_OPUS || it.format == MediaFormat.WEBMA }
                ?: audioOnlyStreams.first()
            Log.d(TAG, "✓ Flux audio-only : ${chosen.format}, url=${chosen.content.take(80)}…")
            return@withContext chosen.content
        }

        // ── Priorité 2 : flux combiné (audio + vidéo dans le même conteneur) ──
        // Format 18 (360p MP4 AAC 128kbps) — présent dans le HTML de la page YouTube.
        // ExoPlayer lit uniquement la piste audio → qualité identique à Spotify gratuit.
        val combinedStreams = streamInfo.videoStreams.filter { it.isUrl && it.content.isNotBlank() }

        if (combinedStreams.isNotEmpty()) {
            val chosen = combinedStreams.first()
            Log.d(TAG, "✓ Flux combiné (audio+vidéo) : ${chosen.format}, url=${chosen.content.take(80)}…")
            return@withContext chosen.content
        }

        // ── Aucun flux disponible ────────────────────────────────────────────────
        Log.e(TAG, "Aucun flux : audioOnly=${streamInfo.audioStreams.size}, " +
                "combined=${streamInfo.videoStreams.size}, " +
                "videoOnly=${streamInfo.videoOnlyStreams.size}")
        streamInfo.errors.forEach { Log.w(TAG, "Erreur non-fatale : ${it.message}") }
        throw Exception("Aucun flux disponible pour cette vidéo")
    }

    // ─── Conversion NewPipe → Song ──────────────────────────────────────────────

    private fun StreamInfoItem.toSong(): Song {
        val videoId = extractVideoId(url) ?: url
        val thumb = thumbnails.maxByOrNull { it.width }?.url ?: ""
        // Normaliser l'URL dès la recherche — évite music.youtube.com au moment du play
        return Song(
            id              = videoId,
            title           = name ?: "Titre inconnu",
            artist          = uploaderName ?: "Artiste inconnu",
            thumbnailUrl    = thumb,
            durationSeconds = duration,
            videoUrl        = normalizeYouTubeUrl(url),
        )
    }

    /**
     * Convertit music.youtube.com → www.youtube.com.
     * NewPipeExtractor's YouTube service ne gère les streams que sur youtube.com.
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

    // ─── Conversion PlaylistInfoItem → ArtistAlbum ─────────────────────────────

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
