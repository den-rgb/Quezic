package com.quezic.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val showProxyHelp: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val proxySettings: ProxySettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
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
    }
}
