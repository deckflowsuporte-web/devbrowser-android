package com.devbrowser.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devbrowser.data.model.*
import com.devbrowser.ui.theme.DevBrowserTheme
import com.devbrowser.viewmodel.BrowserViewModel
import android.webkit.WebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val tabState by viewModel.tabState.collectAsStateWithLifecycle()
    val devToolsState by viewModel.devToolsState.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var urlInput by remember { mutableStateOf("") }
    var showDevTools by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentUrl) {
        urlInput = currentUrl
    }

    DevBrowserTheme {
        Scaffold(
            topBar = {
                Column {
                    // URL Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Enter URL") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = { viewModel.loadUrl(urlInput) }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        
                        IconButton(onClick = { showDevTools = !showDevTools }) {
                            Icon(
                                Icons.Default.DeveloperMode,
                                contentDescription = "DevTools",
                                tint = if (showDevTools) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Navigation buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { /* handled by WebView */ }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                        IconButton(onClick = { /* handled by WebView */ }) {
                            Icon(Icons.Default.ArrowForward, "Forward")
                        }
                        IconButton(onClick = { viewModel.refresh(null) }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                        IconButton(onClick = { viewModel.openNewTab() }) {
                            Icon(Icons.Default.Add, "New Tab")
                        }
                    }
                    
                    // Tab bar
                    if (tabState.tabs.size > 1) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(4.dp)
                        ) {
                            tabState.tabs.forEach { tab ->
                                TabChip(
                                    tab = tab,
                                    isSelected = tab.id == tabState.activeTabId,
                                    onSelect = { viewModel.selectTab(tab.id) },
                                    onClose = { viewModel.closeTab(tab.id) }
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // WebView
                Box(
                    modifier = Modifier
                        .weight(if (showDevTools) 0.5f else 1f)
                        .fillMaxWidth()
                ) {
                    val webView = remember {
                        WebView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            viewModel.configureWebView(this)
                            webViewClient = viewModel.getWebViewClient()
                            webChromeClient = viewModel.getWebChromeClient()
                            loadUrl(currentUrl)
                        }
                    }
                    
                    AndroidView(
                        factory = { webView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // DevTools Panel
                if (showDevTools) {
                    DevToolsPanel(
                        state = devToolsState,
                        onPanelSelect = { viewModel.selectDevToolsPanel(it) },
                        onClearConsole = { viewModel.clearConsole() },
                        onDeleteCookie = { viewModel.deleteCookie(it) },
                        onClearCookies = { viewModel.clearAllCookies() },
                        onCopyCookie = { name, value ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Cookie", "$name=$value"))
                        },
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TabChip(
    tab: BrowserTab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tab.title.take(10),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun DevToolsPanel(
    state: DevToolsState,
    onPanelSelect: (DevToolsPanel) -> Unit,
    onClearConsole: () -> Unit,
    onDeleteCookie: (String) -> Unit,
    onClearCookies: () -> Unit,
    onCopyCookie: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // DevTools tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DevToolsPanel.entries.forEach { panel ->
                FilterChip(
                    selected = state.selectedPanel == panel,
                    onClick = { onPanelSelect(panel) },
                    label = { Text(panel.name, fontSize = 12.sp) },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
        
        // Panel content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            when (state.selectedPanel) {
                DevToolsPanel.CONSOLE -> ConsolePanel(
                    entries = state.consoleEntries,
                    onClear = onClearConsole
                )
                DevToolsPanel.ELEMENTS -> ElementsPanel()
                DevToolsPanel.NETWORK -> NetworkPanel(
                    requests = state.networkRequests,
                    expandedRequest = state.expandedRequest
                )
                DevToolsPanel.APPLICATION -> ApplicationPanel(
                    cookies = state.cookies,
                    onDeleteCookie = onDeleteCookie,
                    onClearCookies = onClearCookies,
                    onCopyCookie = onCopyCookie
                )
            }
        }
    }
}

@Composable
fun ConsolePanel(
    entries: List<ConsoleEntry>,
    onClear: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClear) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }
        }
        
        LazyColumn {
            items(entries) { entry ->
                val color = when (entry.level) {
                    ConsoleLevel.ERROR -> Color.Red
                    ConsoleLevel.WARN -> Color.Yellow
                    ConsoleLevel.INFO -> Color.Cyan
                    ConsoleLevel.DEBUG -> Color.Gray
                    ConsoleLevel.LOG -> Color.White
                }
                Text(
                    text = "[${entry.level.name}] ${entry.message}",
                    color = color,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ElementsPanel() {
    Column {
        Text(
            "DOM Inspector",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Element inspection would require CDP connection",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun NetworkPanel(
    requests: List<NetworkRequest>,
    expandedRequest: NetworkRequest?
) {
    LazyColumn {
        items(requests) { request ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${request.method} ${request.url.take(40)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${request.status}",
                    color = if (request.status < 400) Color.Green else Color.Red,
                    fontSize = 11.sp
                )
            }
            HorizontalDivider()
        }
        
        if (requests.isEmpty()) {
            item {
                Text(
                    "No network requests captured",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun ApplicationPanel(
    cookies: List<CookieInfo>,
    onDeleteCookie: (String) -> Unit,
    onClearCookies: () -> Unit,
    onCopyCookie: (String, String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cookies", fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = onClearCookies) {
                    Text("Clear All", fontSize = 12.sp)
                }
            }
        }
        
        LazyColumn {
            items(cookies) { cookie ->
                CookieItem(
                    cookie = cookie,
                    onDelete = { onDeleteCookie(cookie.name) },
                    onCopy = { onCopyCookie(cookie.name, cookie.value) }
                )
            }
            
            if (cookies.isEmpty()) {
                item {
                    Text(
                        "No cookies for this domain",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CookieItem(
    cookie: CookieInfo,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cookie.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                text = cookie.value.take(50) + if (cookie.value.length > 50) "..." else "",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
            Text(
                text = "Domain: ${cookie.domain}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}
