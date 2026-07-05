package com.louis.musix.player.cache

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Manages ExoPlayer's streaming cache.
 *
 * Caches up to [MAX_CACHE_SIZE_MB] MB of streamed audio on disk.
 * When the limit is reached, the least recently used files are automatically evicted.
 */
@UnstableApi
class CacheManager(context: Context) {

    companion object {
        private const val CACHE_DIR_NAME = "media_cache"
        private const val MAX_CACHE_SIZE_MB = 300L
        private const val MAX_CACHE_SIZE_BYTES = MAX_CACHE_SIZE_MB * 1024 * 1024
    }

    /** The physical cache directory on disk. */
    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)

    /** Evicts least recently used entries when [MAX_CACHE_SIZE_BYTES] is reached. */
    private val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)

    /** The actual SimpleCache instance backed by a standalone SQLite database. */
    val simpleCache: SimpleCache = SimpleCache(
        cacheDir,
        evictor,
        StandaloneDatabaseProvider(context),
    )

    /**
     * Factory that wraps HTTP data sources with caching.
     *
     * - Reads from cache when available (instant, no network).
     * - Writes to cache on the fly when streaming new content.
     * - Falls back gracefully if cache is corrupted.
     */
    val cacheDataSourceFactory: CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(simpleCache)
        .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
        .setFlags(
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
        )

    /**
     * Clears all cached media content.
     *
     * Removes every cached resource one by one instead of releasing the
     * [SimpleCache]: the instance stays valid, so [cacheDataSourceFactory]
     * (already bound to it by the player service) keeps working afterwards.
     */
    fun clearCache() {
        simpleCache.keys.toList().forEach { key ->
            simpleCache.removeResource(key)
        }
    }

    /**
     * Returns the current cache size in bytes.
     */
    val cacheSizeBytes: Long
        get() = simpleCache.cacheSpace
}
