package com.quezic.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Custom DataSource factory for YouTube stream playback.
 * 
 * YouTube requires specific HTTP headers to allow stream playback:
 * - User-Agent: MUST match the client that obtained the stream URL
 * 
 * Without matching User-Agent, YouTube will reject the stream request with 403 Forbidden.
 */
@OptIn(UnstableApi::class)
object YouTubeDataSourceFactory {

    private const val TAG = "YouTubeDataSource"

    // Mobile Web User-Agent
    private const val MWEB_USER_AGENT = 
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
    
    // iOS YouTube app User-Agent
    private const val IOS_USER_AGENT = 
        "com.google.ios.youtube/19.09.3 (iPhone16,2; U; CPU iOS 17_4 like Mac OS X;)"
    
    // Android YouTube app User-Agent
    private const val ANDROID_USER_AGENT = 
        "com.google.android.youtube/19.09.37 (Linux; U; Android 14; en_US; Pixel 8 Pro Build/UP1A.231005.007) gzip"
    
    // Web User-Agent for non-YouTube sources
    private const val WEB_USER_AGENT = 
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    
    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 30_000
    
    // Track the last successful client - updated by MusicExtractorService
    @Volatile
    var lastSuccessfulClient: String = "MWEB"
    
    private fun getCurrentUserAgent(): String {
        return when (lastSuccessfulClient) {
            "MWEB" -> MWEB_USER_AGENT
            "IOS" -> IOS_USER_AGENT
            "ANDROID", "ANDROID_MUSIC" -> ANDROID_USER_AGENT
            "WEB" -> WEB_USER_AGENT
            else -> MWEB_USER_AGENT  // Default to MWEB
        }
    }

    /**
     * Creates a DataSource.Factory configured for YouTube stream playback.
     * Uses OkHttp for better connection handling.
     */
    fun createForYouTube(context: Context): DataSource.Factory {
        val userAgent = getCurrentUserAgent()
        Log.d(TAG, "Creating YouTube DataSource with User-Agent for client: $lastSuccessfulClient")
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        return OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(getYouTubeHeaders())
    }

    /**
     * Creates a DataSource.Factory for general audio playback (SoundCloud, etc.)
     */
    fun createDefault(context: Context): DataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent(WEB_USER_AGENT)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
    }

    /**
     * Creates a DataSource.Factory that automatically detects the source
     * and applies appropriate headers.
     */
    fun createSmart(context: Context): SmartDataSourceFactory {
        return SmartDataSourceFactory(context)
    }

    private fun getYouTubeHeaders(): Map<String, String> {
        // Headers depend on which client was used
        val (clientId, clientVersion) = when (lastSuccessfulClient) {
            "MWEB" -> "2" to "2.20260101.01.00"
            "IOS" -> "5" to "19.09.3"
            "ANDROID" -> "3" to "19.09.37"
            "ANDROID_MUSIC" -> "21" to "6.42.52"
            "WEB" -> "1" to "2.20260101.01.00"
            else -> "2" to "2.20260101.01.00"  // Default to MWEB
        }
        
        // For web-based clients (MWEB, WEB), include Origin/Referer
        val baseHeaders = mutableMapOf(
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Accept-Encoding" to "identity",
            "Connection" to "keep-alive",
            "X-YouTube-Client-Name" to clientId,
            "X-YouTube-Client-Version" to clientVersion
        )
        
        if (lastSuccessfulClient in listOf("MWEB", "WEB")) {
            baseHeaders["Origin"] = "https://www.youtube.com"
            baseHeaders["Referer"] = "https://m.youtube.com/"
        }
        
        return baseHeaders
    }
}

/**
 * Smart DataSource factory that detects the URL source and applies
 * appropriate headers automatically.
 */
@OptIn(UnstableApi::class)
class SmartDataSourceFactory(
    private val context: Context
) : DataSource.Factory {

    // Don't cache factories - create fresh each time to pick up the latest User-Agent settings
    private val defaultFactory = YouTubeDataSourceFactory.createDefault(context)
    private val fileFactory = FileDataSource.Factory()

    override fun createDataSource(): DataSource {
        // Create fresh YouTube factory to pick up current User-Agent
        val youtubeFactory = YouTubeDataSourceFactory.createForYouTube(context)
        return SmartDataSource(youtubeFactory, defaultFactory, fileFactory)
    }
}

/**
 * DataSource wrapper that detects URL type and uses appropriate data source:
 * - Local files (file://) use FileDataSource
 * - YouTube/Google URLs use OkHttpDataSource with custom headers
 * - Other URLs use DefaultHttpDataSource
 */
@OptIn(UnstableApi::class)
class SmartDataSource(
    private val youtubeFactory: DataSource.Factory,
    private val defaultFactory: DataSource.Factory,
    private val fileFactory: DataSource.Factory
) : DataSource {

    private var activeDataSource: DataSource? = null

    override fun open(dataSpec: androidx.media3.datasource.DataSpec): Long {
        val uri = dataSpec.uri
        val scheme = uri.scheme
        val url = uri.toString()
        
        // Choose appropriate data source based on URI scheme/content
        activeDataSource = when {
            // Local file - use FileDataSource
            scheme == "file" || scheme == null || scheme == "content" -> {
                fileFactory.createDataSource()
            }
            // YouTube/Google URLs - use custom headers
            isYouTubeUrl(url) -> {
                youtubeFactory.createDataSource()
            }
            // Other HTTP URLs - use default
            else -> {
                defaultFactory.createDataSource()
            }
        }
        
        return activeDataSource!!.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return activeDataSource?.read(buffer, offset, length) ?: -1
    }

    override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
        activeDataSource?.addTransferListener(transferListener)
    }

    override fun getUri(): android.net.Uri? {
        return activeDataSource?.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return activeDataSource?.responseHeaders ?: emptyMap()
    }

    override fun close() {
        activeDataSource?.close()
        activeDataSource = null
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") ||
                url.contains("youtu.be") ||
                url.contains("googlevideo.com") ||
                url.contains("ytimg.com") ||
                url.contains("yt3.ggpht.com") ||
                url.contains("googleusercontent.com")
    }
}
