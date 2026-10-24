package com.sprout.ide.lang

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

enum class Tok { Keyword, Type, Str, Num, Comment, Func, Op, Const, Annot, Tag, Attr, Heading, Bold, Code }

/** Single-pass tokenizer: good enough for highlighting while typing, never throws on malformed input. */
object Highlighter {

    class Span(val start: Int, val end: Int, val tok: Tok)

    fun tokenize(text: String, lang: Language): List<Span> = when (lang.syntax) {
        Syntax.Markup -> markup(text, lang)
        Syntax.Markdown -> markdown(text)
        Syntax.Plain -> emptyList()
        else -> code(text, lang)
    }

    fun annotate(
        text: String,
        lang: Language,
        colors: SyntaxColors,
        matches: List<IntRange> = emptyList(),
        brackets: Pair<Int, Int>? = null,
        showWhitespace: Boolean = false,
    ): AnnotatedString {
        val shown = if (showWhitespace) text.replace(' ', '·').replace('\t', '→') else text
        val b = AnnotatedString.Builder(shown)
        if (showWhitespace) {
            var i = shown.indexOfAny(charArrayOf('·', '→'))
            while (i >= 0) {
                if (text[i] == ' ' || text[i] == '\t') b.addStyle(SpanStyle(color = colors.gutterText.copy(alpha = 0.45f)), i, i + 1)
                i = shown.indexOfAny(charArrayOf('·', '→'), i + 1)
            }
        }
        for (s in tokenize(text, lang)) {
            if (s.end <= s.start || s.end > text.length) continue
            b.addStyle(style(s.tok, colors), s.start, s.end)
        }
        for (m in matches) {
            if (m.last < text.length) b.addStyle(SpanStyle(background = colors.match), m.first, m.last + 1)
        }
        if (brackets != null) {
            val bg = SpanStyle(background = colors.bracket, fontWeight = FontWeight.Bold)
            if (brackets.first in text.indices) b.addStyle(bg, brackets.first, brackets.first + 1)
            if (brackets.second in text.indices) b.addStyle(bg, brackets.second, brackets.second + 1)
        }
        return b.toAnnotatedString()
    }

    private fun style(t: Tok, c: SyntaxColors): SpanStyle = when (t) {
        Tok.Keyword -> SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold)
        Tok.Type -> SpanStyle(color = c.type)
        Tok.Str -> SpanStyle(color = c.string)
        Tok.Num -> SpanStyle(color = c.number)
        Tok.Comment -> SpanStyle(color = c.comment, fontStyle = FontStyle.Italic)
        Tok.Func -> SpanStyle(color = c.function)
        Tok.Op -> SpanStyle(color = c.operator)
        Tok.Const -> SpanStyle(color = c.constant)
        Tok.Annot -> SpanStyle(color = c.annotation)
        Tok.Tag -> SpanStyle(color = c.tag, fontWeight = FontWeight.SemiBold)
        Tok.Attr -> SpanStyle(color = c.attribute)
        Tok.Heading -> SpanStyle(color = c.keyword, fontWeight = FontWeight.Bold)
        Tok.Bold -> SpanStyle(color = c.text, fontWeight = FontWeight.Bold)
        Tok.Code -> SpanStyle(color = c.string, background = c.currentLine)
    }

    private const val OPS = "+-*/%=<>!&|^~?:.,;"

    private fun code(text: String, lang: Language): List<Span> {
        val out = ArrayList<Span>()
        val n = text.length
        val line = lang.lineComment
        val block = lang.blockComment
        val cssLike = lang.syntax == Syntax.Css
        val jsonLike = lang.syntax == Syntax.Json
        val yamlLike = lang.syntax == Syntax.Yaml
        val sqlLike = lang.syntax == Syntax.Sql
        val shellLike = lang.syntax == Syntax.Shell
        var i = 0
        var lineStart = true
        while (i < n) {
            val ch = text[i]
            if (ch == '\n') { i++; lineStart = true; continue }
            if (ch == ' ' || ch == '\t') { i++; continue }
            val wasLineStart = lineStart
            lineStart = false
            // comments
            if (block != null && text.startsWith(block.first, i)) {
                val end = text.indexOf(block.second, i + block.first.length).let { if (it < 0) n else it + block.second.length }
                out += Span(i, end, Tok.Comment); i = end; continue
            }
            if (line != null && text.startsWith(line, i) && !(shellLike && i > 0 && text[i - 1] == '$')) {
                val end = text.indexOf('\n', i).let { if (it < 0) n else it }
                out += Span(i, end, Tok.Comment); i = end; continue
            }
            // strings
            if (ch in lang.stringDelims) {
                val triple = lang.tripleQuotes && text.startsWith("$ch$ch$ch", i)
                val end = if (triple) {
                    text.indexOf("$ch$ch$ch", i + 3).let { if (it < 0) n else it + 3 }
                } else {
                    var j = i + 1
                    while (j < n && text[j] != ch && (text[j] != '\n' || ch == '`')) {
                        if (text[j] == '\\') j++
                        j++
                    }
                    minOf(j + 1, n)
                }
                // JSON keys and YAML keys look like attributes
                val tok = if (jsonLike && text.nextNonSpace(end) == ':') Tok.Attr else Tok.Str
                out += Span(i, end, tok); i = end; continue
            }
            // numbers
            if (ch.isDigit() || (ch == '.' && i + 1 < n && text[i + 1].isDigit())) {
                var j = i + 1
                while (j < n && (text[j].isLetterOrDigit() || text[j] == '.' || text[j] == '_')) j++
                out += Span(i, j, Tok.Num); i = j; continue
            }
            // annotations / decorators / preprocessor
            if ((ch == '@' || (ch == '#' && lang.lineComment != "#" && !cssLike)) && i + 1 < n && text[i + 1].isLetter()) {
                var j = i + 1
                while (j < n && (text[j].isLetterOrDigit() || text[j] == '_' || text[j] == '.')) j++
                out += Span(i, j, Tok.Annot); i = j; continue
            }
            // shell variables
            if (shellLike && ch == '$') {
                var j = i + 1
                if (j < n && text[j] == '{') { j = text.indexOf('}', j).let { if (it < 0) n else it + 1 } }
                else while (j < n && (text[j].isLetterOrDigit() || text[j] == '_')) j++
                out += Span(i, j, Tok.Const); i = j; continue
            }
            if (ch.isLetter() || ch == '_' || (cssLike && ch == '-')) {
                var j = i + 1
                while (j < n && (text[j].isLetterOrDigit() || text[j] == '_' || ((cssLike || yamlLike) && text[j] == '-'))) j++
                if (j < n && text[j] == '!' && lang.id == "rust") j++
                val w = text.substring(i, j)
                val next = text.nextNonSpace(j)
                val tok = when {
                    yamlLike && wasLineStart && (next == ':' || next == '=') -> Tok.Attr
                    yamlLike -> if (w == "true" || w == "false" || w == "null") Tok.Const else null
                    cssLike && next == ':' && text.lastNonSpace(i).let { it == '{' || it == ';' || it == '\n' || it == null } -> Tok.Attr
                    cssLike && next == '{' -> Tok.Tag
                    cssLike && next == '(' -> Tok.Func
                    cssLike -> null
                    w in lang.keywords || (sqlLike && w.lowercase() in lang.keywords) -> Tok.Keyword
                    w in lang.constants -> Tok.Const
                    w in lang.types || (sqlLike && w.lowercase() in lang.types) -> Tok.Type
                    next == '(' || w.endsWith('!') -> Tok.Func
                    w[0].isUpperCase() && lang.syntax == Syntax.CLike -> Tok.Type
                    else -> null
                }
                if (tok != null) out += Span(i, j, tok)
                i = j; continue
            }
            if (ch == '#' && cssLike) {
                var j = i + 1
                while (j < n && (text[j].isLetterOrDigit() || text[j] == '-' || text[j] == '_')) j++
                out += Span(i, j, Tok.Num); i = j; continue
            }
            if (ch in OPS) {
                var j = i + 1
                while (j < n && text[j] in OPS && text[j] != ',' && text[j] != ';') j++
                if (ch != ',' && ch != ';' && ch != '.') out += Span(i, j, Tok.Op)
                i = j; continue
            }
            i++
        }
        return out
    }

    private fun markup(text: String, lang: Language): List<Span> {
        val out = ArrayList<Span>()
        val n = text.length
        var i = 0
        while (i < n) {
            if (text.startsWith("<!--", i)) {
                val end = text.indexOf("-->", i + 4).let { if (it < 0) n else it + 3 }
                out += Span(i, end, Tok.Comment); i = end; continue
            }
            if (text[i] == '<') {
                var j = i + 1
                if (j < n && (text[j] == '/' || text[j] == '?' || text[j] == '!')) j++
                val nameStart = j
                while (j < n && (text[j].isLetterOrDigit() || text[j] in "-_:.")) j++
                if (j == nameStart) { i++; continue }
                out += Span(i, j, Tok.Tag)
                val tagName = text.substring(nameStart, j).lowercase()
                // attributes until '>'
                while (j < n && text[j] != '>') {
                    val c = text[j]
                    when {
                        c == '"' || c == '\'' -> {
                            val e = text.indexOf(c, j + 1).let { if (it < 0) n else it + 1 }
                            out += Span(j, e, Tok.Str); j = e
                        }
                        c.isLetter() -> {
                            val s = j
                            while (j < n && (text[j].isLetterOrDigit() || text[j] in "-_:@.")) j++
                            out += Span(s, j, Tok.Attr)
                        }
                        else -> j++
                    }
                }
                if (j < n) { out += Span(j, j + 1, Tok.Tag); j++ }
                // embedded script/style
                val embedded = when (tagName) { "script" -> Languages.JavaScript; "style" -> Languages.Css; else -> null }
                if (embedded != null && text.getOrNull(nameStart - 1) != '/') {
                    val close = text.indexOf("</$tagName", j, ignoreCase = true).let { if (it < 0) n else it }
                    code(text.substring(j, close), embedded).forEach { out += Span(it.start + j, it.end + j, it.tok) }
                    j = close
                }
                i = j; continue
            }
            if (text[i] == '&') {
                val e = text.indexOf(';', i)
                if (e in (i + 1)..(i + 10)) { out += Span(i, e + 1, Tok.Const); i = e + 1; continue }
            }
            i++
        }
        return out
    }

    private fun markdown(text: String): List<Span> {
        val out = ArrayList<Span>()
        var pos = 0
        var inFence = false
        for (line in text.split('\n')) {
            val start = pos
            val end = pos + line.length
            val t = line.trimStart()
            when {
                t.startsWith("```") -> { out += Span(start, end, Tok.Code); inFence = !inFence }
                inFence -> out += Span(start, end, Tok.Str)
                t.startsWith("#") -> out += Span(start, end, Tok.Heading)
                t.startsWith(">") -> out += Span(start, end, Tok.Comment)
                else -> {
                    if (t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ") || t.firstOrNull()?.isDigit() == true && t.contains(". ")) {
                        val off = line.length - t.length
                        out += Span(start + off, start + off + 1, Tok.Keyword)
                    }
                    Regex("`[^`]+`").findAll(line).forEach { out += Span(start + it.range.first, start + it.range.last + 1, Tok.Code) }
                    Regex("\\*\\*[^*]+\\*\\*|__[^_]+__").findAll(line).forEach { out += Span(start + it.range.first, start + it.range.last + 1, Tok.Bold) }
                    Regex("\\[[^\\]]*]\\([^)]*\\)").findAll(line).forEach { out += Span(start + it.range.first, start + it.range.last + 1, Tok.Func) }
                }
            }
            pos = end + 1
        }
        return out
    }

    private fun String.nextNonSpace(from: Int): Char? {
        var j = from
        while (j < length && (this[j] == ' ' || this[j] == '\t')) j++
        return getOrNull(j)
    }

    private fun String.lastNonSpace(before: Int): Char? {
        var j = before - 1
        while (j >= 0 && (this[j] == ' ' || this[j] == '\t')) j--
        return getOrNull(j)
    }

    /** Index of the bracket matching the one at/just before [cursor], or null. */
    fun matchBracket(text: String, cursor: Int): Pair<Int, Int>? {
        val pairs = mapOf('(' to ')', '[' to ']', '{' to '}')
        val rev = pairs.entries.associate { (k, v) -> v to k }
        for (pos in listOf(cursor - 1, cursor)) {
            val c = text.getOrNull(pos) ?: continue
            pairs[c]?.let { close ->
                var depth = 0
                var j = pos
                val limit = minOf(text.length, pos + 20000)
                while (j < limit) {
                    if (text[j] == c) depth++ else if (text[j] == close) { depth--; if (depth == 0) return pos to j }
                    j++
                }
            }
            rev[c]?.let { open ->
                var depth = 0
                var j = pos
                val limit = maxOf(0, pos - 20000)
                while (j >= limit) {
                    if (text[j] == c) depth++ else if (text[j] == open) { depth--; if (depth == 0) return j to pos }
                    j--
                }
            }
        }
        return null
    }
}
