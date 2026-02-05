package com.quezic.domain.model

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val error: String? = null,
    // When true, YouTube app playback is available as fallback
    val canOpenInYouTube: Boolean = false,
    val youtubeVideoId: String? = null
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f

    val hasNext: Boolean
        get() = currentIndex < queue.lastIndex || repeatMode == RepeatMode.ALL

    val hasPrevious: Boolean
        get() = currentIndex > 0 || repeatMode == RepeatMode.ALL
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}
