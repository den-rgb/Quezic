package com.quezic.data.local

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages proxy settings for YouTube stream extraction.
 * 
 * When a proxy URL is configured, the app will use the self-hosted proxy
 * to extract YouTube streams instead of direct API calls.
 */
@Singleton
class ProxySettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Get the configured proxy URL.
     * Defaults to the official Quezic proxy if not configured.
     */
    var proxyUrl: String?
        get() {
            val url = prefs.getString(KEY_PROXY_URL, DEFAULT_PROXY_URL)?.takeIf { it.isNotBlank() }
            // Validate and fix URL format
            return url?.let { normalizeUrl(it) }
        }
        set(value) {
            prefs.edit().putString(KEY_PROXY_URL, value?.trim()?.let { normalizeUrl(it) }).apply()
        }
    
    /**
     * Normalize URL to ensure proper format (https://)
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        
        // Fix common issues: https: without // 
        if (normalized.startsWith("https:") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized.removePrefix("https:")
        }
        if (normalized.startsWith("http:") && !normalized.startsWith("http://")) {
            normalized = "http://" + normalized.removePrefix("http:")
        }
        
        // Add https:// if no scheme
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        
        return normalized.trimEnd('/')
    }

    /**
     * Check if a proxy is configured and enabled.
     */
    val isProxyEnabled: Boolean
        get() = proxyUrl != null && proxyEnabled

    /**
     * Enable/disable the proxy (even if URL is configured).
     */
    var proxyEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROXY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PROXY_ENABLED, value).apply()
        }

    /**
     * Test if the proxy is reachable.
     */
    suspend fun testProxy(url: String): ProxyTestResult {
        return try {
            val testUrl = url.trimEnd('/') + "/health"
            val connection = java.net.URL(testUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                ProxyTestResult.Success
            } else {
                ProxyTestResult.Error("Server returned $responseCode")
            }
        } catch (e: Exception) {
            ProxyTestResult.Error(e.message ?: "Connection failed")
        }
    }

    /**
     * Check if cookies are configured on the proxy server.
     */
    suspend fun getCookieStatus(url: String): CookieStatusResult {
        return try {
            val statusUrl = url.trimEnd('/') + "/cookies/status"
            val connection = java.net.URL(statusUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val hasCookies = json.optBoolean("hasCookies", false)
                CookieStatusResult.Success(hasCookies)
            } else {
                CookieStatusResult.Error("Server returned $responseCode")
            }
        } catch (e: Exception) {
            CookieStatusResult.Error(e.message ?: "Connection failed")
        }
    }

    /**
     * Upload cookies to the proxy server.
     */
    suspend fun uploadCookies(url: String, cookieContent: String): CookieUploadResult {
        return try {
            val uploadUrl = url.trimEnd('/') + "/cookies"
            val connection = java.net.URL(uploadUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/plain")

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(cookieContent)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val message = json.optString("message", "Cookies uploaded")
                CookieUploadResult.Success(message)
            } else {
                val errorResponse = try {
                    connection.errorStream?.bufferedReader()?.readText() ?: "Upload failed"
                } catch (_: Exception) { "Upload failed" }
                val errorMsg = try {
                    JSONObject(errorResponse).optString("error", "Upload failed ($responseCode)")
                } catch (_: Exception) { "Upload failed ($responseCode)" }
                CookieUploadResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            CookieUploadResult.Error(e.message ?: "Upload failed")
        }
    }

    /**
     * Delete cookies from the proxy server.
     */
    suspend fun deleteCookies(url: String): CookieUploadResult {
        return try {
            val deleteUrl = url.trimEnd('/') + "/cookies"
            val connection = java.net.URL(deleteUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "DELETE"

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                CookieUploadResult.Success("Cookies deleted")
            } else {
                CookieUploadResult.Error("Failed to delete cookies ($responseCode)")
            }
        } catch (e: Exception) {
            CookieUploadResult.Error(e.message ?: "Delete failed")
        }
    }

    /**
     * Read file content from a URI using ContentResolver.
     */
    fun readFileContent(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract YouTube cookies from Android's WebView CookieManager
     * and convert them to Netscape cookie format.
     *
     * Key challenge: CookieManager.getCookie() returns ALL cookies that would be
     * sent to a URL (including parent-domain cookies). We need to attribute each
     * cookie to its correct domain (.google.com vs .youtube.com) for yt-dlp.
     */
    fun extractWebViewCookies(): String? {
        val cookieManager = android.webkit.CookieManager.getInstance()
        // Flush to ensure all cookies are persisted before reading
        cookieManager.flush()

        // Known Google auth cookies that live on .google.com domain
        // yt-dlp requires these on .google.com, NOT .youtube.com
        val googleAuthCookieNames = setOf(
            "SID", "HSID", "SSID", "APISID", "SAPISID",
            "LSID", "NID", "OGPC", "AEC", "SOCS",
            "__Secure-1PSID", "__Secure-3PSID",
            "__Secure-1PAPISID", "__Secure-3PAPISID",
            "__Secure-1PSIDTS", "__Secure-3PSIDTS",
            "__Secure-1PSIDCC", "__Secure-3PSIDCC",
            "1P_JAR", "SIDCC", "__Secure-ENID"
        )

        val expiry = (System.currentTimeMillis() / 1000 + 86400 * 365).toString()
        val lines = mutableListOf(
            "# Netscape HTTP Cookie File",
            "# Extracted from Android WebView by Quezic",
            ""
        )
        // Track cookie names we've already written to avoid duplicates
        val writtenCookies = mutableSetOf<String>()

        // Step 1: Extract .google.com cookies (auth cookies)
        val googleCookieString = cookieManager.getCookie("https://www.google.com")
        if (googleCookieString != null) {
            parseCookieString(googleCookieString).forEach { (name, value) ->
                if (name.isNotBlank() && writtenCookies.add("google:$name")) {
                    lines.add(".google.com\tTRUE\t/\tTRUE\t$expiry\t$name\t$value")
                }
            }
        }

        // Step 2: Extract .youtube.com cookies, excluding ones that belong to .google.com
        val ytCookieString = cookieManager.getCookie("https://www.youtube.com")
        if (ytCookieString != null) {
            parseCookieString(ytCookieString).forEach { (name, value) ->
                if (name.isNotBlank()) {
                    if (name in googleAuthCookieNames) {
                        // This is a Google auth cookie - write to .google.com if not already
                        if (writtenCookies.add("google:$name")) {
                            lines.add(".google.com\tTRUE\t/\tTRUE\t$expiry\t$name\t$value")
                        }
                    } else {
                        // YouTube-specific cookie
                        if (writtenCookies.add("youtube:$name")) {
                            lines.add(".youtube.com\tTRUE\t/\tTRUE\t$expiry\t$name\t$value")
                        }
                    }
                }
            }
        }

        // Step 3: Extract accounts.google.com cookies
        val accountsCookieString = cookieManager.getCookie("https://accounts.google.com")
        if (accountsCookieString != null) {
            parseCookieString(accountsCookieString).forEach { (name, value) ->
                if (name.isNotBlank() && writtenCookies.add("google:$name")) {
                    lines.add(".google.com\tTRUE\t/\tTRUE\t$expiry\t$name\t$value")
                }
            }
        }

        return if (lines.size > 3) lines.joinToString("\n") + "\n" else null
    }

    /**
     * Parse a cookie header string ("name=value; name2=value2") into name-value pairs.
     */
    private fun parseCookieString(cookieString: String): List<Pair<String, String>> {
        return cookieString.split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { cookie ->
                val eqIndex = cookie.indexOf('=')
                if (eqIndex > 0) {
                    val name = cookie.substring(0, eqIndex).trim()
                    val value = cookie.substring(eqIndex + 1).trim()
                    name to value
                } else null
            }
    }

    companion object {
        private const val PREFS_NAME = "quezic_proxy_settings"
        private const val KEY_PROXY_URL = "proxy_url"
        private const val KEY_PROXY_ENABLED = "proxy_enabled"
        
        // Default proxy server hosted on Railway
        const val DEFAULT_PROXY_URL = "https://delightful-friendship-production-e4ff.up.railway.app"
    }
}

sealed class ProxyTestResult {
    object Success : ProxyTestResult()
    data class Error(val message: String) : ProxyTestResult()
}

sealed class CookieStatusResult {
    data class Success(val hasCookies: Boolean) : CookieStatusResult()
    data class Error(val message: String) : CookieStatusResult()
}

sealed class CookieUploadResult {
    data class Success(val message: String) : CookieUploadResult()
    data class Error(val message: String) : CookieUploadResult()
}
