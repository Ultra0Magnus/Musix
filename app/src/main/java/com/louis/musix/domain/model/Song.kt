package com.louis.musix.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val videoUrl: String,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
) {
    val durationText: String
        get() {
            val m = durationSeconds / 60
            val s = durationSeconds % 60
            return String.format("%d:%02d", m, s)
        }
}

}
