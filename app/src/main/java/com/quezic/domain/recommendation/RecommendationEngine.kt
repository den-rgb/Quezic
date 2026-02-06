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
        val keywordsToSearch = if (forceRefresh) {
            profile.keywords.shuffled().take(3)
        } else {
            profile.keywords.take(3)
        }
        
        keywordsToSearch.forEach { genre ->
            try {
                // Search for genre-based discoveries with "official" to get real songs
                val query = "$genre new music official audio"
                
                val results = extractorService.search(
                    query,
                    listOf(SourceType.YOUTUBE, SourceType.SOUNDCLOUD)
                )
                results.take(8).forEach { result ->
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

        // Strategy 3: Search for artists SIMILAR TO playlist artists (to discover new artists)
        val artistsToSearch = if (forceRefresh) {
            profile.topArtists.shuffled().take(2)
        } else {
            profile.topArtists.take(2)
        }
        
        artistsToSearch.forEach { artist ->
            try {
                val results = extractorService.searchSimilarArtists(
                    artist,
                    listOf(SourceType.YOUTUBE, SourceType.SOUNDCLOUD)
                )
                results.forEach { result ->
                    val isNewArtist = result.artist.lowercase() !in existingArtists
                    // Only add if it's actually a new artist
                    if (isNewArtist) {
                        recommendations.add(ScoredResult(
                            result, 
                            calculateArtistScore(result, profile) + 0.2f, // Bonus for discovery
                            true
                        ))
                    }
                }
            } catch (e: Exception) {
                // Continue with other strategies
            }
        }
        
        // Strategy 4: Curated playlist discovery (to find songs from editorial playlists)
        val playlistSearches = listOf(
            "best new indie songs 2024 official",
            "underground music discoveries official audio",
            "hidden gem songs official video",
            "indie music blog picks official",
            "new artists to watch official audio"
        )
        try {
            val playlistQuery = if (forceRefresh) playlistSearches.random() else playlistSearches.first()
            val results = extractorService.search(
                playlistQuery,
                listOf(SourceType.YOUTUBE, SourceType.SOUNDCLOUD)
            )
            results.take(8).forEach { result ->
                val isNewArtist = result.artist.lowercase() !in existingArtists
                if (isNewArtist) {
                    recommendations.add(ScoredResult(
                        result,
                        0.5f, // Base discovery score
                        true
                    ))
                }
            }
        } catch (e: Exception) {
            // Continue
        }

        // Deduplicate and filter out songs already in the playlist
        val existingSongIds = songs.map { it.id }.toSet()
        val existingTitles = songs.map { normalizeTitle(it.title) }.toSet()
        
        // Get existing artist names for filtering (to avoid songs mentioning these artists)
        val existingArtistNames = songs.map { it.artist.lowercase() }.toSet()
        
        // Get key words from existing song titles to avoid finding "same song, different artist"
        val existingTitleWords = songs.flatMap { song ->
            normalizeTitle(song.title)
                .split(Regex("\\s+"))
                .filter { it.length > 3 }
        }.toSet()

        val filteredResults = recommendations
            .filter { it.result.id !in existingSongIds }
            .filter { normalizeTitle(it.result.title) !in existingTitles }
            .filter { isLikelyMusic(it.result) } // Filter out non-music content
            .filter { isByNewArtist(it.result, existingArtistNames) } // Filter songs mentioning existing artists
            .filter { !hasSimilarTitle(it.result, existingTitleWords) } // Filter songs with same/similar titles
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
     * Check if a result has a very similar title to existing songs
     * Prevents "Love by Artist A" when you already have "Love by Artist B"
     */
    private fun hasSimilarTitle(result: SearchResult, existingTitleWords: Set<String>): Boolean {
        val normalizedTitle = normalizeTitle(result.title)
        val resultWords = normalizedTitle.split(Regex("\\s+")).filter { it.length > 3 }
        
        // If the title is very short (1-2 significant words), check for exact match
        if (resultWords.size <= 2) {
            return resultWords.any { it in existingTitleWords }
        }
        
        // For longer titles, check if majority of words match existing titles
        val matchingWords = resultWords.count { it in existingTitleWords }
        val matchRatio = matchingWords.toFloat() / resultWords.size
        
        // If more than 50% of words match, it's probably the same song
        return matchRatio > 0.5f
    }
    
    /**
     * Check if a result is by a genuinely new artist (not mentioning existing artists)
     * Filters out covers, remixes, and songs that mention existing artist names in the title
     */
    private fun isByNewArtist(result: SearchResult, existingArtistNames: Set<String>): Boolean {
        val titleLower = result.title.lowercase()
        val artistLower = result.artist.lowercase()
        
        // Check if the artist is one we already have
        for (existingArtist in existingArtistNames) {
            // Skip very short artist names to avoid false positives
            if (existingArtist.length < 3) continue
            
            // If the song is BY an existing artist, filter it out
            if (artistLower.contains(existingArtist) || existingArtist.contains(artistLower)) {
                return false
            }
            
            // If the title mentions an existing artist (covers, remixes, reactions, etc.)
            if (titleLower.contains(existingArtist)) {
                return false
            }
            
            // Check individual words for longer artist names
            val artistWords = existingArtist.split(Regex("\\s+")).filter { it.length > 3 }
            // If all significant words of the artist appear in the title, it's probably about that artist
            if (artistWords.size >= 2 && artistWords.all { titleLower.contains(it) }) {
                return false
            }
        }
        
        // Filter out common non-original content patterns
        val coverPatterns = listOf(
            "cover", "remix", "reaction", "reacts", "review",
            "type beat", "instrumental", "karaoke", "tribute",
            "in the style of", "sounds like", "inspired by"
        )
        
        if (coverPatterns.any { titleLower.contains(it) }) {
            return false
        }
        
        return true
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
