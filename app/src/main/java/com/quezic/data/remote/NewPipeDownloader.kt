package com.quezic.data.remote

import android.util.Log
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Custom Downloader implementation for NewPipe Extractor v0.25.1.
 * Handles HTTP requests including YouTube consent cookies and proper headers.
 */
class NewPipeDownloader private constructor() : Downloader() {

    companion object {
        private const val TAG = "NewPipeDownloader"
        private var instance: NewPipeDownloader? = null
        
        @JvmStatic
        fun getInstance(): NewPipeDownloader {
            if (instance == null) {
                instance = NewPipeDownloader()
            }
            return instance!!
        }

        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        private val CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
        private val READ_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
        
        // YouTube consent cookies - updated for 2025/2026
        private const val YOUTUBE_CONSENT_COOKIE = "SOCS=CAISNQgDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjQwMTIxLjA0X3AxGgJlbiACGgYIgJbBrgY; CONSENT=PENDING+987; GPS=1"
    }

    // Store cookies between requests
    private val cookies = mutableMapOf<String, String>()

    init {
        // Pre-set YouTube consent cookies
        cookies["youtube.com"] = YOUTUBE_CONSENT_COOKIE
        cookies["www.youtube.com"] = YOUTUBE_CONSENT_COOKIE
        cookies["m.youtube.com"] = YOUTUBE_CONSENT_COOKIE
        cookies["music.youtube.com"] = YOUTUBE_CONSENT_COOKIE
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        var connection: HttpURLConnection? = null
        
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = httpMethod
                instanceFollowRedirects = true
                useCaches = false
                
                // Set essential headers
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Connection", "keep-alive")
                setRequestProperty("Sec-Fetch-Dest", "document")
                setRequestProperty("Sec-Fetch-Mode", "navigate")
                setRequestProperty("Sec-Fetch-Site", "none")
                setRequestProperty("Sec-Fetch-User", "?1")
                setRequestProperty("Upgrade-Insecure-Requests", "1")
                setRequestProperty("Cache-Control", "max-age=0")
                
                // YouTube-specific headers
                if (url.contains("youtube") || url.contains("googlevideo")) {
                    setRequestProperty("Origin", "https://www.youtube.com")
                    setRequestProperty("Referer", "https://www.youtube.com/")
                }
            }

            // Add cookies for YouTube consent bypass
            val host = URL(url).host
            val cookieString = buildCookieString(host)
            if (cookieString.isNotEmpty()) {
                connection.setRequestProperty("Cookie", cookieString)
            }

            // Add custom headers from request
            for ((headerName, headerValueList) in headers) {
                if (headerValueList.isNotEmpty()) {
                    connection.setRequestProperty(headerName, headerValueList.joinToString("; "))
                }
            }

            // Handle POST data
            if (dataToSend != null && dataToSend.isNotEmpty()) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Length", dataToSend.size.toString())
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                try {
                    connection.outputStream.use { os ->
                        os.write(dataToSend)
                        os.flush()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing POST data: ${e.message}")
                }
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage ?: ""

            // Store any cookies from response
            connection.headerFields["Set-Cookie"]?.forEach { cookie ->
                storeCookie(host, cookie)
            }

            // Check for captcha/rate limit
            if (responseCode == 429) {
                throw ReCaptchaException("Rate limited by server", url)
            }

            // Read response headers
            val responseHeaders = mutableMapOf<String, List<String>>()
            for ((key, value) in connection.headerFields) {
                if (key != null) {
                    responseHeaders[key] = value
                }
            }

            // Read response body
            val responseBody = try {
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }
                inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (e: Exception) {
                Log.w(TAG, "Error reading response body: ${e.message}")
                ""
            }

            return Response(
                responseCode,
                responseMessage,
                responseHeaders,
                responseBody,
                url
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildCookieString(host: String): String {
        val cookieParts = mutableListOf<String>()
        
        // Add host-specific cookies
        cookies[host]?.let { cookieParts.add(it) }
        
        // Add base domain cookies for YouTube
        if (host.contains("youtube") || host.contains("googlevideo") || host.contains("google")) {
            cookies["youtube.com"]?.let { 
                if (!cookieParts.contains(it)) {
                    cookieParts.add(it) 
                }
            }
        }
        
        return cookieParts.joinToString("; ")
    }

    private fun storeCookie(host: String, cookieHeader: String) {
        // Extract cookie name=value from Set-Cookie header
        val cookiePart = cookieHeader.split(";").firstOrNull() ?: return
        val existing = cookies[host] ?: ""
        cookies[host] = if (existing.isEmpty()) cookiePart else "$existing; $cookiePart"
    }
}
