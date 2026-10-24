package com.sprout.ide.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.sprout.ide.data.AppSettings
import com.sprout.ide.lang.Language
import com.sprout.ide.lang.Syntax

/** Auto-indent, bracket pairing and line operations. Pure functions over TextFieldValue. */
object SmartEdit {
    private val pairs = mapOf('(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\'', '`' to '`')
    private val closers = setOf(')', ']', '}', '"', '\'', '`')

    fun process(old: TextFieldValue, new: TextFieldValue, s: AppSettings, lang: Language): TextFieldValue {
        val o = old.text
        val n = new.text
        if (!old.selection.collapsed || !new.selection.collapsed) return new
        val p = old.selection.start
        // single character typed at the cursor
        if (n.length == o.length + 1 && new.selection.start == p + 1 &&
            n.regionMatches(0, o, 0, p) && n.regionMatches(p + 1, o, p, o.length - p)
        ) {
            val c = n[p]
            val next = o.getOrNull(p)
            if (c == '\n' && s.autoIndent) return newline(o, p, s, lang)
            if (s.autoClose) {
                if (c in closers && next == c) return TextFieldValue(o, TextRange(p + 1))
                val close = pairs[c]
                if (close != null && (next == null || next.isWhitespace() || next in closers || next in ",;")) {
                    val prev = o.getOrNull(p - 1)
                    val isQuote = c == close
                    val skip = isQuote && (prev?.isLetterOrDigit() == true || (c == '\'' && lang.syntax == Syntax.Markdown))
                    if (!skip && !(c == '\'' && lang.syntax in setOf(Syntax.Plain, Syntax.Markdown))) {
                        return TextFieldValue(o.substring(0, p) + c + close + o.substring(p), TextRange(p + 1))
                    }
                }
                if (c == '>' && lang.syntax == Syntax.Markup) closeTag(o, p)?.let { return it }
            }
            return new
        }
        // backspace between an empty pair removes both
        if (s.autoClose && n.length == o.length - 1 && new.selection.start == p - 1 && p > 0 && p < o.length) {
            val open = o[p - 1]
            if (pairs[open] == o[p] && n == o.removeRange(p - 1, p)) {
                return TextFieldValue(o.removeRange(p - 1, p + 1), TextRange(p - 1))
            }
        }
        return new
    }

    private fun newline(o: String, p: Int, s: AppSettings, lang: Language): TextFieldValue {
        val lineStart = o.lastIndexOf('\n', p - 1) + 1
        val line = o.substring(lineStart, p)
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        val before = line.trimEnd().lastOrNull()
        val after = o.getOrNull(p)
        val opens = before in setOf('{', '[', '(') ||
            (before == ':' && lang.syntax in setOf(Syntax.Python, Syntax.Yaml)) ||
            (lang.syntax == Syntax.Markup && line.trimEnd().endsWith(">") && !line.trimEnd().endsWith("/>") &&
                Regex("<([A-Za-z][\\w-]*)[^>]*>\\s*$").find(line)?.groupValues?.get(1)?.lowercase() !in voidTags &&
                !line.contains("</"))
        val unit = s.indentUnit
        return if (opens && after != null && (pairs[before] == after || (lang.syntax == Syntax.Markup && o.startsWith("</", p)))) {
            val insert = "\n" + indent + unit + "\n" + indent
            TextFieldValue(o.substring(0, p) + insert + o.substring(p), TextRange(p + 1 + indent.length + unit.length))
        } else {
            val insert = "\n" + indent + if (opens) unit else ""
            TextFieldValue(o.substring(0, p) + insert + o.substring(p), TextRange(p + insert.length))
        }
    }

    private val voidTags = setOf("br", "img", "input", "meta", "link", "hr", "source", "area", "base", "col", "wbr")

    private fun closeTag(o: String, p: Int): TextFieldValue? {
        val open = o.lastIndexOf('<', p - 1)
        if (open < 0) return null
        val tag = o.substring(open, p)
        val m = Regex("^<([A-Za-z][\\w-]*)[^<>]*$").find(tag) ?: return null
        if (tag.endsWith("/")) return null
        val name = m.groupValues[1]
        if (name.lowercase() in voidTags) return null
        return TextFieldValue(o.substring(0, p) + "></$name>" + o.substring(p), TextRange(p + 1))
    }

    // --- actions -----------------------------------------------------------------------------------

    fun insert(v: TextFieldValue, text: String, s: AppSettings): TextFieldValue {
        val t = if (text == "⇥") s.indentUnit else text
        val start = v.selection.min
        val end = v.selection.max
        val close = if (s.autoClose && t.length == 1) pairs[t[0]] else null
        return if (close != null && start != end) {
            val wrapped = t + v.text.substring(start, end) + close
            TextFieldValue(v.text.replaceRange(start, end, wrapped), TextRange(start + 1, end + 1))
        } else if (close != null && t[0] != close) {
            TextFieldValue(v.text.replaceRange(start, end, t + close), TextRange(start + 1))
        } else {
            TextFieldValue(v.text.replaceRange(start, end, t), TextRange(start + t.length))
        }
    }

    private fun lineBounds(text: String, sel: TextRange): IntRange {
        val start = text.lastIndexOf('\n', sel.min - 1) + 1
        val endIdx = text.indexOf('\n', maxOf(sel.max - if (sel.max > sel.min && text.getOrNull(sel.max - 1) == '\n') 1 else 0, start))
        val end = if (endIdx < 0) text.length else endIdx
        return start until end
    }

    private fun mapLines(v: TextFieldValue, f: (List<String>) -> List<String>): TextFieldValue {
        val r = lineBounds(v.text, v.selection)
        val block = v.text.substring(r.first, r.last + 1)
        val replaced = f(block.split('\n')).joinToString("\n")
        val text = v.text.replaceRange(r.first, r.last + 1, replaced)
        val sel = if (v.selection.collapsed) {
            val delta = replaced.length - block.length
            TextRange((v.selection.start + delta).coerceIn(r.first, r.first + replaced.length))
        } else TextRange(r.first, r.first + replaced.length)
        return TextFieldValue(text, sel)
    }

    fun indent(v: TextFieldValue, s: AppSettings) = mapLines(v) { lines -> lines.map { s.indentUnit + it } }

    fun outdent(v: TextFieldValue, s: AppSettings) = mapLines(v) { lines ->
        lines.map { l ->
            when {
                l.startsWith("\t") -> l.substring(1)
                else -> l.drop(l.takeWhile { it == ' ' }.length.coerceAtMost(s.tabSize))
            }
        }
    }

    fun toggleComment(v: TextFieldValue, lang: Language): TextFieldValue {
        val lc = lang.lineComment
        if (lc == null) {
            val (a, b) = lang.blockComment ?: return v
            return mapLines(v) { lines ->
                val joined = lines.joinToString("\n")
                val t = joined.trim()
                listOf(if (t.startsWith(a) && t.endsWith(b)) t.removePrefix(a).removeSuffix(b).trim() else "$a $joined $b")
            }
        }
        return mapLines(v) { lines ->
            val nonBlank = lines.filter { it.isNotBlank() }
            val allCommented = nonBlank.isNotEmpty() && nonBlank.all { it.trimStart().startsWith(lc) }
            val minIndent = nonBlank.minOfOrNull { l -> l.takeWhile { it == ' ' || it == '\t' }.length } ?: 0
            lines.map { l ->
                if (l.isBlank()) l
                else if (allCommented) {
                    val idx = l.indexOf(lc)
                    l.removeRange(idx, idx + lc.length).let { if (it.getOrNull(idx) == ' ') it.removeRange(idx, idx + 1) else it }
                } else l.substring(0, minIndent) + "$lc " + l.substring(minIndent)
            }
        }
    }

    fun duplicateLine(v: TextFieldValue): TextFieldValue {
        val r = lineBounds(v.text, v.selection)
        val block = v.text.substring(r.first, r.last + 1)
        val text = v.text.substring(0, r.last + 1) + "\n" + block + v.text.substring(r.last + 1)
        val shift = block.length + 1
        return TextFieldValue(text, TextRange(v.selection.start + shift, v.selection.end + shift))
    }

    fun deleteLine(v: TextFieldValue): TextFieldValue {
        val r = lineBounds(v.text, v.selection)
        val end = if (r.last + 1 < v.text.length) r.last + 2 else r.last + 1
        val start = if (end == r.last + 1 && r.first > 0) r.first - 1 else r.first
        return TextFieldValue(v.text.removeRange(start, end), TextRange(start.coerceAtMost(v.text.length - (end - start))))
    }

    fun moveLine(v: TextFieldValue, up: Boolean): TextFieldValue {
        val text = v.text
        val r = lineBounds(text, v.selection)
        val block = text.substring(r.first, r.last + 1)
        return if (up) {
            if (r.first == 0) return v
            val prevStart = text.lastIndexOf('\n', r.first - 2) + 1
            val prev = text.substring(prevStart, r.first - 1)
            val newText = text.substring(0, prevStart) + block + "\n" + prev + text.substring(r.last + 1)
            val d = r.first - prevStart
            TextFieldValue(newText, TextRange(v.selection.start - d, v.selection.end - d))
        } else {
            if (r.last + 1 >= text.length) return v
            val nextEnd = text.indexOf('\n', r.last + 2).let { if (it < 0) text.length else it }
            val next = text.substring(r.last + 2, nextEnd)
            val newText = text.substring(0, r.first) + next + "\n" + block + text.substring(nextEnd)
            val d = next.length + 1
            TextFieldValue(newText, TextRange(v.selection.start + d, v.selection.end + d))
        }
    }

    fun selectLine(v: TextFieldValue): TextFieldValue {
        val r = lineBounds(v.text, v.selection)
        return v.copy(selection = TextRange(r.first, r.last + 1))
    }

    fun offsetOfLine(text: String, line: Int): Int {
        var l = 1
        var i = 0
        while (l < line) {
            val nl = text.indexOf('\n', i)
            if (nl < 0) return text.length
            i = nl + 1; l++
        }
        return i
    }
}
