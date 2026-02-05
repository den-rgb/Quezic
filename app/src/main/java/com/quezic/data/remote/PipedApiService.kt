package com.quezic.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback service using Piped API for YouTube audio streams.
 * Piped is a privacy-focused YouTube frontend with public API instances.
 */
@Singleton
class PipedApiService @Inject constructor() {

    companion object {
        private const val TAG = "PipedApi"
        
        // List of public Piped API instances (updated Feb 2026)
        // Source: https://github.com/TeamPiped/Piped/wiki/Instances
        // Priority: CDN-backed instances first
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",        // Official - multi-region CDN
            "https://pipedapi.leptons.xyz",        // 🇦🇹 Austria - CDN
            "https://pipedapi.nosebs.ru",          // 🇫🇮 Finland - CDN
            "https://pipedapi-libre.kavin.rocks",  // 🇳🇱 Netherlands - libre
            "https://piped-api.privacy.com.de",    // 🇩🇪 Germany
            "https://pipedapi.reallyaweso.me",     // 🇩🇪 Germany
            "https://api.piped.private.coffee",    // 🇦🇹 Austria
            "https://piped-api.codespace.cz"       // 🇨🇿 Czech Republic
        )
        
        private const val CONNECT_TIMEOUT = 15000
        private const val READ_TIMEOUT = 15000
    }

    /**
     * Get audio stream URL from a YouTube video URL
     */
    suspend fun getAudioStreamUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(youtubeUrl) ?: run {
            Log.e(TAG, "Could not extract video ID from: $youtubeUrl")
            return@withContext null
        }
        
        for (instance in PIPED_INSTANCES) {
            try {
                val streamUrl = fetchStreamFromInstance(instance, videoId)
                if (streamUrl != null) {
                    Log.d(TAG, "Successfully got stream from $instance")
                    return@withContext streamUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get stream from $instance: ${e.message}")
            }
        }
        
        Log.e(TAG, "All Piped instances failed")
        null
    }

    /**
     * Search for videos using Piped API
     */
    suspend fun search(query: String): List<PipedSearchResult> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        
        for (instance in PIPED_INSTANCES) {
            try {
                val results = searchFromInstance(instance, encodedQuery)
                if (results.isNotEmpty()) {
                    return@withContext results
                }
            } catch (e: Exception) {
                Log.w(TAG, "Search failed on $instance: ${e.message}")
            }
        }
        
        emptyList()
    }

    private fun fetchStreamFromInstance(instance: String, videoId: String): String? {
        val url = URL("$instance/streams/$videoId")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            
            if (connection.responseCode != 200) {
                Log.w(TAG, "HTTP ${connection.responseCode} from $instance")
                return null
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            
            // Get audio streams
            val audioStreams = json.optJSONArray("audioStreams") ?: return null
            
            // Find best audio stream (prefer high bitrate)
            var bestStream: JSONObject? = null
            var bestBitrate = 0
            
            for (i in 0 until audioStreams.length()) {
                val stream = audioStreams.getJSONObject(i)
                val bitrate = stream.optInt("bitrate", 0)
                val streamUrl = stream.optString("url", "")
                
                if (streamUrl.isNotEmpty() && bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestStream = stream
                }
            }
            
            return bestStream?.optString("url")
            
        } finally {
            connection.disconnect()
        }
    }

    private fun searchFromInstance(instance: String, query: String): List<PipedSearchResult> {
        // Use 'videos' filter as 'music_songs' can cause issues on some instances
        val url = URL("$instance/search?q=$query&filter=videos")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            
            if (connection.responseCode != 200) {
                return emptyList()
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val items = json.optJSONArray("items") ?: return emptyList()
            
            val results = mutableListOf<PipedSearchResult>()
            
            for (i in 0 until minOf(items.length(), 20)) {
                val item = items.getJSONObject(i)
                val itemUrl = item.optString("url", "")
                
                // Only include video items
                if (itemUrl.startsWith("/watch")) {
                    results.add(
                        PipedSearchResult(
                            videoId = itemUrl.substringAfter("v=").substringBefore("&"),
                            title = item.optString("title", ""),
                            uploaderName = item.optString("uploaderName", "Unknown"),
                            thumbnailUrl = item.optString("thumbnail", ""),
                            duration = item.optLong("duration", 0) * 1000
                        )
                    )
                }
            }
            
            return results
            
        } finally {
            connection.disconnect()
        }
    }

    private fun extractVideoId(youtubeUrl: String): String? {
        // Handle various YouTube URL formats
        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})")
        )
        
        for (pattern in patterns) {
            pattern.find(youtubeUrl)?.groupValues?.get(1)?.let { return it }
        }
        
        // Maybe it's already just the video ID
        if (youtubeUrl.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return youtubeUrl
        }
        
        return null
    }
}

data class PipedSearchResult(
    val videoId: String,
    val title: String,
    val uploaderName: String,
    val thumbnailUrl: String,
    val duration: Long
)
