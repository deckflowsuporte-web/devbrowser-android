package com.devbrowser.viewmodel

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbrowser.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor() : ViewModel() {

    private val _tabState = MutableStateFlow(TabState())
    val tabState: StateFlow<TabState> = _tabState.asStateFlow()

    private val _devToolsState = MutableStateFlow(DevToolsState())
    val devToolsState: StateFlow<DevToolsState> = _devToolsState.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    fun getWebViewClient(): WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            url?.let {
                _currentUrl.value = it
                updateTab(it, isLoading = true)
                addConsoleEntry(ConsoleLevel.INFO, "Page started: $it")
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            url?.let {
                _currentUrl.value = it
                updateTab(
                    url = it,
                    title = view?.title ?: URL(it).host,
                    isLoading = false,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false
                )
                addConsoleEntry(ConsoleLevel.INFO, "Page finished: $it")
            }
        }
    }

    fun getWebChromeClient(): WebChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
            message?.let {
                addConsoleEntry(ConsoleLevel.LOG, "[$sourceID:$lineNumber] $it")
            }
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress == 100) {
                loadCookies(_currentUrl.value)
            }
        }
    }

    fun configureWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        WebView.setWebContentsDebuggingEnabled(true)
    }

    fun loadUrl(url: String) {
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        
        _currentUrl.value = formattedUrl
        updateTab(formattedUrl)
    }

    fun goBack(webView: WebView?) {
        webView?.goBack()
    }

    fun goForward(webView: WebView?) {
        webView?.goForward()
    }

    fun refresh(webView: WebView?) {
        webView?.reload()
    }

    fun openNewTab() {
        viewModelScope.launch {
            val newId = (_tabState.value.tabs.maxOfOrNull { it.id } ?: 0) + 1
            if (_tabState.value.tabs.size < 10) {
                _tabState.update { state ->
                    state.copy(
                        tabs = state.tabs + BrowserTab(id = newId),
                        activeTabId = newId
                    )
                }
                _currentUrl.value = "https://www.google.com"
            }
        }
    }

    fun selectTab(tabId: Int) {
        viewModelScope.launch {
            _tabState.update { state ->
                state.copy(activeTabId = tabId)
            }
            _tabState.value.tabs.find { it.id == tabId }?.let { tab ->
                _currentUrl.value = tab.url
            }
        }
    }

    fun closeTab(tabId: Int) {
        viewModelScope.launch {
            _tabState.update { state ->
                val newTabs = state.tabs.filter { it.id != tabId }
                val newActiveId = if (state.activeTabId == tabId) {
                    newTabs.firstOrNull()?.id ?: 0
                } else state.activeTabId
                state.copy(tabs = newTabs, activeTabId = newActiveId)
            }
        }
    }

    private fun updateTab(
        url: String,
        title: String? = null,
        isLoading: Boolean? = null,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null
    ) {
        viewModelScope.launch {
            _tabState.update { state ->
                state.copy(
                    tabs = state.tabs.map { tab ->
                        if (tab.id == state.activeTabId) {
                            tab.copy(
                                url = url,
                                title = title ?: tab.title,
                                isLoading = isLoading ?: tab.isLoading,
                                canGoBack = canGoBack ?: tab.canGoBack,
                                canGoForward = canGoForward ?: tab.canGoForward
                            )
                        } else tab
                    }
                )
            }
        }
    }

    // DevTools functions
    fun toggleDevTools() {
        _devToolsState.update { it.copy(isVisible = !it.isVisible) }
    }

    fun selectDevToolsPanel(panel: DevToolsPanel) {
        _devToolsState.update { it.copy(selectedPanel = panel) }
        
        // Load data for the selected panel
        when (panel) {
            DevToolsPanel.APPLICATION -> loadCookies(_currentUrl.value)
            DevToolsPanel.NETWORK -> { /* Network monitoring handled separately */ }
            else -> { }
        }
    }

    private fun addConsoleEntry(level: ConsoleLevel, message: String) {
        _devToolsState.update { state ->
            state.copy(
                consoleEntries = state.consoleEntries + ConsoleEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    message = message,
                    source = "console"
                )
            )
        }
    }

    fun clearConsole() {
        _devToolsState.update { it.copy(consoleEntries = emptyList()) }
    }

    fun addNetworkRequest(request: NetworkRequest) {
        _devToolsState.update { state ->
            state.copy(networkRequests = state.networkRequests + request)
        }
    }

    fun loadCookies(url: String) {
        viewModelScope.launch {
            try {
                val cookieManager = CookieManager.getInstance()
                val cookieString = cookieManager.getCookie(url) ?: ""
                
                val cookies = if (cookieString.isNotEmpty()) {
                    cookieString.split(";").mapNotNull { cookie ->
                        val parts = cookie.trim().split("=", limit = 2)
                        if (parts.size == 2) {
                            CookieInfo(
                                name = parts[0].trim(),
                                value = parts[1].trim(),
                                domain = URL(url).host,
                                path = "/",
                                expires = 0,
                                secure = cookie.contains("secure", ignoreCase = true),
                                httpOnly = cookie.contains("httponly", ignoreCase = true),
                                sameSite = ""
                            )
                        } else null
                    }
                } else emptyList()
                
                _devToolsState.update { it.copy(cookies = cookies) }
            } catch (e: Exception) {
                addConsoleEntry(ConsoleLevel.ERROR, "Error loading cookies: ${e.message}")
            }
        }
    }

    fun deleteCookie(cookieName: String) {
        viewModelScope.launch {
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setCookie(_currentUrl.value, "$cookieName=; expires=Thu, 01 Jan 1970 00:00:00 GMT")
                loadCookies(_currentUrl.value)
                addConsoleEntry(ConsoleLevel.INFO, "Cookie deleted: $cookieName")
            } catch (e: Exception) {
                addConsoleEntry(ConsoleLevel.ERROR, "Error deleting cookie: ${e.message}")
            }
        }
    }

    fun clearAllCookies() {
        viewModelScope.launch {
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                _devToolsState.update { it.copy(cookies = emptyList()) }
                addConsoleEntry(ConsoleLevel.INFO, "All cookies cleared")
            } catch (e: Exception) {
                addConsoleEntry(ConsoleLevel.ERROR, "Error clearing cookies: ${e.message}")
            }
        }
    }

    fun expandNetworkRequest(request: NetworkRequest) {
        _devToolsState.update { it.copy(expandedRequest = request) }
    }

    fun collapseNetworkRequest() {
        _devToolsState.update { it.copy(expandedRequest = null) }
    }
}
