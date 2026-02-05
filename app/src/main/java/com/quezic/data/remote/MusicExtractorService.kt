package com.quezic.data.remote

import android.util.Log
import com.quezic.data.local.ProxySettings
import com.quezic.domain.model.SearchResult
import com.quezic.domain.model.SourceType
import com.quezic.domain.model.StreamQuality
import com.quezic.player.YouTubeDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for extracting music from YouTube and SoundCloud.
 * Uses multiple fallback services for reliability:
 * 1. Self-hosted proxy (if configured - uses Puppeteer for valid tokens)
 * 2. NewPipe Extractor (primary - direct extraction)
 * 3. Piped API (fallback - uses CDN instances)
 * 4. Invidious API (fallback - privacy-focused YouTube frontend)
 */
@Singleton
class MusicExtractorService @Inject constructor(
    private val pipedApiService: PipedApiService,
    private val invidiousApiService: InvidiousApiService,
    private val proxySettings: ProxySettings
) {

    companion object {
        private const val TAG = "MusicExtractor"
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 500L
        private const val YOUTUBE_SEARCH_API = "https://www.youtube.com/youtubei/v1/search"
        private const val YOUTUBE_PLAYER_API = "https://www.youtube.com/youtubei/v1/player"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        // Android client - often has better access to streams
        private const val ANDROID_USER_AGENT = "com.google.android.youtube/19.09.37 (Linux; U; Android 14; en_US; Pixel 8 Pro Build/UP1A.231005.007) gzip"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    init {
        NewPipeInit.init()
    }

    /**
     * Search for music across multiple sources
     */
    suspend fun search(
        query: String,
        sources: List<SourceType>
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        
        if (query.isBlank()) return@withContext emptyList()

        sources.forEach { source ->
            try {
                when (source) {
                    SourceType.YOUTUBE -> {
                        val youtubeResults = searchYouTube(query)
                        results.addAll(youtubeResults)
                    }
                    SourceType.SOUNDCLOUD -> {
                        val soundcloudResults = searchSoundCloud(query)
                        results.addAll(soundcloudResults)
                    }
                    else -> { /* Skip unsupported sources */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching $source: ${e.message}", e)
            }
        }
        
        results
    }

    private suspend fun searchYouTube(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        // Try NewPipe YouTube search first
        try {
            Log.d(TAG, "Trying NewPipe YouTube search for: $query")
            val results = searchYouTubeWithNewPipe(query)
            if (results.isNotEmpty()) {
                Log.d(TAG, "NewPipe YouTube found ${results.size} results")
                return@withContext results
            } else {
                Log.w(TAG, "NewPipe YouTube returned empty results")
            }
        } catch (e: Exception) {
            Log.w(TAG, "NewPipe YouTube search failed: ${e.message}", e)
        }
        
        // Try direct YouTube API search
        try {
            Log.d(TAG, "Trying direct YouTube API search for: $query")
            val directResults = searchYouTubeDirectApi(query)
            if (directResults.isNotEmpty()) {
                Log.d(TAG, "Direct YouTube API found ${directResults.size} results")
                return@withContext directResults
            } else {
                Log.w(TAG, "Direct YouTube API returned empty results")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct YouTube API search failed: ${e.message}", e)
        }
        
        // Fallback to Piped API
        try {
            Log.d(TAG, "Trying Piped API search for: $query")
            val pipedResults = pipedApiService.search("$query music")
            if (pipedResults.isNotEmpty()) {
                Log.d(TAG, "Piped API found ${pipedResults.size} results")
                return@withContext pipedResults.map { result ->
                    SearchResult(
                        id = "yt_${result.videoId}",
                        title = cleanTitle(result.title),
                        artist = cleanArtistName(result.uploaderName),
                        thumbnailUrl = result.thumbnailUrl,
                        duration = result.duration,
                        sourceType = SourceType.YOUTUBE,
                        sourceId = "https://www.youtube.com/watch?v=${result.videoId}",
                        sourceUrl = "https://www.youtube.com/watch?v=${result.videoId}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Piped search also failed: ${e.message}")
        }
        
        Log.e(TAG, "All YouTube search methods failed for: $query")
        emptyList()
    }

    private suspend fun searchYouTubeWithNewPipe(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val youtubeService = ServiceList.YouTube
        // Simple search - append "audio" for better music results
        val searchExtractor = youtubeService.getSearchExtractor(query)
        
        Log.d(TAG, "NewPipe: Fetching search page for query: $query")
        Log.d(TAG, "NewPipe: Search URL: ${searchExtractor.url}")
        
        searchExtractor.fetchPage()
        
        val initialPage = searchExtractor.initialPage
        val items = initialPage.items
        Log.d(TAG, "NewPipe raw items: ${items.size}")
        Log.d(TAG, "NewPipe: Has more pages: ${initialPage.hasNextPage()}")
        
        // Log first few items for debugging
        items.take(3).forEachIndexed { idx, item ->
            Log.d(TAG, "NewPipe item $idx: ${item.javaClass.simpleName} - ${item.name}")
        }
        
        val results = items.filterIsInstance<StreamInfoItem>()
            .filter { it.duration > 0 }
            .take(15)
            .map { item: StreamInfoItem ->
                SearchResult(
                    id = "yt_${item.url.hashCode()}",
                    title = cleanTitle(item.name),
                    artist = cleanArtistName(item.uploaderName ?: "Unknown Artist"),
                    thumbnailUrl = item.thumbnails.maxByOrNull { thumb -> thumb.width }?.url 
                        ?: item.thumbnails.firstOrNull()?.url,
                    duration = item.duration * 1000,
                    sourceType = SourceType.YOUTUBE,
                    sourceId = item.url,
                    sourceUrl = item.url
                )
            }
        
        Log.d(TAG, "NewPipe filtered results: ${results.size}")
        results
    }

    /**
     * Search YouTube using its internal API directly.
     * This is a fallback when NewPipe extractor fails.
     */
    private suspend fun searchYouTubeDirectApi(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20260101.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", query)
        }

        val request = Request.Builder()
            .url("$YOUTUBE_SEARCH_API?prettyPrint=false")
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Accept", "application/json")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("Content-Type", "application/json")
            .addHeader("Origin", "https://www.youtube.com")
            .addHeader("Referer", "https://www.youtube.com/")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        
        response.use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "YouTube API returned ${resp.code}")
                return@withContext emptyList<SearchResult>()
            }

            val body = resp.body?.string() ?: return@withContext emptyList<SearchResult>()
            val json = JSONObject(body)
            
            val results = mutableListOf<SearchResult>()
            
            // Navigate through the complex YouTube response structure
            val contents = json.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            
            if (contents == null) {
                Log.w(TAG, "Could not find contents in YouTube response")
                return@withContext emptyList<SearchResult>()
            }
            
            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i)
                val itemSection = section?.optJSONObject("itemSectionRenderer")
                val sectionContents = itemSection?.optJSONArray("contents") ?: continue
                
                for (j in 0 until sectionContents.length()) {
                    val item = sectionContents.optJSONObject(j)
                    val videoRenderer = item?.optJSONObject("videoRenderer") ?: continue
                    
                    val videoId = videoRenderer.optString("videoId")
                    if (videoId.isNullOrBlank()) continue
                    
                    val title = videoRenderer.optJSONObject("title")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: continue
                    
                    val channel = videoRenderer.optJSONObject("ownerText")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: "Unknown Artist"
                    
                    val lengthText = videoRenderer.optJSONObject("lengthText")
                        ?.optString("simpleText")
                    val durationMs = parseDuration(lengthText)
                    
                    if (durationMs <= 0) continue // Skip live streams or invalid
                    
                    val thumbnail = videoRenderer.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                        ?.optJSONObject(0)
                        ?.optString("url")
                    
                    results.add(SearchResult(
                        id = "yt_$videoId",
                        title = cleanTitle(title),
                        artist = cleanArtistName(channel),
                        thumbnailUrl = thumbnail,
                        duration = durationMs,
                        sourceType = SourceType.YOUTUBE,
                        sourceId = "https://www.youtube.com/watch?v=$videoId",
                        sourceUrl = "https://www.youtube.com/watch?v=$videoId"
                    ))
                    
                    if (results.size >= 15) break
                }
                if (results.size >= 15) break
            }
            
            Log.d(TAG, "Direct API parsed ${results.size} results")
            results
        }
    }
    
    private fun parseDuration(durationText: String?): Long {
        if (durationText.isNullOrBlank()) return 0
        
        val parts = durationText.split(":").reversed()
        var totalMs = 0L
        
        if (parts.isNotEmpty()) totalMs += (parts[0].toLongOrNull() ?: 0) * 1000 // seconds
        if (parts.size > 1) totalMs += (parts[1].toLongOrNull() ?: 0) * 60 * 1000 // minutes
        if (parts.size > 2) totalMs += (parts[2].toLongOrNull() ?: 0) * 60 * 60 * 1000 // hours
        
        return totalMs
    }

    private suspend fun searchSoundCloud(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        retryWithBackoff {
            val soundcloudService = ServiceList.SoundCloud
            val searchExtractor = soundcloudService.getSearchExtractor(query)
            searchExtractor.fetchPage()
            
            val items = searchExtractor.initialPage.items
            
            items.filterIsInstance<StreamInfoItem>()
                .filter { it.duration > 0 }
                .take(10)
                .map { item ->
                    SearchResult(
                        id = "sc_${item.url.hashCode()}",
                        title = item.name,
                        artist = item.uploaderName ?: "Unknown Artist",
                        thumbnailUrl = item.thumbnails.maxByOrNull { it.width }?.url
                            ?: item.thumbnails.firstOrNull()?.url,
                        duration = item.duration * 1000,
                        sourceType = SourceType.SOUNDCLOUD,
                        sourceId = item.url,
                        sourceUrl = item.url
                    )
                }
        } ?: emptyList()
    }

    /**
     * Get the actual audio stream URL for a song.
     * Tries multiple services in order until one works.
     */
    suspend fun getStreamUrl(
        sourceType: SourceType,
        sourceId: String,
        quality: StreamQuality = StreamQuality.HIGH
    ): String? = withContext(Dispatchers.IO) {
        try {
            when (sourceType) {
                SourceType.YOUTUBE -> getYouTubeStreamUrl(sourceId, quality)
                SourceType.SOUNDCLOUD -> getSoundCloudStreamUrl(sourceId, quality, forDownload = false)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stream URL: ${e.message}", e)
            null
        }
    }

    /**
     * Get a downloadable audio stream URL for a song.
     * Prefers progressive (direct) streams over HLS/DASH for downloads.
     */
    suspend fun getDownloadUrl(
        sourceType: SourceType,
        sourceId: String,
        quality: StreamQuality = StreamQuality.HIGH
    ): String? = withContext(Dispatchers.IO) {
        try {
            when (sourceType) {
                SourceType.YOUTUBE -> getYouTubeStreamUrl(sourceId, quality, forDownload = true)
                SourceType.SOUNDCLOUD -> getSoundCloudStreamUrl(sourceId, quality, forDownload = true)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting download URL: ${e.message}", e)
            null
        }
    }

    private suspend fun getYouTubeStreamUrl(
        videoUrl: String,
        quality: StreamQuality,
        forDownload: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        
        // Extract video ID for proxy
        val videoId = extractVideoId(videoUrl)
        
        // 0. Try self-hosted proxy first (most reliable when configured)
        if (proxySettings.isProxyEnabled && videoId != null) {
            Log.d(TAG, "Trying self-hosted proxy for: $videoId (forDownload=$forDownload)")
            try {
                val proxyUrl = getStreamFromProxy(videoId, forDownload)
                if (proxyUrl != null) {
                    Log.d(TAG, "✓ Got stream URL via proxy: ${proxyUrl.take(100)}...")
                    YouTubeDataSourceFactory.lastSuccessfulClient = "WEB"
                    return@withContext proxyUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "Proxy extraction failed: ${e.message}")
            }
        }
        
        // 1. Try NewPipe (direct extraction)
        Log.d(TAG, "Trying NewPipe extractor for: $videoUrl (forDownload=$forDownload)")
        try {
            val streamUrl = getYouTubeStreamWithNewPipe(videoUrl, quality, forDownload)
            if (streamUrl != null) {
                Log.d(TAG, "✓ Got stream URL via NewPipe: ${streamUrl.take(100)}...")
                return@withContext streamUrl
            } else {
                Log.w(TAG, "NewPipe returned null stream URL")
            }
        } catch (e: Exception) {
            Log.e(TAG, "NewPipe extraction failed: ${e.javaClass.simpleName}: ${e.message}", e)
        }
        
        // 2. Try direct YouTube Player API
        Log.d(TAG, "Trying direct YouTube Player API...")
        try {
            val directUrl = getYouTubeStreamDirectApi(videoUrl, quality)
            if (directUrl != null) {
                Log.d(TAG, "✓ Got stream URL via direct API: ${directUrl.take(100)}...")
                return@withContext directUrl
            } else {
                Log.w(TAG, "Direct YouTube API returned null")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct YouTube API failed: ${e.message}", e)
        }
        
        // For downloads, don't use third-party APIs as they may return HLS
        // Note: The proxy is already tried first and returns direct URLs for downloads
        if (forDownload) {
            Log.e(TAG, "All download methods failed for: $videoUrl")
            return@withContext null
        }
        
        // 3. Try Piped API
        Log.d(TAG, "Trying Piped API...")
        try {
            val pipedUrl = pipedApiService.getAudioStreamUrl(videoUrl)
            if (pipedUrl != null) {
                Log.d(TAG, "✓ Got stream URL via Piped: ${pipedUrl.take(100)}...")
                return@withContext pipedUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "Piped API failed: ${e.message}")
        }
        
        // 4. Try Invidious API
        Log.d(TAG, "Trying Invidious API...")
        try {
            val invidiousUrl = invidiousApiService.getAudioStreamUrl(videoUrl)
            if (invidiousUrl != null) {
                Log.d(TAG, "✓ Got stream URL via Invidious: ${invidiousUrl.take(100)}...")
                return@withContext invidiousUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "Invidious API failed: ${e.message}")
        }
        
        Log.e(TAG, "All extraction methods failed for: $videoUrl")
        null
    }
    
    /**
     * Get stream URL using YouTube's internal Player API directly.
     * Uses multiple client types - TV embedded client works best as it doesn't require PoToken.
     */
    private suspend fun getYouTubeStreamDirectApi(
        videoUrl: String,
        quality: StreamQuality
    ): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(videoUrl) ?: run {
            Log.w(TAG, "Could not extract video ID from: $videoUrl")
            return@withContext null
        }
        
        // Try scraping the watch page first - this works like a browser
        try {
            Log.d(TAG, "Trying watch page scraping...")
            val streamUrl = tryGetStreamFromWatchPage(videoId, quality)
            if (streamUrl != null) {
                Log.d(TAG, "Got stream via watch page scraping")
                YouTubeDataSourceFactory.lastSuccessfulClient = "WEB"
                return@withContext streamUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "Watch page scraping failed: ${e.message}")
        }
        
        // Try TV HTML5 embedded client - doesn't require PoToken
        try {
            Log.d(TAG, "Trying TVHTML5_SIMPLY_EMBEDDED_PLAYER client...")
            val streamUrl = tryGetStreamWithTvEmbeddedClient(videoId, quality)
            if (streamUrl != null) {
                Log.d(TAG, "Got stream via TV embedded client")
                return@withContext streamUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "TV embedded client failed: ${e.message}")
        }
        
        // Fallback to other clients - try MWEB first as it sometimes works without PoToken
        val clients = listOf(
            // Mobile Web client - doesn't require app attestation
            Triple("MWEB", "2.20260101.01.00", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"),
            // iOS client - doesn't require PoToken/attestation  
            Triple("IOS", "19.09.3", "com.google.ios.youtube/19.09.3 (iPhone16,2; U; CPU iOS 17_4 like Mac OS X;)"),
            // Android client - requires PoToken but sometimes works
            Triple("ANDROID", "19.09.37", ANDROID_USER_AGENT),
            // Web client
            Triple("WEB", "2.20260101.01.00", USER_AGENT)
        )
        
        for ((clientName, clientVersion, userAgent) in clients) {
            try {
                val streamUrl = tryGetStreamWithClient(videoId, clientName, clientVersion, userAgent, quality)
                if (streamUrl != null) {
                    Log.d(TAG, "Got stream via $clientName client")
                    // Update the data source factory to use matching User-Agent
                    YouTubeDataSourceFactory.lastSuccessfulClient = clientName
                    return@withContext streamUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "$clientName client failed: ${e.message}")
            }
        }
        
        null
    }
    
    /**
     * Try getting stream by scraping the YouTube watch page.
     * This extracts the player config from the page HTML, similar to how yt-dlp works.
     */
    private fun tryGetStreamFromWatchPage(
        videoId: String,
        quality: StreamQuality
    ): String? {
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        
        val request = Request.Builder()
            .url(watchUrl)
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("Cookie", "CONSENT=PENDING+987")
            .get()
            .build()
        
        val response = httpClient.newCall(request).execute()
        
        return response.use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "Watch page returned ${resp.code}")
                return@use null
            }
            
            val html = resp.body?.string() ?: return@use null
            
            // Look for ytInitialPlayerResponse in the HTML
            val playerResponsePattern = Regex("""var ytInitialPlayerResponse\s*=\s*(\{.+?\});""")
            val match = playerResponsePattern.find(html)
            
            if (match == null) {
                // Try alternative pattern
                val altPattern = Regex("""ytInitialPlayerResponse\s*=\s*(\{.+?\});""")
                val altMatch = altPattern.find(html)
                
                if (altMatch == null) {
                    Log.w(TAG, "Could not find player response in watch page")
                    return@use null
                }
                
                val jsonStr = altMatch.groupValues[1]
                parsePlayerResponseJson(jsonStr, quality)
            } else {
                val jsonStr = match.groupValues[1]
                parsePlayerResponseJson(jsonStr, quality)
            }
        }
    }
    
    private fun parsePlayerResponseJson(jsonStr: String, quality: StreamQuality): String? {
        return try {
            val json = JSONObject(jsonStr)
            
            // Check playability
            val playabilityStatus = json.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status")
            if (status != "OK") {
                val reason = playabilityStatus?.optString("reason") ?: "Unknown"
                Log.w(TAG, "Video not playable (watch page): $reason")
                return null
            }
            
            val streamingData = json.optJSONObject("streamingData")
            if (streamingData == null) {
                Log.w(TAG, "No streaming data in watch page response")
                return null
            }
            
            parseAudioStreams(streamingData, quality)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse player response: ${e.message}")
            null
        }
    }
    
    /**
     * Try getting stream using TV HTML5 embedded player client.
     * This client is used for embedded videos and doesn't require PoToken authentication.
     */
    private fun tryGetStreamWithTvEmbeddedClient(
        videoId: String,
        quality: StreamQuality
    ): String? {
        val requestBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "TVHTML5_SIMPLY_EMBEDDED_PLAYER")
                    put("clientVersion", "2.0")
                    put("hl", "en")
                    put("gl", "US")
                    put("clientScreen", "EMBED")
                })
                put("thirdParty", JSONObject().apply {
                    put("embedUrl", "https://www.youtube.com/")
                })
            })
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }

        val request = Request.Builder()
            .url("$YOUTUBE_PLAYER_API?prettyPrint=false")
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Origin", "https://www.youtube.com")
            .addHeader("Referer", "https://www.youtube.com/")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        
        return response.use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "TV embedded API returned ${resp.code}")
                return@use null
            }

            val body = resp.body?.string() ?: return@use null
            val json = JSONObject(body)
            
            // Check playability
            val playabilityStatus = json.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status")
            if (status != "OK") {
                val reason = playabilityStatus?.optString("reason") ?: "Unknown"
                Log.w(TAG, "Video not playable (TV embedded): $reason")
                return@use null
            }
            
            // Get streaming data
            val streamingData = json.optJSONObject("streamingData") ?: run {
                Log.w(TAG, "No streaming data in TV embedded response")
                return@use null
            }
            
            // Parse and return best audio stream
            parseAudioStreams(streamingData, quality)
        }
    }
    
    /**
     * Parse audio streams from YouTube streaming data and select best one based on quality.
     * Also checks for HLS manifest URL as a fallback.
     */
    private fun parseAudioStreams(streamingData: JSONObject, quality: StreamQuality): String? {
        val audioStreams = mutableListOf<Pair<Int, String>>() // bitrate to url
        
        // Try adaptive formats first (audio-only streams)
        val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
        if (adaptiveFormats != null) {
            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.optJSONObject(i) ?: continue
                val mimeType = format.optString("mimeType", "")
                
                // Only audio streams
                if (!mimeType.startsWith("audio/")) continue
                
                // Try direct URL first
                var url = format.optString("url", "")
                
                // If no direct URL, check for signatureCipher (encrypted URL)
                if (url.isBlank()) {
                    val signatureCipher = format.optString("signatureCipher", "")
                    if (signatureCipher.isNotBlank()) {
                        // Skip - we can't decrypt signature cipher without the decryption algorithm
                        Log.d(TAG, "Skipping encrypted stream (signatureCipher)")
                        continue
                    }
                }
                
                if (url.isBlank()) continue
                
                val bitrate = format.optInt("bitrate", 0)
                audioStreams.add(bitrate to url)
            }
        }
        
        // If no adaptive audio, try regular formats (combined audio+video)
        if (audioStreams.isEmpty()) {
            val formats = streamingData.optJSONArray("formats")
            if (formats != null) {
                for (i in 0 until formats.length()) {
                    val format = formats.optJSONObject(i) ?: continue
                    val url = format.optString("url", "")
                    if (url.isBlank()) continue
                    
                    val bitrate = format.optInt("bitrate", 0)
                    audioStreams.add(bitrate to url)
                }
            }
        }
        
        // If still no streams, try HLS manifest URL
        if (audioStreams.isEmpty()) {
            val hlsManifestUrl = streamingData.optString("hlsManifestUrl", "")
            if (hlsManifestUrl.isNotBlank()) {
                Log.d(TAG, "Using HLS manifest URL")
                return hlsManifestUrl
            }
            
            // Also try dashManifestUrl
            val dashManifestUrl = streamingData.optString("dashManifestUrl", "")
            if (dashManifestUrl.isNotBlank()) {
                Log.d(TAG, "Using DASH manifest URL")
                return dashManifestUrl
            }
        }
        
        if (audioStreams.isEmpty()) {
            Log.w(TAG, "No audio streams found")
            return null
        }
        
        // Sort by bitrate and select based on quality
        val sorted = audioStreams.sortedByDescending { it.first }
        Log.d(TAG, "Found ${sorted.size} streams, bitrates: ${sorted.map { it.first / 1000 }}kbps")
        
        val selected = when (quality) {
            StreamQuality.BEST -> sorted.first()
            StreamQuality.HIGH -> sorted.find { it.first <= 256000 } ?: sorted.first()
            StreamQuality.MEDIUM -> sorted.find { it.first <= 128000 } ?: sorted.last()
            StreamQuality.LOW -> sorted.last()
        }
        
        return selected.second
    }
    
    private fun tryGetStreamWithClient(
        videoId: String,
        clientName: String,
        clientVersion: String,
        userAgent: String,
        quality: StreamQuality
    ): String? {
        val requestBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", clientName)
                    put("clientVersion", clientVersion)
                    put("hl", "en")
                    put("gl", "US")
                    when (clientName) {
                        "ANDROID", "ANDROID_MUSIC" -> {
                            put("androidSdkVersion", 34)
                            put("osName", "Android")
                            put("osVersion", "14")
                            put("platform", "MOBILE")
                        }
                        "IOS" -> {
                            put("deviceMake", "Apple")
                            put("deviceModel", "iPhone16,2")
                            put("osName", "iOS")
                            put("osVersion", "17.4.1")
                            put("platform", "MOBILE")
                        }
                        "MWEB" -> {
                            put("platform", "MOBILE")
                            put("clientFormFactor", "SMALL_FORM_FACTOR")
                        }
                        "WEB" -> {
                            put("platform", "DESKTOP")
                        }
                    }
                })
            })
            put("videoId", videoId)
            put("playbackContext", JSONObject().apply {
                put("contentPlaybackContext", JSONObject().apply {
                    put("signatureTimestamp", System.currentTimeMillis() / 1000)
                })
            })
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }
        
        // Map client names to YouTube client IDs
        val clientId = when (clientName) {
            "ANDROID" -> "3"
            "ANDROID_MUSIC" -> "21"
            "IOS" -> "5"
            "IOS_MUSIC" -> "26"
            "MWEB" -> "2"  // Mobile web
            "WEB" -> "1"
            else -> "1"
        }

        val request = Request.Builder()
            .url("$YOUTUBE_PLAYER_API?prettyPrint=false")
            .addHeader("User-Agent", userAgent)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Origin", "https://www.youtube.com")
            .addHeader("X-YouTube-Client-Name", clientId)
            .addHeader("X-YouTube-Client-Version", clientVersion)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        
        return response.use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "YouTube Player API returned ${resp.code} for $clientName")
                return@use null
            }

            val body = resp.body?.string() ?: return@use null
            val json = JSONObject(body)
            
            // Check playability
            val playabilityStatus = json.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status")
            if (status != "OK") {
                val reason = playabilityStatus?.optString("reason") ?: "Unknown"
                Log.w(TAG, "Video not playable ($clientName): $reason")
                return@use null
            }
            
            // Get streaming data
            val streamingData = json.optJSONObject("streamingData") ?: run {
                Log.w(TAG, "No streaming data in response ($clientName)")
                return@use null
            }
            
            // Parse and return audio streams
            parseAudioStreams(streamingData, quality)
        }
    }
    
    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/v/([a-zA-Z0-9_-]{11})")
        )
        
        for (pattern in patterns) {
            pattern.find(url)?.groupValues?.get(1)?.let { return it }
        }
        
        // Maybe it's already just the video ID
        if (url.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return url
        }
        
        return null
    }
    
    /**
     * Get stream URL from self-hosted proxy server.
     * For streaming: Uses /proxy endpoint which streams audio through the server (more reliable)
     * For downloads: Uses the direct YouTube URL from /stream endpoint (required for download manager)
     */
    private fun getStreamFromProxy(videoId: String, forDownload: Boolean = false): String? {
        val proxyUrl = proxySettings.proxyUrl ?: return null
        val baseUrl = proxyUrl.trimEnd('/')
        
        // Get stream info from /stream endpoint
        val streamEndpoint = "$baseUrl/stream/$videoId"
        Log.d(TAG, "Proxy: Requesting stream info from $streamEndpoint")
        
        val request = Request.Builder()
            .url(streamEndpoint)
            .addHeader("Accept", "application/json")
            .get()
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    Log.w(TAG, "Proxy returned ${resp.code}: $errorBody")
                    return@use null
                }
                
                val body = resp.body?.string() ?: return@use null
                val json = JSONObject(body)
                
                if (json.has("error")) {
                    Log.w(TAG, "Proxy error: ${json.getString("error")}")
                    return@use null
                }
                
                val mimeType = json.optString("mimeType", "")
                val directUrl = json.optString("url", "")
                Log.d(TAG, "Proxy: Stream available, mimeType: $mimeType")
                
                if (forDownload) {
                    // For downloads, use the direct YouTube URL
                    // The download manager needs a direct URL, not a proxy
                    if (directUrl.isNotBlank()) {
                        Log.d(TAG, "Proxy: Using direct URL for download")
                        return@use directUrl
                    } else {
                        Log.w(TAG, "Proxy: No direct URL available for download")
                        return@use null
                    }
                } else {
                    // For streaming, use the proxy endpoint for reliability
                    val proxyStreamUrl = "$baseUrl/proxy/$videoId"
                    Log.d(TAG, "Proxy: Using stream proxy URL: $proxyStreamUrl")
                    return@use proxyStreamUrl
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Proxy request failed: ${e.message}")
            null
        }
    }

    private suspend fun getYouTubeStreamWithNewPipe(
        videoUrl: String,
        quality: StreamQuality,
        forDownload: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "NewPipe: Fetching stream info for: $videoUrl (forDownload=$forDownload)")
        
        val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        Log.d(TAG, "NewPipe: Got stream info - title: ${streamInfo.name}")
        
        val audioStreams = streamInfo.audioStreams
        Log.d(TAG, "NewPipe: Found ${audioStreams.size} audio streams")
        
        if (audioStreams.isEmpty()) {
            // Try video streams as fallback (some videos only have combined streams)
            val videoStreams = streamInfo.videoOnlyStreams
            Log.d(TAG, "NewPipe: No audio-only streams, checking ${videoStreams.size} video streams")
            
            // Log available stream types for debugging
            streamInfo.videoStreams.forEach { stream ->
                Log.d(TAG, "  Video+Audio stream: ${stream.format?.name} @ ${stream.resolution}")
            }
            
            Log.w(TAG, "No audio streams found for: $videoUrl")
            return@withContext null
        }
        
        // Log available audio streams for debugging
        audioStreams.forEach { stream ->
            Log.d(TAG, "  Audio stream: ${stream.format?.name} @ ${stream.averageBitrate}kbps, delivery: ${stream.deliveryMethod}, url length: ${stream.content?.length ?: 0}")
        }
        
        val selectedStream = selectAudioStream(audioStreams, quality, preferProgressive = forDownload)
        if (selectedStream == null) {
            Log.w(TAG, "NewPipe: Could not select appropriate stream")
            return@withContext null
        }
        
        Log.d(TAG, "NewPipe: Selected stream: ${selectedStream.format?.name} @ ${selectedStream.averageBitrate}kbps")
        selectedStream.content
    }

    private suspend fun getSoundCloudStreamUrl(
        trackUrl: String,
        quality: StreamQuality,
        forDownload: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        retryWithBackoff {
            Log.d(TAG, "SoundCloud: Fetching stream for: $trackUrl (forDownload=$forDownload)")
            val streamInfo = StreamInfo.getInfo(ServiceList.SoundCloud, trackUrl)
            val audioStreams = streamInfo.audioStreams
            
            if (audioStreams.isEmpty()) {
                Log.w(TAG, "No audio streams found for: $trackUrl")
                return@retryWithBackoff null
            }
            
            // Log available streams for debugging
            audioStreams.forEach { stream ->
                Log.d(TAG, "  SoundCloud stream: ${stream.format?.name} @ ${stream.averageBitrate}kbps, delivery: ${stream.deliveryMethod}")
            }
            
            val selectedStream = selectAudioStream(audioStreams, quality, preferProgressive = forDownload)
            if (selectedStream != null) {
                Log.d(TAG, "Selected stream: ${selectedStream.format?.name}, delivery: ${selectedStream.deliveryMethod}")
            }
            selectedStream?.content
        }
    }

    private fun selectAudioStream(
        streams: List<AudioStream>,
        quality: StreamQuality,
        preferProgressive: Boolean = false
    ): AudioStream? {
        if (streams.isEmpty()) return null
        
        // For downloads, prefer progressive streams (direct URLs) over HLS/DASH
        val candidateStreams = if (preferProgressive) {
            val progressiveStreams = streams.filter { 
                it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP 
            }
            if (progressiveStreams.isNotEmpty()) {
                Log.d(TAG, "Found ${progressiveStreams.size} progressive streams for download")
                progressiveStreams
            } else {
                Log.w(TAG, "No progressive streams found, falling back to all streams")
                streams
            }
        } else {
            streams
        }
        
        val sortedStreams = candidateStreams.sortedByDescending { it.averageBitrate }
        
        return when (quality) {
            StreamQuality.BEST -> sortedStreams.firstOrNull()
            StreamQuality.HIGH -> sortedStreams.find { it.averageBitrate <= 256 } ?: sortedStreams.firstOrNull()
            StreamQuality.MEDIUM -> sortedStreams.find { it.averageBitrate <= 128 } ?: sortedStreams.lastOrNull()
            StreamQuality.LOW -> sortedStreams.lastOrNull()
        }
    }

    suspend fun getRelatedSongs(
        sourceType: SourceType,
        sourceId: String,
        limit: Int = 10
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            when (sourceType) {
                SourceType.YOUTUBE -> getYouTubeRelated(sourceId, limit)
                SourceType.SOUNDCLOUD -> getSoundCloudRelated(sourceId, limit)
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting related songs: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun getYouTubeRelated(
        videoUrl: String,
        limit: Int
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
            val relatedItems = streamInfo.relatedItems
            
            relatedItems
                .filterIsInstance<StreamInfoItem>()
                .filter { it.duration > 0 }
                .take(limit)
                .map { item ->
                    SearchResult(
                        id = "yt_${item.url.hashCode()}",
                        title = cleanTitle(item.name),
                        artist = cleanArtistName(item.uploaderName ?: "Unknown Artist"),
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        duration = item.duration * 1000,
                        sourceType = SourceType.YOUTUBE,
                        sourceId = item.url,
                        sourceUrl = item.url
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get related songs: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getSoundCloudRelated(
        trackUrl: String,
        limit: Int
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        retryWithBackoff {
            val streamInfo = StreamInfo.getInfo(ServiceList.SoundCloud, trackUrl)
            val relatedItems = streamInfo.relatedItems
            
            relatedItems
                .filterIsInstance<StreamInfoItem>()
                .filter { it.duration > 0 }
                .take(limit)
                .map { item ->
                    SearchResult(
                        id = "sc_${item.url.hashCode()}",
                        title = item.name,
                        artist = item.uploaderName ?: "Unknown Artist",
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        duration = item.duration * 1000,
                        sourceType = SourceType.SOUNDCLOUD,
                        sourceId = item.url,
                        sourceUrl = item.url
                    )
                }
        } ?: emptyList()
    }

    suspend fun searchByArtist(
        artistName: String,
        sources: List<SourceType>
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        search("$artistName songs", sources)
    }

    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = MAX_RETRIES,
        initialDelay: Long = RETRY_DELAY_MS,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay *= 2
                } else {
                    Log.e(TAG, "All retries failed", e)
                }
            }
        }
        return null
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("\\(Official.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[Official.*?]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Lyrics.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[Lyrics.*?]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Audio.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[Audio.*?]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(HD\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(HQ\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\|.*$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanArtistName(name: String): String {
        return name
            .replace(Regex(" - Topic$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("VEVO$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("Official$", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
