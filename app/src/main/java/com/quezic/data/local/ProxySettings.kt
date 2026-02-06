package com.quezic.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
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
