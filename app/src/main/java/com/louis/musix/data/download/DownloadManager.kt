package com.louis.musix.data.download

import android.content.Context
import android.util.Log
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

private const val TAG = "Musix.DownloadManager"

class DownloadManager(
    private val context: Context,
    private val youtubeRepo: YouTubeRepository,
    private val libraryRepo: LibraryRepository,
) {
    private val httpClient = OkHttpClient()
    private val downloadDir = File(context.filesDir, "downloads").apply { mkdirs() }

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()

    suspend fun toggleDownload(song: Song) {
        if (song.isDownloaded && song.localFilePath != null) {
            removeDownload(song)
        } else {
            downloadSong(song)
        }
    }

    private suspend fun downloadSong(song: Song) = withContext(Dispatchers.IO) {
        if (_downloadingIds.value.contains(song.id)) return@withContext

        _downloadingIds.update { it + song.id }
        Log.d(TAG, "Starting download for ${song.title}...")

        try {
            // First, ensure song is cached in DB
            libraryRepo.cacheSong(song)
            
            // Get fresh stream URL
            val streamUrl = youtubeRepo.getAudioStreamUrl(song.videoUrl)
            
            val fileName = "${song.id}.m4a"
            val destFile = File(downloadDir, fileName)
            
            val request = Request.Builder().url(streamUrl).build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) throw Exception("Failed to download: HTTP ${response.code}")
            
            response.body?.byteStream()?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Update DB with local path
            val localUri = destFile.absolutePath
            libraryRepo.updateDownloadStatus(song.id, true, localUri)
            Log.d(TAG, "Download complete: ${song.title}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${song.title}: ${e.message}")
        } finally {
            _downloadingIds.update { it - song.id }
        }
    }

    private suspend fun removeDownload(song: Song) = withContext(Dispatchers.IO) {
        try {
            song.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            libraryRepo.updateDownloadStatus(song.id, false, null)
            Log.d(TAG, "Removed download: ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove download: ${e.message}")
        }
    }
}
