package com.quezic.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback service using Invidious API for YouTube audio streams.
 * Invidious is a privacy-focused YouTube frontend.
 */
@Singleton
class InvidiousApiService @Inject constructor() {

    companion object {
        private const val TAG = "InvidiousApi"
        
        // List of public Invidious API instances (updated Feb 2026)
        // Source: https://api.invidious.io/
        // NOTE: Most Invidious instances have disabled their API
        // Only instances with API enabled are listed here
        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.nadeko.net",         // 🇨🇱 Chile - API enabled, 87% uptime
            "https://invidious.protokolla.fi", // 🇫🇮 Finland - may have API
            "https://iv.nbohr.dk",            // 🇩🇰 Denmark - may have API
            "https://invidious.lunar.icu"     // 🇩🇪 Germany - may have API
        )
        
        private const val CONNECT_TIMEOUT = 15000
        private const val READ_TIMEOUT = 15000
    }

    /**
     * Get audio stream URL from a YouTube video ID
     */
    suspend fun getAudioStreamUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(youtubeUrl) ?: run {
            Log.e(TAG, "Could not extract video ID from: $youtubeUrl")
            return@withContext null
        }
        
        for (instance in INVIDIOUS_INSTANCES) {
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
        
        Log.e(TAG, "All Invidious instances failed")
        null
    }

    private fun fetchStreamFromInstance(instance: String, videoId: String): String? {
        val url = URL("$instance/api/v1/videos/$videoId")
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
            
            // Get adaptive formats (audio only)
            val adaptiveFormats = json.optJSONArray("adaptiveFormats") ?: return null
            
            // Find best audio stream
            var bestStream: JSONObject? = null
            var bestBitrate = 0
            
            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.getJSONObject(i)
                val type = format.optString("type", "")
                
                // Only get audio streams
                if (type.startsWith("audio/")) {
                    val bitrate = format.optInt("bitrate", 0)
                    if (bitrate > bestBitrate) {
                        bestBitrate = bitrate
                        bestStream = format
                    }
                }
            }
            
            return bestStream?.optString("url")
            
        } finally {
            connection.disconnect()
        }
    }

    private fun extractVideoId(youtubeUrl: String): String? {
        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})")
        )
        
        for (pattern in patterns) {
            pattern.find(youtubeUrl)?.groupValues?.get(1)?.let { return it }
        }
        
        if (youtubeUrl.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return youtubeUrl
        }
        
        return null
    }
}
