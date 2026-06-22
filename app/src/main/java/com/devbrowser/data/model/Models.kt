package com.devbrowser.data.model

import android.webkit.CookieManager

data class BrowserTab(
    val id: Int,
    val url: String = "https://www.google.com",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)

data class DevToolsTab(
    val name: String,
    val selected: Boolean = false
)

data class ConsoleEntry(
    val timestamp: Long,
    val level: ConsoleLevel,
    val message: String,
    val source: String = ""
)

enum class ConsoleLevel {
    LOG, WARN, ERROR, INFO, DEBUG
}

data class NetworkRequest(
    val id: String,
    val url: String,
    val method: String,
    val status: Int,
    val type: String,
    val time: Long,
    val size: Long = 0,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val timing: Long = 0
)

data class CookieInfo(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expires: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
    val sameSite: String
) {
    companion object {
        fun fromCookieManager(url: String): List<CookieInfo> {
            val cookieManager = CookieManager.getInstance()
            val cookieString = cookieManager.getCookie(url) ?: return emptyList()
            
            return cookieString.split(";").mapNotNull { cookie ->
                val parts = cookie.trim().split("=")
                if (parts.size >= 2) {
                    CookieInfo(
                        name = parts[0].trim(),
                        value = parts.drop(1).joinToString("=").trim(),
                        domain = "",
                        path = "/",
                        expires = 0,
                        secure = cookie.contains("Secure"),
                        httpOnly = cookie.contains("HttpOnly"),
                        sameSite = ""
                    )
                } else null
            }
        }
    }
}

data class StorageInfo(
    val type: StorageType,
    val key: String,
    val value: String
)

enum class StorageType {
    LOCAL_STORAGE, SESSION_STORAGE, COOKIES, INDEXED_DB
}

data class TabState(
    val tabs: List<BrowserTab> = listOf(BrowserTab(id = 0)),
    val activeTabId: Int = 0
) {
    val activeTab: BrowserTab?
        get() = tabs.find { it.id == activeTabId }
}

data class DevToolsState(
    val selectedPanel: DevToolsPanel = DevToolsPanel.CONSOLE,
    val consoleEntries: List<ConsoleEntry> = emptyList(),
    val networkRequests: List<NetworkRequest> = emptyList(),
    val cookies: List<CookieInfo> = emptyList(),
    val storage: List<StorageInfo> = emptyList(),
    val isVisible: Boolean = false,
    val expandedRequest: NetworkRequest? = null
)

enum class DevToolsPanel {
    CONSOLE, ELEMENTS, NETWORK, APPLICATION
}
