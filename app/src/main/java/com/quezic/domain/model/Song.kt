package com.quezic.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long, // in milliseconds
    val thumbnailUrl: String? = null,
    val localPath: String? = null, // null if not downloaded
    val sourceType: SourceType,
    val sourceId: String, // Original source ID for re-fetching stream URL
    val sourceUrl: String? = null, // Direct playback URL (may expire)
    val genre: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val isFavorite: Boolean = false
) {
    val isDownloaded: Boolean
        get() = localPath != null

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

enum class SourceType {
    YOUTUBE,
    SOUNDCLOUD,
    BANDCAMP,
    LOCAL
}
