package com.quezic.domain.recommendation

import android.util.Log
import com.quezic.data.remote.LastFmService
import com.quezic.data.remote.MusicExtractorService
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.Song
import com.quezic.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart recommendation engine using Last.fm for music intelligence
 * and YouTube/SoundCloud for playable content.
 * 
 * Uses:
 * - Collaborative filtering via Last.fm's similar artists/tracks
 * - Content-based filtering via genre tags
 * - Artist similarity scoring
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val extractorService: MusicExtractorService,
    private val lastFmService: LastFmService
) {
    
    companion object {
        private const val TAG = "RecommendationEngine"
    }

    /**
     * Get recommendations based on a list of songs using Last.fm music intelligence
     * Uses collaborative filtering (similar artists/tracks) and content-based filtering (genres)
     */
    suspend fun getRecommendations(
        songs: List<Song>,
        limit: Int = 10,
        forceRefresh: Boolean = false
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        if (songs.isEmpty()) return@withContext emptyList()

        Log.d(TAG, "Getting recommendations for ${songs.size} songs, forceRefresh=$forceRefresh")
        
        // Analyze the playlist to extract features
        val profile = analyzePlaylist(songs)
        val existingArtists = songs.map { it.artist.lowercase() }.toSet()
        val existingTitlesNormalized = songs.map { normalizeTitle(it.title) }.toSet()
        
        // Get recommendations from multiple strategies
        val recommendations = mutableListOf<ScoredResult>()
        
        // Strategy 1: Last.fm Similar Artists (Collaborative Filtering)
        // Get similar artists and search for their top tracks
        val artistsForSimilar = if (forceRefresh) {
            profile.topArtists.shuffled().take(3)
        } else {
            profile.topArtists.take(3)
        }
        
        artistsForSimilar.forEach { artistName ->
            try {
                val similarArtists = lastFmService.getSimilarArtists(artistName, limit = 5)
                Log.d(TAG, "Last.fm found ${similarArtists.size} similar artists for $artistName")
                
                similarArtists.forEach { similar ->
                    // Skip if we already have this artist
                    if (similar.name.lowercase() in existingArtists) return@forEach
                    
                    // Search YouTube for this artist's music
                    try {
                        val searchResults = extractorService.search(
                            "${similar.name} official audio",
                            listOf(SourceType.YOUTUBE)
                        )
                        searchResults.take(2).forEach { result ->
                            if (isValidRecommendation(result, existingArtists, existingTitlesNormalized)) {
                                recommendations.add(ScoredResult(
                                    result,
                                    0.7f + (similar.matchScore * 0.2f), // Higher score for better Last.fm match
                                    true
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // Continue
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get similar artists for $artistName: ${e.message}")
            }
        }
        
        // Strategy 2: Last.fm Similar Tracks (Track-based Collaborative Filtering)
        val songsForSimilar = if (forceRefresh) {
            songs.shuffled().take(3)
        } else {
            songs.take(3)
        }
        
        songsForSimilar.forEach { song ->
            try {
                val similarTracks = lastFmService.getSimilarTracks(song.artist, song.title, limit = 5)
                Log.d(TAG, "Last.fm found ${similarTracks.size} similar tracks for ${song.title}")
                
                similarTracks.forEach { similar ->
                    // Skip if we already have this artist
                    if (similar.artist.lowercase() in existingArtists) return@forEach
                    
                    // Search YouTube for this specific track
                    try {
                        val searchResults = extractorService.search(
                            "${similar.artist} ${similar.name} official",
                            listOf(SourceType.YOUTUBE)
                        )
                        searchResults.take(1).forEach { result ->
                            if (isValidRecommendation(result, existingArtists, existingTitlesNormalized)) {
                                recommendations.add(ScoredResult(
                                    result,
                                    0.8f + (similar.matchScore * 0.15f), // High score for track similarity
                                    true
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // Continue
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get similar tracks for ${song.title}: ${e.message}")
            }
        }
        
        // Strategy 3: Genre-based discovery using Last.fm tags (Content-Based Filtering)
        val artistForTags = profile.topArtists.firstOrNull()
        if (artistForTags != null) {
            try {
                val tags = lastFmService.getArtistTags(artistForTags)
                Log.d(TAG, "Last.fm tags for $artistForTags: $tags")
                
                tags.take(2).forEach { tag ->
                    try {
                        val searchResults = extractorService.search(
                            "$tag music official audio",
                            listOf(SourceType.YOUTUBE)
                        )
                        searchResults.take(3).forEach { result ->
                            if (isValidRecommendation(result, existingArtists, existingTitlesNormalized)) {
                                recommendations.add(ScoredResult(
                                    result,
                                    0.5f, // Base genre score
                                    true
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // Continue
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get tags: ${e.message}")
            }
        }
        
        Log.d(TAG, "Collected ${recommendations.size} raw recommendations")

        // Deduplicate and filter
        val existingSongIds = songs.map { it.id }.toSet()

        val filteredResults = recommendations
            .filter { it.result.id !in existingSongIds }
            .filter { isLikelyMusic(it.result) }
            .filter { isValidRecommendation(it.result, existingArtists, existingTitlesNormalized) }
            .distinctBy { it.result.id }
        
        // Ensure a mix of familiar and new artists
        val newArtistResults = filteredResults.filter { it.isNewArtist }.sortedByDescending { it.score }
        val familiarArtistResults = filteredResults.filter { !it.isNewArtist }.sortedByDescending { it.score }
        
        // Take at least 40% new artists, up to 60%
        val newArtistCount = (limit * 0.5).toInt().coerceIn(2, limit - 2)
        val familiarArtistCount = limit - newArtistCount
        
        val selected = mutableListOf<ScoredResult>()
        selected.addAll(newArtistResults.take(newArtistCount))
        selected.addAll(familiarArtistResults.take(familiarArtistCount))
        
        // Fill remaining slots if needed
        if (selected.size < limit) {
            val remaining = filteredResults
                .filter { it !in selected }
                .sortedByDescending { it.score }
                .take(limit - selected.size)
            selected.addAll(remaining)
        }
        
        selected
            .sortedByDescending { it.score }
            .take(limit)
            .map { it.result }
    }

    /**
     * Check if a result is valid (not an existing artist, not a similar title)
     */
    private fun isValidRecommendation(
        result: SearchResult,
        existingArtists: Set<String>,
        existingTitles: Set<String>
    ): Boolean {
        val artistLower = result.artist.lowercase()
        val titleNormalized = normalizeTitle(result.title)
        
        // Reject if same artist
        if (artistLower in existingArtists) return false
        
        // Reject if any existing artist name appears in result artist
        if (existingArtists.any { artistLower.contains(it) || it.contains(artistLower) }) return false
        
        // Reject if same title
        if (titleNormalized in existingTitles) return false
        
        // Reject covers, remixes, reactions
        val titleLower = result.title.lowercase()
        val rejectPatterns = listOf(
            "cover", "remix", "reaction", "reacts", "review",
            "karaoke", "instrumental", "tribute", "live session"
        )
        if (rejectPatterns.any { titleLower.contains(it) }) return false
        
        return true
    }
    
    /**
     * Analyze a playlist to extract a profile for recommendations
     */
    private fun analyzePlaylist(songs: List<Song>): PlaylistProfile {
        // Count artist occurrences
        val artistCounts = songs.groupingBy { it.artist.lowercase() }.eachCount()
        val topArtists = artistCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        // Extract keywords from titles
        val keywords = extractKeywords(songs)

        // Calculate average duration
        val avgDuration = songs.map { it.duration }.average().toLong()

        // Identify genres if available
        val genres = songs.mapNotNull { it.genre }.distinct()

        // Determine preferred sources
        val sourceCounts = songs.groupingBy { it.sourceType }.eachCount()
        val preferredSources = sourceCounts.entries
            .sortedByDescending { it.value }
            .map { it.key }

        return PlaylistProfile(
            topArtists = topArtists,
            keywords = keywords,
            avgDuration = avgDuration,
            genres = genres,
            preferredSources = preferredSources
        )
    }

    /**
     * Get genre/mood-based search terms instead of song title keywords
     * This prevents finding songs with the same name by different artists
     */
    private fun extractKeywords(songs: List<Song>): List<String> {
        // Use predefined genre/mood terms based on common music styles
        // These are broad enough to discover new music without matching specific song titles
        val genreTerms = listOf(
            "indie rock",
            "alternative",
            "dream pop",
            "shoegaze",
            "post punk",
            "synth pop",
            "art rock",
            "psychedelic",
            "lo-fi",
            "garage rock",
            "new wave",
            "darkwave",
            "electronic",
            "experimental"
        )
        
        // Return a shuffled subset for variety
        return genreTerms.shuffled().take(5)
    }

    /**
     * Normalize a title for comparison (remove common variations)
     */
    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("\\(.*\\)"), "") // Remove parentheses
            .replace(Regex("\\[.*]"), "")   // Remove brackets
            .replace(Regex("official|video|audio|lyrics|hd|hq"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Filter out non-music content like TV shows, podcasts, Shorts, etc.
     */
    private fun isLikelyMusic(result: SearchResult): Boolean {
        val titleLower = result.title.lowercase()
        val artistLower = result.artist.lowercase()
        val sourceUrl = result.sourceId.lowercase()
        
        // Exclude YouTube Shorts (they have /shorts/ in the URL or are very short)
        if (sourceUrl.contains("shorts") || sourceUrl.contains("/short/")) {
            return false
        }
        
        // Exclude TV show patterns
        val tvShowPatterns = listOf(
            Regex("s\\d+\\s*e\\d+", RegexOption.IGNORE_CASE),          // S01E01, S1E2, etc
            Regex("season\\s*\\d+\\s*episode\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("episode\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("ep\\.?\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("part\\s*\\d+\\s*of\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("#shorts", RegexOption.IGNORE_CASE),
            Regex("\\bshorts?\\b", RegexOption.IGNORE_CASE)  // "short" or "shorts" as word
        )
        
        if (tvShowPatterns.any { it.containsMatchIn(titleLower) }) {
            return false
        }
        
        // Exclude common non-music keywords
        val excludeKeywords = listOf(
            "podcast", "episode", "trailer", "teaser", "preview",
            "full movie", "full episode", "documentary", "interview",
            "reaction", "explained", "tutorial", "how to", "review",
            "unboxing", "gameplay", "walkthrough", "let's play",
            "compilation", "best of 20", "top 10", "top 20",
            "tv show", "series", "season finale", "premiere",
            "behind the scenes", "making of", "commentary",
            "audiobook", "chapter", "reading",
            "#shorts", "#short", "shorts", "tiktok", "viral",
            "meme", "funny", "comedy", "prank", "challenge",
            "asmr", "satisfying", "oddly satisfying",
            "news", "breaking", "update", "announcement",
            "stream highlights", "best moments", "clips",
            "vlog", "day in my life", "get ready with me", "grwm"
        )
        
        if (excludeKeywords.any { titleLower.contains(it) }) {
            return false
        }
        
        // Exclude if title looks like "Show Name - Episode Title" pattern without music indicators
        if (titleLower.contains(" - ") && 
            !titleLower.contains("music") && 
            !titleLower.contains("song") && 
            !titleLower.contains("audio") &&
            !titleLower.contains("official") &&
            (titleLower.contains("episode") || titleLower.contains("ep "))) {
            return false
        }
        
        // Duration check: songs are usually 1.5-10 minutes
        // Shorts are typically under 60 seconds
        // Very long content (>12 min) is usually not a song
        val durationMs = result.duration
        if (durationMs > 0) {
            val durationSec = durationMs / 1000
            // Exclude very short (under 90 seconds - likely Shorts) or very long (over 12 min)
            if (durationSec < 90 || durationSec > 720) {
                return false
            }
        }
        
        // Positive signals for music
        val musicIndicators = listOf(
            "official video", "official audio", "official music",
            "lyrics", "lyric video", "music video",
            "audio", "full song", "official", "vevo",
            "topic", "- topic", "records", "entertainment"
        )
        
        // If duration is unknown, require at least one music indicator
        if (durationMs <= 0 && !musicIndicators.any { titleLower.contains(it) || artistLower.contains(it) }) {
            return false
        }
        
        return true
    }

    private data class ScoredResult(
        val result: SearchResult,
        val score: Float,
        val isNewArtist: Boolean = false
    )

    private data class PlaylistProfile(
        val topArtists: List<String>,
        val keywords: List<String>,
        val avgDuration: Long,
        val genres: List<String>,
        val preferredSources: List<SourceType>
    )
}
