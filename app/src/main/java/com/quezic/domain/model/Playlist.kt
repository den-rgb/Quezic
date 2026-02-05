package com.quezic.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val totalDuration: Long = 0, // in milliseconds
    val isSmartPlaylist: Boolean = false,
    val smartPlaylistType: SmartPlaylistType? = null
) {
    val formattedDuration: String
        get() {
            val totalMinutes = totalDuration / 1000 / 60
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes} min"
            }
        }
}

enum class SmartPlaylistType {
    RECENTLY_PLAYED,
    MOST_PLAYED,
    FAVORITES,
    RECENTLY_ADDED
}
