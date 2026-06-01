package com.louis.musix.data.newpipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the pure URL helpers in [YouTubeRepository].
 * These guard the fragile video-id extraction and music→www normalization
 * that every playback path depends on.
 */
class YouTubeUrlTest {

    private val repo = YouTubeRepository()

    // ─── extractVideoId ─────────────────────────────────────────────────────

    @Test
    fun `extracts id from standard watch url`() {
        assertEquals("dQw4w9WgXcQ", repo.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `extracts id from watch url with extra params`() {
        assertEquals(
            "dQw4w9WgXcQ",
            repo.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=ABC&index=2"),
        )
    }

    @Test
    fun `extracts id from youtu_be short link`() {
        assertEquals("dQw4w9WgXcQ", repo.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `extracts id from shorts url`() {
        assertEquals("abc123XYZ", repo.extractVideoId("https://youtube.com/shorts/abc123XYZ"))
    }

    @Test
    fun `extracts id from music youtube url`() {
        assertEquals("dQw4w9WgXcQ", repo.extractVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `returns null when no id present`() {
        assertNull(repo.extractVideoId("https://example.com/nope"))
    }

    // ─── normalizeYouTubeUrl ────────────────────────────────────────────────

    @Test
    fun `normalizes music url to www`() {
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            repo.normalizeYouTubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"),
        )
    }

    @Test
    fun `leaves non-music url unchanged`() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals(url, repo.normalizeYouTubeUrl(url))
    }
}
