package com.sprout.ide.ui.screens

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sprout.ide.AppViewModel
import com.sprout.ide.lang.Languages
import com.sprout.ide.ui.theme.JetBrainsMono
import java.io.File

private class LogLine(val level: ConsoleMessage.MessageLevel, val text: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(vm: AppViewModel, file: File) {
    val logs = remember { mutableStateListOf<LogLine>() }
    var consoleOpen by remember { mutableStateOf(Languages.forFile(file.name) == Languages.JavaScript) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val scheme = MaterialTheme.colorScheme
    val lang = Languages.forFile(file.name)

    fun load(w: WebView) {
        logs.clear()
        val base = "file://" + file.parentFile!!.absolutePath + "/"
        when (lang) {
            Languages.JavaScript -> w.loadDataWithBaseURL(
                base,
                """<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                |<style>body{font-family:system-ui;color:${css(scheme.onSurface)};background:${css(scheme.surface)};padding:16px}</style></head>
                |<body><p style="opacity:.6">Скрипт <b>${file.name}</b> выполнен — вывод в консоли ниже.</p>
                |<script src="${file.name}"></script></body></html>""".trimMargin(),
                "text/html", "utf-8", null,
            )
            Languages.Markdown -> w.loadDataWithBaseURL(
                base, markdownToHtml(file.readText(), scheme.onSurface, scheme.surface, scheme.primary, scheme.surfaceContainerHigh),
                "text/html", "utf-8", null,
            )
            else -> w.loadUrl("file://" + file.absolutePath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Предпросмотр · ${lang.name}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.let { load(it) } }) { Icon(Icons.Rounded.Refresh, "Перезапустить") }
                    IconToggleButton(consoleOpen, { consoleOpen = it }) {
                        BadgedBox(badge = {
                            val errors = logs.count { it.level == ConsoleMessage.MessageLevel.ERROR }
                            if (errors > 0) Badge { Text("$errors") }
                        }) { Icon(Icons.Rounded.Terminal, "Консоль") }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        settings.allowFileAccess = true
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                                val where = if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                                    "  (${m.sourceId().substringAfterLast('/')}:${m.lineNumber()})" else ""
                                logs += LogLine(m.messageLevel(), m.message() + where)
                                return true
                            }
                        }
                        setBackgroundColor(scheme.surface.toArgb())
                        webView = this
                        load(this)
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            AnimatedVisibility(consoleOpen) {
                Console(logs) { logs.clear() }
            }
        }
    }
}

@Composable
private fun Console(logs: List<LogLine>, onClear: () -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) state.animateScrollToItem(logs.lastIndex) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge.copy(
        bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp))) {
        Column {
            Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Rounded.Terminal, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Консоль", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteSweep, "Очистить") }
            }
            HorizontalDivider()
            Box(Modifier.heightIn(min = 120.dp, max = 260.dp)) {
                if (logs.isEmpty()) {
                    Text("Нет вывода", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(state = state, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(logs) { l ->
                        val color = when (l.level) {
                            ConsoleMessage.MessageLevel.ERROR -> MaterialTheme.colorScheme.error
                            ConsoleMessage.MessageLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            (if (l.level == ConsoleMessage.MessageLevel.ERROR) "✖ " else "› ") + l.text,
                            color = color, fontFamily = JetBrainsMono, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .background(if (l.level == ConsoleMessage.MessageLevel.ERROR) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

private fun css(c: Color) = String.format("#%06X", c.toArgb() and 0xFFFFFF)

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/** Minimal Markdown renderer: headings, lists, quotes, fenced code, inline code/bold/italic/links. */
private fun markdownToHtml(md: String, fg: Color, bg: Color, accent: Color, codeBg: Color): String {
    val out = StringBuilder()
    var inCode = false
    var inList: String? = null
    fun closeList() { inList?.let { out.append("</$it>") }; inList = null }
    fun inline(s: String): String = esc(s)
        .replace(Regex("`([^`]+)`"), "<code>$1</code>")
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "<b>$1</b>")
        .replace(Regex("\\*([^*]+)\\*"), "<i>$1</i>")
        .replace(Regex("!\\[([^\\]]*)]\\(([^)]+)\\)"), "<img alt=\"$1\" src=\"$2\">")
        .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")
    for (line in md.lines()) {
        if (line.trimStart().startsWith("```")) {
            if (inCode) out.append("</code></pre>") else { closeList(); out.append("<pre><code>") }
            inCode = !inCode; continue
        }
        if (inCode) { out.append(esc(line)).append('\n'); continue }
        val t = line.trim()
        when {
            t.isEmpty() -> closeList()
            t.startsWith("#") -> {
                closeList()
                val lvl = t.takeWhile { it == '#' }.length.coerceAtMost(6)
                out.append("<h$lvl>${inline(t.drop(lvl).trim())}</h$lvl>")
            }
            t.startsWith(">") -> { closeList(); out.append("<blockquote>${inline(t.drop(1).trim())}</blockquote>") }
            t.matches(Regex("[-*+] .*")) -> {
                if (inList != "ul") { closeList(); out.append("<ul>"); inList = "ul" }
                out.append("<li>${inline(t.drop(2))}</li>")
            }
            t.matches(Regex("\\d+\\. .*")) -> {
                if (inList != "ol") { closeList(); out.append("<ol>"); inList = "ol" }
                out.append("<li>${inline(t.substringAfter(". "))}</li>")
            }
            t.matches(Regex("-{3,}|\\*{3,}")) -> { closeList(); out.append("<hr>") }
            else -> { closeList(); out.append("<p>${inline(t)}</p>") }
        }
    }
    closeList()
    if (inCode) out.append("</code></pre>")
    return """<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        |<style>
        |body{font-family:system-ui,sans-serif;color:${css(fg)};background:${css(bg)};padding:8px 20px;line-height:1.6}
        |a,h1,h2,h3{color:${css(accent)}} code{font-family:monospace;background:${css(codeBg)};padding:2px 6px;border-radius:8px}
        |pre{background:${css(codeBg)};padding:14px;border-radius:20px;overflow:auto} pre code{padding:0;background:none}
        |blockquote{border-left:4px solid ${css(accent)};margin:0;padding:4px 14px;opacity:.85} img{max-width:100%;border-radius:16px}
        |</style></head><body>$out</body></html>""".trimMargin()
}
