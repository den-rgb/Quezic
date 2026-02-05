package com.quezic.domain.model

/**
 * Represents a track from a Spotify playlist.
 */
data class SpotifyTrack(
    val name: String,
    val artist: String,
    val album: String?,
    val durationMs: Long
) {
    /**
     * Creates a search query string for finding this track on other platforms.
     */
    fun toSearchQuery(): String = "$artist $name"
    
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

/**
 * Represents a Spotify playlist with its metadata and tracks.
 */
data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val ownerName: String?,
    val tracks: List<SpotifyTrack>
) {
    val trackCount: Int get() = tracks.size
    
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
    
    val formattedTotalDuration: String
        get() {
            val totalMinutes = totalDurationMs / 1000 / 60
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes} min"
            }
        }
}

/**
 * Result of attempting to match a Spotify track to a local source.
 */
sealed class MatchResult {
    /**
     * Successfully matched with a search result.
     * @param searchResult The matched result from YouTube/SoundCloud
     * @param confidence Confidence score from 0.0 to 1.0
     */
    data class Matched(
        val searchResult: SearchResult, 
        val confidence: Float
    ) : MatchResult()
    
    /**
     * Multiple potential matches found, user should choose.
     */
    data class MultipleOptions(
        val options: List<SearchResult>
    ) : MatchResult()
    
    /**
     * No suitable match was found.
     */
    object NotFound : MatchResult()
    
    /**
     * User chose to skip this track.
     */
    object Skipped : MatchResult()
}

/**
 * State of a single track during the matching process.
 */
data class TrackMatchState(
    val spotifyTrack: SpotifyTrack,
    val result: MatchResult = MatchResult.NotFound,
    val isProcessing: Boolean = false,
    val selectedResult: SearchResult? = null
) {
    val isMatched: Boolean 
        get() = result is MatchResult.Matched || selectedResult != null
    
    val isSkipped: Boolean 
        get() = result is MatchResult.Skipped
    
    val displayResult: SearchResult?
        get() = selectedResult ?: (result as? MatchResult.Matched)?.searchResult
}
