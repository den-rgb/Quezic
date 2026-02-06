package com.quezic.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Last.fm API service for music intelligence
 * Provides similar artists, similar tracks, and artist info
 * Free tier: No API key required for basic calls
 */
@Singleton
class LastFmService @Inject constructor() {
    
    companion object {
        private const val TAG = "LastFmService"
        private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
        // Last.fm API key (free tier - 5 calls/second, unlimited daily)
        private const val API_KEY = "57ee3318536b23ee81d6b27e36997cde"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Get similar artists to the given artist
     */
    suspend fun getSimilarArtists(artistName: String, limit: Int = 10): List<SimilarArtist> = 
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(artistName, "UTF-8")
                val url = "${BASE_URL}?method=artist.getsimilar&artist=$encoded&api_key=$API_KEY&format=json&limit=$limit"
                
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to get similar artists for $artistName: ${response.code}")
                    return@withContext emptyList()
                }
                
                val json = JSONObject(response.body?.string() ?: "{}")
                val similarArtists = json.optJSONObject("similarartists")
                    ?.optJSONArray("artist") ?: return@withContext emptyList()
                
                val results = mutableListOf<SimilarArtist>()
                for (i in 0 until similarArtists.length()) {
                    val artist = similarArtists.getJSONObject(i)
                    results.add(SimilarArtist(
                        name = artist.optString("name"),
                        matchScore = artist.optString("match").toFloatOrNull() ?: 0f,
                        url = artist.optString("url")
                    ))
                }
                
                Log.d(TAG, "Found ${results.size} similar artists for $artistName")
                results
            } catch (e: Exception) {
                Log.e(TAG, "Error getting similar artists: ${e.message}")
                emptyList()
            }
        }
    
    /**
     * Get similar tracks to the given track
     */
    suspend fun getSimilarTracks(artist: String, track: String, limit: Int = 10): List<SimilarTrack> =
        withContext(Dispatchers.IO) {
            try {
                val encodedArtist = URLEncoder.encode(artist, "UTF-8")
                val encodedTrack = URLEncoder.encode(track, "UTF-8")
                val url = "${BASE_URL}?method=track.getsimilar&artist=$encodedArtist&track=$encodedTrack&api_key=$API_KEY&format=json&limit=$limit"
                
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to get similar tracks: ${response.code}")
                    return@withContext emptyList()
                }
                
                val json = JSONObject(response.body?.string() ?: "{}")
                val similarTracks = json.optJSONObject("similartracks")
                    ?.optJSONArray("track") ?: return@withContext emptyList()
                
                val results = mutableListOf<SimilarTrack>()
                for (i in 0 until similarTracks.length()) {
                    val trackObj = similarTracks.getJSONObject(i)
                    val artistObj = trackObj.optJSONObject("artist")
                    results.add(SimilarTrack(
                        name = trackObj.optString("name"),
                        artist = artistObj?.optString("name") ?: "",
                        matchScore = trackObj.optString("match").toFloatOrNull() ?: 0f,
                        url = trackObj.optString("url")
                    ))
                }
                
                Log.d(TAG, "Found ${results.size} similar tracks for $track by $artist")
                results
            } catch (e: Exception) {
                Log.e(TAG, "Error getting similar tracks: ${e.message}")
                emptyList()
            }
        }
    
    /**
     * Get top tracks for an artist
     */
    suspend fun getArtistTopTracks(artistName: String, limit: Int = 5): List<TopTrack> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(artistName, "UTF-8")
                val url = "${BASE_URL}?method=artist.gettoptracks&artist=$encoded&api_key=$API_KEY&format=json&limit=$limit"
                
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }
                
                val json = JSONObject(response.body?.string() ?: "{}")
                val topTracks = json.optJSONObject("toptracks")
                    ?.optJSONArray("track") ?: return@withContext emptyList()
                
                val results = mutableListOf<TopTrack>()
                for (i in 0 until topTracks.length()) {
                    val track = topTracks.getJSONObject(i)
                    results.add(TopTrack(
                        name = track.optString("name"),
                        artist = artistName,
                        playcount = track.optLong("playcount", 0)
                    ))
                }
                
                results
            } catch (e: Exception) {
                Log.e(TAG, "Error getting top tracks: ${e.message}")
                emptyList()
            }
        }
    
    /**
     * Get artist tags (genres)
     */
    suspend fun getArtistTags(artistName: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(artistName, "UTF-8")
                val url = "${BASE_URL}?method=artist.gettoptags&artist=$encoded&api_key=$API_KEY&format=json"
                
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }
                
                val json = JSONObject(response.body?.string() ?: "{}")
                val tags = json.optJSONObject("toptags")
                    ?.optJSONArray("tag") ?: return@withContext emptyList()
                
                val results = mutableListOf<String>()
                for (i in 0 until minOf(tags.length(), 5)) {
                    results.add(tags.getJSONObject(i).optString("name"))
                }
                
                results
            } catch (e: Exception) {
                Log.e(TAG, "Error getting artist tags: ${e.message}")
                emptyList()
            }
        }
    
    data class SimilarArtist(
        val name: String,
        val matchScore: Float,
        val url: String
    )
    
    data class SimilarTrack(
        val name: String,
        val artist: String,
        val matchScore: Float,
        val url: String
    )
    
    data class TopTrack(
        val name: String,
        val artist: String,
        val playcount: Long
    )
}
