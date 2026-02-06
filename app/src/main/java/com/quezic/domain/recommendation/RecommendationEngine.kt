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
     * Balances familiar artists with discovery of new artists
     */
    suspend fun getRecommendations(
        songs: List<Song>,
        limit: Int = 10,
        forceRefresh: Boolean = false
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        if (songs.isEmpty()) return@withContext emptyList()

        // Analyze the playlist to extract features
        val profile = analyzePlaylist(songs)
        val existingArtists = songs.map { it.artist.lowercase() }.toSet()
        
        // Get recommendations from multiple strategies
        val recommendations = mutableListOf<ScoredResult>()

        // Strategy 1: Get related songs (best for discovering new artists)
        // Take random songs if refreshing to get different recommendations
        val seedSongs = if (forceRefresh) {
            songs.shuffled().take(3)
        } else {
            songs.take(3)
        }
        
        seedSongs.forEach { song ->
            try {
                val related = extractorService.getRelatedSongs(
                    song.sourceType,
                    song.sourceId,
                    8
                )
                related.forEach { result ->
                    val isNewArtist = result.artist.lowercase() !in existingArtists
                    recommendations.add(ScoredResult(
                        result, 
                        calculateRelatedScore(result, profile, isNewArtist),
                        isNewArtist
                    ))
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // Strategy 2: Search for genre/mood keywords (good for variety)
        // Add "music" to help filter out non-music content
        val keywordsToSearch = if (forceRefresh) {
            profile.keywords.shuffled().take(2)
        } else {
            profile.keywords.take(2)
        }
        
        keywordsToSearch.forEach { keyword ->
            try {
                // Add "music" or "song" to search queries to filter out podcasts/shows
                val searchQuery = "$keyword music"
                val results = extractorService.search(
                    searchQuery,
                    listOf(SourceType.YOUTUBE, SourceType.SOUNDCLOUD)
                )
                results.take(5).forEach { result ->
                    val isNewArtist = result.artist.lowercase() !in existingArtists
                    recommendations.add(ScoredResult(
                        result, 
                        calculateKeywordScore(result, profile, isNewArtist),
                        isNewArtist
                    ))
                }
            } catch (e: Exception) {
                // Continue
            }
        }

        // Strategy 3: Search for similar artists (limit to ensure variety)
        // Only search 1 artist to avoid too many same-artist suggestions
        profile.topArtists.take(1).forEach { artist ->
            try {
                val results = extractorService.searchByArtist(
                    artist,
                    listOf(SourceType.YOUTUBE, SourceType.SOUNDCLOUD)
                )
                results.take(3).forEach { result ->
                    recommendations.add(ScoredResult(result, calculateArtistScore(result, profile), false))
                }
            } catch (e: Exception) {
                // Continue with other strategies
            }
        }

        // Deduplicate and filter out songs already in the playlist
        val existingSongIds = songs.map { it.id }.toSet()
        val existingTitles = songs.map { normalizeTitle(it.title) }.toSet()

        val filteredResults = recommendations
            .filter { it.result.id !in existingSongIds }
            .filter { normalizeTitle(it.result.title) !in existingTitles }
            .filter { isLikelyMusic(it.result) } // Filter out non-music content
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
     * New artists get a discovery bonus
     */
    private fun calculateKeywordScore(result: SearchResult, profile: PlaylistProfile, isNewArtist: Boolean): Float {
        var score = 0.4f // Base score for keyword searches

        // Check title for keyword matches
        val titleWords = result.title.lowercase().split(Regex("\\s+"))
        val matchingKeywords = titleWords.count { it in profile.keywords }
        score += (matchingKeywords * 0.1f).coerceAtMost(0.3f)

        // Discovery bonus for new artists (instead of penalizing them)
        if (isNewArtist) {
            score += 0.15f // Bonus for discovering new artists
        } else if (result.artist.lowercase() in profile.topArtists) {
            score += 0.1f // Smaller boost for familiar artists
        }

        return score.coerceIn(0f, 1f)
    }

    /**
     * Score a related song result
     * New artists get a discovery bonus to encourage variety
     */
    private fun calculateRelatedScore(result: SearchResult, profile: PlaylistProfile, isNewArtist: Boolean): Float {
        var score = 0.55f // Base score for related songs

        // Discovery bonus for new artists
        if (isNewArtist) {
            score += 0.25f // Strong bonus for discovering new artists via related songs
        } else if (result.artist.lowercase() in profile.topArtists) {
            score += 0.1f // Smaller boost for same artist
        }

        // Check for keyword matches (mood/genre similarity)
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
    
    /**
     * Filter out non-music content like TV shows, podcasts, episodes, etc.
     */
    private fun isLikelyMusic(result: SearchResult): Boolean {
        val titleLower = result.title.lowercase()
        val artistLower = result.artist.lowercase()
        
        // Exclude TV show patterns
        val tvShowPatterns = listOf(
            Regex("s\\d+\\s*e\\d+", RegexOption.IGNORE_CASE),          // S01E01, S1E2, etc
            Regex("season\\s*\\d+\\s*episode\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("episode\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("ep\\.?\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("part\\s*\\d+\\s*of\\s*\\d+", RegexOption.IGNORE_CASE)
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
            "audiobook", "chapter", "reading"
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
        
        // Duration check: songs are usually 1-10 minutes
        // Exclude very long content (>15 min) or very short (<30 sec)
        val durationMs = result.duration
        if (durationMs > 0) {
            val durationMin = durationMs / 60000
            if (durationMin > 15 || durationMin < 0.5) {
                return false
            }
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
