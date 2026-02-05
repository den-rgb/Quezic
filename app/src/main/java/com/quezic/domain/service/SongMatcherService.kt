package com.quezic.domain.service

import android.util.Log
import com.quezic.data.remote.MusicExtractorService
import com.quezic.domain.model.MatchResult
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.SourceType
import com.quezic.domain.model.SpotifyTrack
import com.quezic.domain.model.TrackMatchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Service for matching Spotify tracks to YouTube/SoundCloud equivalents.
 * Uses intelligent search and scoring to find the best matches.
 */
@Singleton
class SongMatcherService @Inject constructor(
    private val musicExtractorService: MusicExtractorService
) {
    companion object {
        private const val TAG = "SongMatcherService"
        
        // Matching thresholds
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
        private const val MEDIUM_CONFIDENCE_THRESHOLD = 0.5f
        
        // Duration tolerance (15 seconds)
        private const val DURATION_TOLERANCE_MS = 15000L
        
        // Rate limiting delay between searches (ms)
        private const val SEARCH_DELAY_MS = 300L
        
        // Preferred sources in order (YouTube first - more reliable, no Go+ paywall)
        private val PREFERRED_SOURCES = listOf(SourceType.YOUTUBE, SourceType.SOUNDCLOUD)
    }

    /**
     * Finds the best match for a Spotify track.
     * Searches both SoundCloud and YouTube and returns the best result.
     */
    suspend fun findMatch(spotifyTrack: SpotifyTrack): MatchResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Finding match for: ${spotifyTrack.artist} - ${spotifyTrack.name}")
        
        try {
            val searchQuery = spotifyTrack.toSearchQuery()
            
            // Search on all sources
            val searchResults = musicExtractorService.search(searchQuery, PREFERRED_SOURCES)
            
            if (searchResults.isEmpty()) {
                Log.d(TAG, "No results found for: $searchQuery")
                return@withContext MatchResult.NotFound
            }
            
            // Score and sort results
            val scoredResults = searchResults.map { result ->
                Pair(result, calculateMatchScore(spotifyTrack, result))
            }.sortedByDescending { it.second }
            
            val topResult = scoredResults.first()
            val topScore = topResult.second
            
            Log.d(TAG, "Best match: ${topResult.first.title} with score $topScore")
            
            when {
                topScore >= HIGH_CONFIDENCE_THRESHOLD -> {
                    MatchResult.Matched(topResult.first, topScore)
                }
                topScore >= MEDIUM_CONFIDENCE_THRESHOLD -> {
                    // Medium confidence - return top 3 options for user to choose
                    val topOptions = scoredResults.take(3).map { it.first }
                    if (topOptions.size == 1) {
                        MatchResult.Matched(topResult.first, topScore)
                    } else {
                        MatchResult.MultipleOptions(topOptions)
                    }
                }
                else -> {
                    // Low confidence - still return options but mark as multiple
                    val topOptions = scoredResults.take(3).map { it.first }
                    MatchResult.MultipleOptions(topOptions)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding match: ${e.message}", e)
            MatchResult.NotFound
        }
    }

    /**
     * Processes a list of Spotify tracks and finds matches for each.
     * Includes rate limiting to avoid API throttling.
     * 
     * @param tracks List of Spotify tracks to match
     * @param onProgress Callback with current progress (0.0 to 1.0)
     * @param onTrackMatched Callback when a track is matched
     * @return List of match states for all tracks
     */
    suspend fun matchTracks(
        tracks: List<SpotifyTrack>,
        onProgress: (Float) -> Unit = {},
        onTrackMatched: (Int, TrackMatchState) -> Unit = { _, _ -> }
    ): List<TrackMatchState> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TrackMatchState>()
        
        tracks.forEachIndexed { index, track ->
            val matchResult = findMatch(track)
            
            val state = TrackMatchState(
                spotifyTrack = track,
                result = matchResult,
                isProcessing = false,
                selectedResult = (matchResult as? MatchResult.Matched)?.searchResult
            )
            
            results.add(state)
            
            val progress = (index + 1).toFloat() / tracks.size
            onProgress(progress)
            onTrackMatched(index, state)
            
            // Rate limiting delay
            if (index < tracks.size - 1) {
                delay(SEARCH_DELAY_MS)
            }
        }
        
        results
    }

    /**
     * Calculates a match score between a Spotify track and a search result.
     * Score ranges from 0.0 (no match) to 1.0 (perfect match).
     */
    private fun calculateMatchScore(spotifyTrack: SpotifyTrack, result: SearchResult): Float {
        var score = 0f
        
        // Title similarity (40% weight)
        val titleSimilarity = calculateStringSimilarity(
            spotifyTrack.name.lowercase(Locale.ROOT),
            cleanTitle(result.title).lowercase(Locale.ROOT)
        )
        score += titleSimilarity * 0.4f
        
        // Artist similarity (40% weight)
        val artistSimilarity = calculateStringSimilarity(
            spotifyTrack.artist.lowercase(Locale.ROOT),
            cleanArtist(result.artist).lowercase(Locale.ROOT)
        )
        score += artistSimilarity * 0.4f
        
        // Duration similarity (20% weight)
        val durationScore = calculateDurationScore(spotifyTrack.durationMs, result.duration)
        score += durationScore * 0.2f
        
        // Small bonus for YouTube (more reliable, no paywall issues)
        if (result.sourceType == SourceType.YOUTUBE) {
            score = minOf(1f, score + 0.03f)
        }
        
        return score
    }

    /**
     * Calculates string similarity using a combination of methods.
     */
    private fun calculateStringSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1f
        if (s1.isBlank() || s2.isBlank()) return 0f
        
        // Check for containment
        if (s1.contains(s2) || s2.contains(s1)) {
            val shorter = minOf(s1.length, s2.length)
            val longer = maxOf(s1.length, s2.length)
            return shorter.toFloat() / longer.toFloat()
        }
        
        // Jaccard similarity on words
        val words1 = s1.split(Regex("\\s+")).filter { it.length > 1 }.toSet()
        val words2 = s2.split(Regex("\\s+")).filter { it.length > 1 }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return levenshteinSimilarity(s1, s2)
        }
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union > 0) {
            intersection.toFloat() / union.toFloat()
        } else {
            0f
        }
    }

    /**
     * Calculates Levenshtein distance based similarity.
     */
    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1f
        
        val distance = levenshteinDistance(s1, s2)
        return 1f - (distance.toFloat() / maxLen.toFloat())
    }

    /**
     * Calculates Levenshtein distance between two strings.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        
        if (m == 0) return n
        if (n == 0) return m
        
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[m][n]
    }

    /**
     * Calculates duration similarity score.
     */
    private fun calculateDurationScore(spotifyDuration: Long, resultDuration: Long): Float {
        val diff = abs(spotifyDuration - resultDuration)
        
        return when {
            diff <= 3000 -> 1f // Within 3 seconds - perfect
            diff <= DURATION_TOLERANCE_MS -> 0.8f // Within tolerance
            diff <= DURATION_TOLERANCE_MS * 2 -> 0.5f // Double tolerance
            diff <= DURATION_TOLERANCE_MS * 4 -> 0.2f // Quadruple tolerance
            else -> 0f
        }
    }

    /**
     * Cleans up a title string by removing common suffixes and formatting.
     */
    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("\\s*\\(Official.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[Official.*?]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Lyrics.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[Lyrics.*?]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Audio.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[Audio.*?]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(HD\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(HQ\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-\\s*Topic$", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /**
     * Cleans up an artist name string.
     */
    private fun cleanArtist(artist: String): String {
        return artist
            .replace(Regex("\\s*-\\s*Topic$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^VEVO$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("VEVO$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*Official$", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
