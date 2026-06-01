package com.louis.musix.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the LRC parser in [LyricsRepository].
 * Guards timestamp parsing (2- vs 3-digit fractions), ordering, and blank filtering.
 */
class LrcParserTest {

    private val repo = LyricsRepository()

    @Test
    fun `parses centisecond timestamps`() {
        val lines = repo.parseLrc("[00:12.34]Hello")
        assertEquals(1, lines.size)
        // 12s + 340ms = 12340ms
        assertEquals(12_340L, lines[0].timeMs)
        assertEquals("Hello", lines[0].text)
    }

    @Test
    fun `parses millisecond timestamps`() {
        val lines = repo.parseLrc("[01:02.345]World")
        // 1min 2s + 345ms = 62345ms
        assertEquals(62_345L, lines[0].timeMs)
    }

    @Test
    fun `sorts lines by timestamp`() {
        val lrc = """
            [00:30.00]Third
            [00:10.00]First
            [00:20.00]Second
        """.trimIndent()
        val lines = repo.parseLrc(lrc)
        assertEquals(listOf("First", "Second", "Third"), lines.map { it.text })
    }

    @Test
    fun `skips blank lines`() {
        val lrc = """
            [00:01.00]Real
            [00:02.00]
        """.trimIndent()
        val lines = repo.parseLrc(lrc)
        assertEquals(1, lines.size)
        assertEquals("Real", lines[0].text)
    }

    @Test
    fun `ignores lines without a timestamp`() {
        val lines = repo.parseLrc("just some plain text\nno brackets here")
        assertTrue(lines.isEmpty())
    }
}
