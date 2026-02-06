package com.quezic.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quezic.data.local.CookieStatusResult
import com.quezic.data.local.CookieUploadResult
import com.quezic.data.local.ProxySettings
import com.quezic.data.local.ProxyTestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val proxyUrl: String = "",
    val proxyEnabled: Boolean = true,
    val isTestingProxy: Boolean = false,
    val proxyTestResult: ProxyTestResult? = null,
    val showProxyHelp: Boolean = false,
    // Cookie state
    val cookieStatus: CookieStatus = CookieStatus.Unknown,
    val isCheckingCookies: Boolean = false,
    val isUploadingCookies: Boolean = false,
    val cookieMessage: String? = null,
    val cookieMessageIsError: Boolean = false,
    // YouTube sign-in WebView
    val showYouTubeSignIn: Boolean = false
)

enum class CookieStatus {
    Unknown,
    Configured,
    NotConfigured
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val proxySettings: ProxySettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkCookieStatus()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                proxyUrl = proxySettings.proxyUrl ?: "",
                proxyEnabled = proxySettings.proxyEnabled
            )
        }
    }

    fun updateProxyUrl(url: String) {
        _uiState.update { it.copy(proxyUrl = url, proxyTestResult = null) }
    }

    fun toggleProxyEnabled(enabled: Boolean) {
        proxySettings.proxyEnabled = enabled
        _uiState.update { it.copy(proxyEnabled = enabled) }
    }

    fun saveProxyUrl() {
        val url = _uiState.value.proxyUrl.trim()
        proxySettings.proxyUrl = url.ifBlank { null }
    }

    fun testProxy() {
        val url = _uiState.value.proxyUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(proxyTestResult = ProxyTestResult.Error("URL is empty")) }
            return
        }

        _uiState.update { it.copy(isTestingProxy = true, proxyTestResult = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = proxySettings.testProxy(url)
            _uiState.update { 
                it.copy(
                    isTestingProxy = false, 
                    proxyTestResult = result
                ) 
            }
        }
    }

    fun toggleProxyHelp() {
        _uiState.update { it.copy(showProxyHelp = !it.showProxyHelp) }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(proxyTestResult = null) }
    }
    
    fun resetToDefaultProxy() {
        val defaultUrl = ProxySettings.DEFAULT_PROXY_URL
        proxySettings.proxyUrl = defaultUrl
        _uiState.update { 
            it.copy(
                proxyUrl = defaultUrl, 
                proxyTestResult = null
            ) 
        }
        checkCookieStatus()
    }

    fun checkCookieStatus() {
        val url = proxySettings.proxyUrl ?: _uiState.value.proxyUrl.trim()
        if (url.isBlank()) return

        _uiState.update { it.copy(isCheckingCookies = true) }

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = proxySettings.getCookieStatus(url)) {
                is CookieStatusResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCookies = false,
                            cookieStatus = if (result.hasCookies) CookieStatus.Configured else CookieStatus.NotConfigured
                        )
                    }
                }
                is CookieStatusResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCookies = false,
                            cookieStatus = CookieStatus.Unknown
                        )
                    }
                }
            }
        }
    }

    fun uploadCookies(uri: Uri) {
        val url = proxySettings.proxyUrl ?: _uiState.value.proxyUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(cookieMessage = "Proxy URL is not configured", cookieMessageIsError = true) }
            return
        }

        val content = proxySettings.readFileContent(uri)
        if (content.isNullOrBlank()) {
            _uiState.update { it.copy(cookieMessage = "Could not read file", cookieMessageIsError = true) }
            return
        }

        _uiState.update { it.copy(isUploadingCookies = true, cookieMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = proxySettings.uploadCookies(url, content)) {
                is CookieUploadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUploadingCookies = false,
                            cookieStatus = CookieStatus.Configured,
                            cookieMessage = result.message,
                            cookieMessageIsError = false
                        )
                    }
                }
                is CookieUploadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingCookies = false,
                            cookieMessage = result.message,
                            cookieMessageIsError = true
                        )
                    }
                }
            }
        }
    }

    fun deleteCookies() {
        val url = proxySettings.proxyUrl ?: _uiState.value.proxyUrl.trim()
        if (url.isBlank()) return

        _uiState.update { it.copy(isUploadingCookies = true, cookieMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = proxySettings.deleteCookies(url)) {
                is CookieUploadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUploadingCookies = false,
                            cookieStatus = CookieStatus.NotConfigured,
                            cookieMessage = result.message,
                            cookieMessageIsError = false
                        )
                    }
                }
                is CookieUploadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingCookies = false,
                            cookieMessage = result.message,
                            cookieMessageIsError = true
                        )
                    }
                }
            }
        }
    }

    fun clearCookieMessage() {
        _uiState.update { it.copy(cookieMessage = null) }
    }

    fun showYouTubeSignIn() {
        _uiState.update { it.copy(showYouTubeSignIn = true) }
    }

    fun hideYouTubeSignIn() {
        _uiState.update { it.copy(showYouTubeSignIn = false) }
    }

    /**
     * Called after the user has signed into YouTube via the WebView.
     * Extracts cookies from Android's CookieManager and uploads to the proxy.
     */
    fun extractAndUploadCookies() {
        val url = proxySettings.proxyUrl ?: _uiState.value.proxyUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(
                showYouTubeSignIn = false,
                cookieMessage = "Proxy URL is not configured",
                cookieMessageIsError = true
            ) }
            return
        }

        val cookieContent = proxySettings.extractWebViewCookies()
        if (cookieContent.isNullOrBlank()) {
            _uiState.update { it.copy(
                showYouTubeSignIn = false,
                cookieMessage = "No YouTube cookies found. Make sure you signed in.",
                cookieMessageIsError = true
            ) }
            return
        }

        _uiState.update { it.copy(showYouTubeSignIn = false, isUploadingCookies = true, cookieMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = proxySettings.uploadCookies(url, cookieContent)) {
                is CookieUploadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUploadingCookies = false,
                            cookieStatus = CookieStatus.Configured,
                            cookieMessage = "Cookies extracted and uploaded successfully!",
                            cookieMessageIsError = false
                        )
                    }
                }
                is CookieUploadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingCookies = false,
                            cookieMessage = result.message,
                            cookieMessageIsError = true
                        )
                    }
                }
            }
        }
    }
}
