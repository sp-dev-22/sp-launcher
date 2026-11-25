package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.FullNoteDialog
import org.json.JSONObject

/**
 * In-app browser (v1.19.1): a fullscreen WebView.
 *
 * JavaScript is enabled ONLY so the page text can be read back for the
 * "AI ko bhejo" flow — there is deliberately NO addJavascriptInterface,
 * so a page can never reach app data.
 *
 * HONEST LIMIT (v1.19.1): the AI chat does not exist yet, so NOTHING is ever
 * sent anywhere. Tapping "AI ko bhejo" extracts the page text and shows it
 * in a dialog that says so plainly, with a "Poora dekho" button that opens
 * the full text through [FullNoteDialog]. [onSendToAi] is a reserved seam for
 * the day the AI chat lands — it is never invoked today.
 */
@Composable
fun AiBrowserScreen(
    initialUrl: String = "https://www.google.com",
    // Reserved seam for the AI chat (coming soon): invoked with the page
    // title + text the day the chat lands. Never invoked today — nothing
    // is ever sent anywhere while the chat does not exist.
    onSendToAi: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var urlText by remember { mutableStateOf(initialUrl) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pendingTitle by remember { mutableStateOf("") }
    var pendingText by remember { mutableStateOf("") }
    var showSendDialog by remember { mutableStateOf(false) }

    var showFullNote by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true // Only to read page text back.
            settings.domStorageEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let { currentUrl = it; urlText = it }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.let {
                        canGoBack = it.canGoBack()
                        canGoForward = it.canGoForward()
                        it.url?.let { loaded -> currentUrl = loaded; urlText = loaded }
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progress = newProgress
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (webView.url == null) {
            webView.loadUrl(initialUrl)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.stopLoading()
                webView.destroy()
            } catch (_: Exception) {
                // Destroy must never crash the screen teardown.
            }
        }
    }

    // Hardware back goes back in the page first, and only closes the
    // browser when there is no page history left.
    BackHandler(enabled = canGoBack) {
        webView.goBack()
    }

    fun goTo(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        val url = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        webView.loadUrl(url)
    }

    /**
     * "AI ko bhejo" (v1.19.1): the AI chat does not exist yet, so this only
     * reads the page text back and shows it — it sends nothing anywhere.
     * The dialog says so plainly. When the AI chat lands, this is the place
     * to ask for confirmation and call [onSendToAi] instead.
     */
    fun previewPageText() {
        webView.evaluateJavascript(
            "(function(){return document.title + '\\n' + document.body.innerText;})()"
        ) { value ->
            val decoded = unescapeJsString(value)
            val title = webView.title?.takeIf { it.isNotBlank() } ?: currentUrl
            pendingTitle = title
            pendingText = decoded.take(MAX_SEND_CHARS)
            showSendDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 4.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Browser",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        )
                    } catch (_: Exception) {
                        // No browser app to open; the in-app view still works.
                    }
                }
            ) {
                Text("Bahari browser me kholo", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                placeholder = { Text("Address likho…") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { goTo(urlText) }) {
                Text("Go")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { webView.goBack() }, enabled = canGoBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Pichla page")
            }
            IconButton(onClick = { webView.goForward() }, enabled = canGoForward) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Agla page")
            }
            IconButton(onClick = { webView.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { previewPageText() }) {
                Text("AI ko bhejo")
            }
        }

        if (progress in 1 until 100) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }

    if (showSendDialog) {
        AiComingSoonDialog(
            title = pendingTitle,
            text = pendingText,
            onShowFullNote = { showFullNote = true },
            onDismiss = { showSendDialog = false }
        )
    }

    if (showFullNote) {
        FullNoteDialog(
            title = pendingTitle,
            text = pendingText,
            onDismiss = { showFullNote = false }
        )
    }
}

/** How much of a page is ever shown in the preview at once. */
private const val MAX_SEND_CHARS = 6000

/**
 * Shown instead of a send confirmation until the AI chat exists (v1.19.1).
 * Says plainly that nothing was sent anywhere, shows a short preview of the
 * extracted page text, and offers "Poora dekho" for the full text.
 */
@Composable
private fun AiComingSoonDialog(
    title: String,
    text: String,
    onShowFullNote: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI chat jald aa raha hai", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ye page AI ko nahi bheja gaya — AI chat abhi available nahi hai. Neeche is page ke text ki ek jhalak hai.",
                    fontSize = 13.sp
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = text.take(600),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Theek hai") }
        },
        dismissButton = {
            TextButton(onClick = onShowFullNote) { Text("Poora dekho") }
        }
    )
}

/**
 * evaluateJavascript hands the result back as a JSON string literal
 * (quoted, with \n / \" / \\ / \uXXXX escapes). Decode it by parsing it
 * as the value of a one-key JSON object; fall back to the raw value if
 * the page returned something that is not a string (e.g. "null").
 */
private fun unescapeJsString(value: String?): String {
    if (value.isNullOrBlank() || value == "null") return ""
    return try {
        JSONObject("{\"v\":$value}").optString("v", "")
    } catch (_: Exception) {
        value.trim('"')
    }
}
