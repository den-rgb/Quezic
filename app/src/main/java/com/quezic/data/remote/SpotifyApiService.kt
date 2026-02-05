package com.quezic.data.remote

import android.util.Log
import com.quezic.domain.model.SpotifyPlaylist
import com.quezic.domain.model.SpotifyTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for fetching Spotify playlist data from share URLs.
 * Uses multiple approaches to get public playlist information without OAuth.
 */
@Singleton
class SpotifyApiService @Inject constructor() {

    companion object {
        private const val TAG = "SpotifyApiService"
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 30L
        
        // Regex patterns for extracting playlist ID from various Spotify URL formats
        private val PLAYLIST_PATTERNS = listOf(
            Pattern.compile("spotify\\.com/playlist/([a-zA-Z0-9]+)"),
            Pattern.compile("spotify:playlist:([a-zA-Z0-9]+)"),
            Pattern.compile("open\\.spotify\\.com/playlist/([a-zA-Z0-9]+)")
        )
        
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Validates if a URL is a valid Spotify playlist URL.
     */
    fun isValidSpotifyPlaylistUrl(url: String): Boolean {
        return extractPlaylistId(url) != null
    }

    /**
     * Extracts the playlist ID from a Spotify URL.
     */
    fun extractPlaylistId(url: String): String? {
        for (pattern in PLAYLIST_PATTERNS) {
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    /**
     * Fetches playlist data from a Spotify share URL.
     */
    suspend fun fetchPlaylist(spotifyUrl: String): Result<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = extractPlaylistId(spotifyUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid Spotify playlist URL"))

            Log.d(TAG, "Fetching playlist: $playlistId")
            
            // Try the embed page - this is most reliable for public playlists
            val embedResult = fetchFromEmbed(playlistId)
            if (embedResult.isSuccess && embedResult.getOrNull()?.tracks?.isNotEmpty() == true) {
                Log.d(TAG, "Successfully fetched via embed page")
                return@withContext embedResult
            }
            
            Log.e(TAG, "Embed fetch failed or returned no tracks")
            Result.failure(Exception("Could not fetch playlist data. Make sure the playlist is public."))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching playlist: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches playlist data from Spotify's embed endpoint.
     */
    private suspend fun fetchFromEmbed(playlistId: String): Result<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        try {
            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Embed request failed: HTTP ${resp.code}")
                    return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                }

                val html = resp.body?.string() 
                    ?: return@withContext Result.failure(Exception("Empty response"))

                Log.d(TAG, "Got embed HTML, length: ${html.length}")

                val playlist = parseEmbedHtml(html, playlistId)
                if (playlist != null && playlist.tracks.isNotEmpty()) {
                    Log.d(TAG, "Successfully parsed embed: ${playlist.name} with ${playlist.tracks.size} tracks")
                    Result.success(playlist)
                } else {
                    Log.e(TAG, "Failed to parse tracks from embed HTML")
                    Result.failure(Exception("Could not parse embed data"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Embed fetch failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parses the embed HTML to extract playlist data.
     */
    private fun parseEmbedHtml(html: String, playlistId: String): SpotifyPlaylist? {
        try {
            // Method 1: Look for __NEXT_DATA__ script
            val nextDataPattern = Pattern.compile(
                "<script id=\"__NEXT_DATA__\"[^>]*>([\\s\\S]*?)</script>",
                Pattern.CASE_INSENSITIVE
            )
            val nextDataMatcher = nextDataPattern.matcher(html)
            
            if (nextDataMatcher.find()) {
                val jsonStr = nextDataMatcher.group(1)?.trim()
                if (!jsonStr.isNullOrBlank()) {
                    Log.d(TAG, "Found __NEXT_DATA__, parsing...")
                    try {
                        val json = JSONObject(jsonStr)
                        val playlist = parseNextDataJson(json, playlistId)
                        if (playlist != null && playlist.tracks.isNotEmpty()) {
                            return playlist
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse __NEXT_DATA__: ${e.message}")
                    }
                }
            }

            // Method 2: Look for resource script with balanced brace matching
            val scriptPattern = Pattern.compile("<script[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE)
            val scriptMatcher = scriptPattern.matcher(html)
            
            while (scriptMatcher.find()) {
                val scriptContent = scriptMatcher.group(1) ?: continue
                
                // Look for playlist data patterns
                if (scriptContent.contains("\"type\":\"playlist\"") || 
                    scriptContent.contains("\"trackList\"") ||
                    scriptContent.contains("\"tracks\"")) {
                    
                    Log.d(TAG, "Found potential playlist script")
                    
                    // Try to extract JSON objects
                    val playlist = extractPlaylistFromScript(scriptContent, playlistId)
                    if (playlist != null && playlist.tracks.isNotEmpty()) {
                        return playlist
                    }
                }
            }

            // Method 3: Look for inline data in specific patterns
            val inlinePatterns = listOf(
                "window\\.__PRELOADED_STATE__\\s*=\\s*",
                "Spotify\\.Entity\\s*=\\s*",
                "__SSR_DATA__\\s*=\\s*"
            )
            
            for (patternStr in inlinePatterns) {
                val pattern = Pattern.compile(patternStr + "(.+?)(?:;|</script>)", Pattern.DOTALL)
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    val jsonStr = matcher.group(1)?.trim()
                    if (!jsonStr.isNullOrBlank()) {
                        try {
                            val json = JSONObject(jsonStr)
                            val playlist = parseEntityJson(json, playlistId)
                            if (playlist != null && playlist.tracks.isNotEmpty()) {
                                return playlist
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Pattern $patternStr parse failed: ${e.message}")
                        }
                    }
                }
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing embed HTML: ${e.message}", e)
            return null
        }
    }

    /**
     * Extracts playlist data from a script content.
     */
    private fun extractPlaylistFromScript(scriptContent: String, playlistId: String): SpotifyPlaylist? {
        // Find JSON object boundaries using balanced brace matching
        val startIdx = scriptContent.indexOf("{")
        if (startIdx < 0) return null
        
        var braceCount = 0
        var endIdx = -1
        
        for (i in startIdx until scriptContent.length) {
            when (scriptContent[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        endIdx = i + 1
                        break
                    }
                }
            }
        }
        
        if (endIdx <= startIdx) return null
        
        val jsonStr = scriptContent.substring(startIdx, endIdx)
        
        try {
            val json = JSONObject(jsonStr)
            
            // Try to find entity/playlist data
            val playlist = parseNextDataJson(json, playlistId) 
                ?: parseEntityJson(json, playlistId)
            
            if (playlist != null && playlist.tracks.isNotEmpty()) {
                return playlist
            }
        } catch (e: Exception) {
            Log.d(TAG, "JSON parse failed: ${e.message}")
        }
        
        return null
    }

    /**
     * Parses __NEXT_DATA__ JSON structure.
     */
    private fun parseNextDataJson(json: JSONObject, playlistId: String): SpotifyPlaylist? {
        // Navigate through possible paths to find the entity
        val paths = listOf(
            listOf("props", "pageProps", "state", "data", "entity"),
            listOf("props", "pageProps", "entity"),
            listOf("props", "pageProps", "state", "data"),
            listOf("props", "pageProps")
        )
        
        for (path in paths) {
            var current: JSONObject? = json
            for (key in path) {
                current = current?.optJSONObject(key)
                if (current == null) break
            }
            
            if (current != null) {
                val playlist = parseEntityJson(current, playlistId)
                if (playlist != null && playlist.tracks.isNotEmpty()) {
                    return playlist
                }
            }
        }
        
        return null
    }

    /**
     * Parses the entity JSON object to extract playlist data.
     */
    private fun parseEntityJson(entity: JSONObject, playlistId: String): SpotifyPlaylist? {
        try {
            val name = entity.optString("name", "")
                .ifBlank { entity.optString("title", "Imported Playlist") }
            val description = entity.optString("description", null)
            
            // Get cover image - try multiple paths
            val coverUrl = getCoverUrl(entity)
            
            // Get owner
            val ownerName = getOwnerName(entity)
            
            // Get tracks
            val tracks = extractTracks(entity)
            
            Log.d(TAG, "Parsed entity: name=$name, tracks=${tracks.size}")
            
            return SpotifyPlaylist(
                id = playlistId,
                name = name,
                description = description?.takeIf { it.isNotBlank() },
                coverUrl = coverUrl,
                ownerName = ownerName,
                tracks = tracks
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing entity JSON: ${e.message}", e)
            return null
        }
    }

    /**
     * Gets cover URL from entity JSON.
     */
    private fun getCoverUrl(entity: JSONObject): String? {
        // Try standard images array
        entity.optJSONArray("images")?.let { images ->
            if (images.length() > 0) {
                return images.optJSONObject(0)?.optString("url")
            }
        }
        
        // Try coverArt.sources
        entity.optJSONObject("coverArt")?.optJSONArray("sources")?.let { sources ->
            if (sources.length() > 0) {
                return sources.optJSONObject(0)?.optString("url")
            }
        }
        
        // Try image.url
        return entity.optJSONObject("image")?.optString("url")
    }

    /**
     * Gets owner name from entity JSON.
     */
    private fun getOwnerName(entity: JSONObject): String? {
        entity.optJSONObject("owner")?.let { owner ->
            return owner.optString("name", null)
                ?: owner.optString("display_name", null)
        }
        
        entity.optJSONObject("ownerV2")?.optJSONObject("data")?.let { data ->
            return data.optString("name", null)
        }
        
        // Try subtitle field
        return entity.optString("subtitle", null)
    }

    /**
     * Extracts tracks from entity JSON.
     */
    private fun extractTracks(entity: JSONObject): List<SpotifyTrack> {
        val tracks = mutableListOf<SpotifyTrack>()
        
        // Try different paths for track items
        val trackItems = findTrackItems(entity)
        
        if (trackItems != null) {
            Log.d(TAG, "Found ${trackItems.length()} track items")
            
            for (i in 0 until trackItems.length()) {
                val item = trackItems.optJSONObject(i) ?: continue
                val track = extractTrack(item)
                if (track != null) {
                    tracks.add(track)
                }
            }
        }
        
        return tracks
    }

    /**
     * Finds track items array from entity JSON.
     */
    private fun findTrackItems(entity: JSONObject): JSONArray? {
        // Direct trackList array
        entity.optJSONArray("trackList")?.let { 
            if (it.length() > 0) return it 
        }
        
        // tracks.items
        entity.optJSONObject("tracks")?.optJSONArray("items")?.let { 
            if (it.length() > 0) return it 
        }
        
        // content.items
        entity.optJSONObject("content")?.optJSONArray("items")?.let { 
            if (it.length() > 0) return it 
        }
        
        return null
    }

    /**
     * Extracts a single track from a track item JSON.
     */
    private fun extractTrack(item: JSONObject): SpotifyTrack? {
        try {
            // The track data might be nested under "track" or "itemV2.data"
            val track = item.optJSONObject("track")
                ?: item.optJSONObject("itemV2")?.optJSONObject("data")
                ?: item
            
            // Get track name
            val trackName = track.optString("name", "")
                .ifBlank { track.optString("title", "") }
            
            if (trackName.isBlank()) {
                return null
            }
            
            // Get duration
            val durationMs = track.optLong("duration_ms", 0L)
                .takeIf { it > 0 }
                ?: track.optLong("duration", 0L)
                .takeIf { it > 0 }
                ?: track.optJSONObject("duration")?.optLong("totalMilliseconds", 0L)
                ?: 0L
            
            // Get artist name
            val artistName = extractArtistName(track)
            
            // Get album name
            val albumName = track.optJSONObject("album")?.optString("name")
                ?: track.optJSONObject("albumOfTrack")?.optJSONObject("data")?.optString("name")
            
            return SpotifyTrack(
                name = trackName,
                artist = artistName,
                album = albumName,
                durationMs = durationMs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting track: ${e.message}")
            return null
        }
    }

    /**
     * Extracts artist name from track JSON.
     */
    private fun extractArtistName(track: JSONObject): String {
        // Try standard artists array
        track.optJSONArray("artists")?.let { artists ->
            if (artists.length() > 0) {
                val names = mutableListOf<String>()
                for (i in 0 until artists.length()) {
                    val name = artists.optJSONObject(i)?.optString("name")
                    if (!name.isNullOrBlank()) names.add(name)
                }
                if (names.isNotEmpty()) return names.joinToString(", ")
            }
        }
        
        // Try artists.items (newer format)
        track.optJSONObject("artists")?.optJSONArray("items")?.let { items ->
            if (items.length() > 0) {
                val names = mutableListOf<String>()
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i)
                    val name = item?.optJSONObject("profile")?.optString("name")
                        ?: item?.optString("name")
                    if (!name.isNullOrBlank()) names.add(name)
                }
                if (names.isNotEmpty()) return names.joinToString(", ")
            }
        }
        
        // Try firstArtist
        track.optJSONObject("firstArtist")?.optJSONArray("items")?.let { items ->
            if (items.length() > 0) {
                val name = items.optJSONObject(0)?.optJSONObject("profile")?.optString("name")
                if (!name.isNullOrBlank()) return name
            }
        }
        
        // Try subtitle (often contains artist name in embed)
        track.optString("subtitle", null)?.let { subtitle ->
            if (subtitle.isNotBlank()) return subtitle
        }
        
        return "Unknown Artist"
    }
}
