package com.quezic.domain.recommendation

import com.quezic.data.remote.MusicExtractorService
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.Song
import com.quezic.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart recommendation engine that analyzes playlist content
 * and suggests similar songs based on multiple factors.
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val extractorService: MusicExtractorService
) {

    /**
     * Get recommendations based on a list of songs (e.g., from a playlist)
     */
    suspend fun getRecommendations(
        songs: List<Song>,
        limit: Int = 10
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        if (songs.isEmpty()) return@withContext emptyList()

        // Analyze the playlist to extract features
        val profile = analyzePlaylist(songs)
        
        // Get recommendations from multiple strategies
        val recommendations = mutableListOf<ScoredResult>()

        // Strategy 1: Search for similar artists
        profile.topArtists.take(3).forEach { artist ->
            try {
                val results = extractorService.searchByArtist(
                    artist,
                    listOf(SourceType.YOUTUBE, SourceType.SOUNDCLOUD)
                )
                results.forEach { result ->
                    recommendations.add(ScoredResult(result, calculateArtistScore(result, profile)))
                }
            } catch (e: Exception) {
                // Continue with other strategies
            }
        }

        // Strategy 2: Search for genre/mood keywords
        profile.keywords.take(3).forEach { keyword ->
            try {
                val results = extractorService.search(
                    keyword,
                    listOf(SourceType.YOUTUBE, SourceType.SOUNDCLOUD)
                )
                results.forEach { result ->
                    recommendations.add(ScoredResult(result, calculateKeywordScore(result, profile)))
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // Strategy 3: Get related songs from sources
        songs.take(3).forEach { song ->
            try {
                val related = extractorService.getRelatedSongs(
                    song.sourceType,
                    song.sourceId,
                    5
                )
                related.forEach { result ->
                    recommendations.add(ScoredResult(result, calculateRelatedScore(result, profile)))
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // Deduplicate and filter out songs already in the playlist
        val existingSongIds = songs.map { it.id }.toSet()
        val existingTitles = songs.map { normalizeTitle(it.title) }.toSet()

        recommendations
            .filter { it.result.id !in existingSongIds }
            .filter { normalizeTitle(it.result.title) !in existingTitles }
            .distinctBy { it.result.id }
            .sortedByDescending { it.score }
            .take(limit)
            .map { it.result }
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
     * Extract meaningful keywords from song titles
     */
    private fun extractKeywords(songs: List<Song>): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "up", "about", "into", "through", "during",
            "official", "video", "audio", "lyrics", "hd", "hq", "music", "song",
            "feat", "ft", "remix", "mix", "version", "edit", "extended", "original"
        )

        val wordCounts = mutableMapOf<String, Int>()
        
        songs.forEach { song ->
            val words = song.title
                .lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 2 && it !in stopWords }

            words.forEach { word ->
                wordCounts[word] = (wordCounts[word] ?: 0) + 1
            }

            // Add artist as keyword
            val artistWords = song.artist
                .lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.length > 2 && it !in stopWords }

            artistWords.forEach { word ->
                wordCounts[word] = (wordCounts[word] ?: 0) + 2 // Weight artist higher
            }
        }

        return wordCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }

    /**
     * Score a result based on artist similarity
     */
    private fun calculateArtistScore(result: SearchResult, profile: PlaylistProfile): Float {
        var score = 0.5f // Base score

        // Boost if artist matches
        if (result.artist.lowercase() in profile.topArtists) {
            score += 0.3f
        }

        // Boost if keywords match
        val titleWords = result.title.lowercase().split(Regex("\\s+"))
        val matchingKeywords = titleWords.count { it in profile.keywords }
        score += (matchingKeywords * 0.05f).coerceAtMost(0.2f)

        // Duration similarity
        val durationDiff = kotlin.math.abs(result.duration - profile.avgDuration)
        if (durationDiff < 60000) { // Within 1 minute
            score += 0.1f
        }

        return score.coerceIn(0f, 1f)
    }

    /**
     * Score a result based on keyword match
     */
    private fun calculateKeywordScore(result: SearchResult, profile: PlaylistProfile): Float {
        var score = 0.3f // Lower base score for keyword searches

        // Check title for keyword matches
        val titleWords = result.title.lowercase().split(Regex("\\s+"))
        val matchingKeywords = titleWords.count { it in profile.keywords }
        score += (matchingKeywords * 0.1f).coerceAtMost(0.4f)

        // Boost if artist matches
        if (result.artist.lowercase() in profile.topArtists) {
            score += 0.2f
        }

        return score.coerceIn(0f, 1f)
    }

    /**
     * Score a related song result
     */
    private fun calculateRelatedScore(result: SearchResult, profile: PlaylistProfile): Float {
        var score = 0.6f // Higher base score for related songs

        // Boost if artist matches
        if (result.artist.lowercase() in profile.topArtists) {
            score += 0.2f
        }

        // Check for keyword matches
        val titleWords = result.title.lowercase().split(Regex("\\s+"))
        val matchingKeywords = titleWords.count { it in profile.keywords }
        score += (matchingKeywords * 0.05f).coerceAtMost(0.15f)

        return score.coerceIn(0f, 1f)
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

    private data class ScoredResult(
        val result: SearchResult,
        val score: Float
    )

    private data class PlaylistProfile(
        val topArtists: List<String>,
        val keywords: List<String>,
        val avgDuration: Long,
        val genres: List<String>,
        val preferredSources: List<SourceType>
    )
}
