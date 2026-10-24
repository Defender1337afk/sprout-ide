package com.sprout.ide.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprout.ide.data.AppSettings
import com.sprout.ide.lang.Highlighter
import com.sprout.ide.lang.SyntaxColors
import com.sprout.ide.ui.theme.family

private val PadV = 10.dp
private val PadH = 10.dp

@Composable
fun CodeEditor(
    tab: EditorTab,
    settings: AppSettings,
    colors: SyntaxColors,
    matches: List<IntRange>,
    focusRequester: FocusRequester,
    onFontSizeCommit: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val value = tab.value
    val text = value.text
    var fontSize by remember(settings.fontSize) { mutableFloatStateOf(settings.fontSize) }
    val currentFont by rememberUpdatedState(fontSize)

    val textStyle = TextStyle(
        fontFamily = settings.editorFont.family(),
        fontSize = fontSize.sp,
        lineHeight = (fontSize * settings.lineHeight).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        color = colors.text,
        fontFeatureSettings = if (settings.ligatures) null else "liga 0, calt 0",
    )

    val brackets = if (settings.bracketHighlight && value.selection.collapsed) {
        remember(text, value.selection.start) { Highlighter.matchBracket(text, value.selection.start) }
    } else null
    val lang = tab.language
    val annotated = remember(text, lang, colors, matches, brackets, settings.showWhitespace) {
        Highlighter.annotate(text, lang, colors, matches, brackets, settings.showWhitespace)
    }
    val transformation = remember(annotated, text) {
        VisualTransformation { src ->
            TransformedText(if (src.text == text) annotated else AnnotatedString(src.text), OffsetMapping.Identity)
        }
    }
    val lineStarts = remember(text) {
        val list = ArrayList<Int>(text.length / 30 + 1)
        list += 0
        text.forEachIndexed { i, c -> if (c == '\n') list += i + 1 }
        list
    }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val density = LocalDensity.current
    val padV = with(density) { PadV.toPx() }
    val padH = with(density) { PadH.toPx() }

    val measurer = rememberTextMeasurer(cacheSize = 64)
    val gutterStyle = textStyle.copy(color = colors.gutterText, fontFeatureSettings = "tnum")
    val digits = lineStarts.size.toString().length.coerceAtLeast(2)
    val gutterWidthPx = if (settings.lineNumbers) {
        remember(digits, gutterStyle) { measurer.measure("8".repeat(digits), gutterStyle).size.width } + padH * 1.6f
    } else 0f

    val zoomModifier = if (settings.pinchZoom) Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var zoomed = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    val z = event.calculateZoom()
                    if (z != 1f) {
                        fontSize = (fontSize * z).coerceIn(8f, 36f)
                        zoomed = true
                        event.changes.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
            if (zoomed) onFontSizeCommit(currentFont)
        }
    } else Modifier

    val currentLineIndex = remember(lineStarts, value.selection.start) {
        val idx = lineStarts.binarySearch(value.selection.start)
        if (idx >= 0) idx else -idx - 2
    }

    BoxWithConstraints(modifier.background(colors.background).then(zoomModifier)) {
        val viewportH = constraints.maxHeight
        val viewportW = constraints.maxWidth

        // keep the cursor visible while typing / navigating
        LaunchedEffect(value.selection, text, layout) {
            val l = layout ?: return@LaunchedEffect
            val off = value.selection.end.coerceIn(0, l.layoutInput.text.length)
            val r = l.getCursorRect(off)
            val top = r.top + padV
            val bottom = r.bottom + padV
            val margin = r.height * 1.5f
            if (top < vScroll.value) vScroll.scrollTo((top - margin).toInt().coerceAtLeast(0))
            else if (bottom > vScroll.value + viewportH) vScroll.scrollTo((bottom - viewportH + margin).toInt())
            if (!settings.wordWrap) {
                val x = r.left + padH
                val visibleW = viewportW - gutterWidthPx
                if (x < hScroll.value + padH) hScroll.scrollTo((x - visibleW / 3).toInt().coerceAtLeast(0))
                else if (x > hScroll.value + visibleW - padH * 2) hScroll.scrollTo((x - visibleW * 2 / 3).toInt())
            }
        }

        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(colors.cursor, colors.selection),
        ) {
            Row(Modifier.fillMaxSize().verticalScroll(vScroll)) {
                if (settings.lineNumbers) {
                    val l = layout
                    val heightDp = with(density) { ((l?.size?.height ?: 0) + padV * 2).toDp() }
                    Canvas(Modifier.width(with(density) { gutterWidthPx.toDp() }).height(heightDp).background(colors.gutter)) {
                        if (l == null) return@Canvas
                        val visTop = vScroll.value - padV
                        val visBottom = vScroll.value + viewportH.toFloat()
                        val maxOffset = l.layoutInput.text.length
                        // binary search first visible logical line
                        var lo = 0
                        var hi = lineStarts.size - 1
                        while (lo < hi) {
                            val mid = (lo + hi + 1) / 2
                            val y = l.getLineTop(l.getLineForOffset(lineStarts[mid].coerceAtMost(maxOffset)))
                            if (y <= visTop) lo = mid else hi = mid - 1
                        }
                        var i = lo
                        while (i < lineStarts.size) {
                            val start = lineStarts[i].coerceAtMost(maxOffset)
                            val y = l.getLineTop(l.getLineForOffset(start))
                            if (y > visBottom) break
                            val current = i == currentLineIndex
                            val res = measurer.measure(
                                (i + 1).toString(),
                                if (current) gutterStyle.copy(color = colors.cursor, fontWeight = FontWeight.Bold) else gutterStyle,
                            )
                            drawText(res, topLeft = Offset(size.width - res.size.width - padH * 0.8f, y + padV))
                            i++
                        }
                    }
                }
                Box(
                    Modifier.weight(1f).then(if (settings.wordWrap) Modifier else Modifier.horizontalScroll(hScroll))
                ) {
                    val minW = with(density) { (viewportW - gutterWidthPx).toDp() }
                    val minH = with(density) { viewportH.toDp() }
                    BasicTextField(
                        value = value,
                        onValueChange = { tab.onValueChange(it, settings) },
                        textStyle = textStyle,
                        visualTransformation = transformation,
                        cursorBrush = SolidColor(colors.cursor),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                        ),
                        onTextLayout = { layout = it },
                        modifier = Modifier
                            .then(if (settings.wordWrap) Modifier.fillMaxWidth() else Modifier.widthIn(min = minW))
                            .heightIn(min = minH)
                            .drawBehind {
                                val l = layout ?: return@drawBehind
                                if (!settings.highlightLine || !value.selection.collapsed) return@drawBehind
                                val maxOffset = l.layoutInput.text.length
                                val start = lineStarts.getOrElse(currentLineIndex) { 0 }.coerceAtMost(maxOffset)
                                val end = (lineStarts.getOrNull(currentLineIndex + 1)?.minus(1) ?: maxOffset).coerceIn(start, maxOffset)
                                val top = l.getLineTop(l.getLineForOffset(start))
                                val bottom = l.getLineBottom(l.getLineForOffset(end))
                                drawRect(colors.currentLine, Offset(0f, top + padV), Size(size.width, bottom - top))
                            }
                            .padding(horizontal = PadH, vertical = PadV)
                            .focusRequester(focusRequester),
                    )
                }
            }
        }
    }
}
