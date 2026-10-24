package com.sprout.ide.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val title: String) { System("Система"), Light("Светлая"), Dark("Тёмная") }

enum class PaletteStyle(val title: String) {
    Tonal("Тональная"), Vibrant("Яркая"), Expressive("Экспрессивная"), Fidelity("Точная"), Monochrome("Монохром")
}

enum class ContrastLevel(val title: String) { Standard("Обычный"), Medium("Средний"), High("Высокий") }

enum class EditorFont(val title: String) {
    JetBrainsMono("JetBrains Mono"), FiraCode("Fira Code"), SourceCodePro("Source Code Pro"), System("Системный моно")
}

enum class FileSort(val title: String) { Name("Имя"), Type("Тип"), Modified("Дата") }

enum class ProjectShape(val title: String) {
    Cookie9("Печенька 9"), Cookie12("Печенька 12"), Cookie4("Клевер"), Sunny("Солнце"), Flower("Цветок"),
    Pill("Пилюля"), Gem("Кристалл"), Ghost("Призрак"), Pentagon("Пятиугольник"), Circle("Круг"), Square("Квадрат")
}

enum class SymbolBarStyle(val title: String) { Chips("Чипы"), Flat("Плоский"), Pills("Капсулы") }

data class AppSettings(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.System,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = false,
    val seedColor: Long = 0xFF2E8B45,
    val paletteStyle: PaletteStyle = PaletteStyle.Tonal,
    val contrast: ContrastLevel = ContrastLevel.Standard,
    val cornerScale: Float = 1f,
    val projectShape: ProjectShape = ProjectShape.Cookie9,
    val expressiveMotion: Boolean = true,
    val haptics: Boolean = true,
    val uiScale: Float = 1f,
    // Editor
    val editorFont: EditorFont = EditorFont.JetBrainsMono,
    val fontSize: Float = 14f,
    val lineHeight: Float = 1.45f,
    val ligatures: Boolean = true,
    val syntaxTheme: String = "material",
    val lineNumbers: Boolean = true,
    val highlightLine: Boolean = true,
    val wordWrap: Boolean = false,
    val tabSize: Int = 4,
    val useSpaces: Boolean = true,
    val autoClose: Boolean = true,
    val autoIndent: Boolean = true,
    val autoSave: Boolean = true,
    val pinchZoom: Boolean = true,
    val showWhitespace: Boolean = false,
    val bracketHighlight: Boolean = true,
    // Symbol bar
    val symbolBar: Boolean = true,
    val symbolBarStyle: SymbolBarStyle = SymbolBarStyle.Chips,
    val symbols: String = DEFAULT_SYMBOLS,
    // Files
    val showHidden: Boolean = false,
    val fileSort: FileSort = FileSort.Name,
    val foldersFirst: Boolean = true,
    val confirmDelete: Boolean = true,
    val restoreTabs: Boolean = true,
) {
    val indentUnit: String get() = if (useSpaces) " ".repeat(tabSize) else "\t"

    companion object {
        const val DEFAULT_SYMBOLS = "⇥ ← → { } ( ) [ ] < > ; : = \" ' / \\ | & ! ? + - * % # _ $ @ ^ ~ `"
    }
}

/** SharedPreferences-backed settings so the theme is known synchronously at startup (no flash). */
class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        _state.value = next
        save(next)
    }

    fun reset() {
        prefs.edit().clear().apply()
        _state.value = AppSettings()
    }

    private inline fun <reified E : Enum<E>> SharedPreferences.enum(key: String, def: E): E =
        getString(key, null)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: def

    private fun load(): AppSettings = with(prefs) {
        val d = AppSettings()
        AppSettings(
            themeMode = enum("themeMode", d.themeMode),
            amoled = getBoolean("amoled", d.amoled),
            dynamicColor = getBoolean("dynamicColor", d.dynamicColor),
            seedColor = getLong("seedColor", d.seedColor),
            paletteStyle = enum("paletteStyle", d.paletteStyle),
            contrast = enum("contrast", d.contrast),
            cornerScale = getFloat("cornerScale", d.cornerScale),
            projectShape = enum("projectShape", d.projectShape),
            expressiveMotion = getBoolean("expressiveMotion", d.expressiveMotion),
            haptics = getBoolean("haptics", d.haptics),
            uiScale = getFloat("uiScale", d.uiScale),
            editorFont = enum("editorFont", d.editorFont),
            fontSize = getFloat("fontSize", d.fontSize),
            lineHeight = getFloat("lineHeight", d.lineHeight),
            ligatures = getBoolean("ligatures", d.ligatures),
            syntaxTheme = getString("syntaxTheme", d.syntaxTheme) ?: d.syntaxTheme,
            lineNumbers = getBoolean("lineNumbers", d.lineNumbers),
            highlightLine = getBoolean("highlightLine", d.highlightLine),
            wordWrap = getBoolean("wordWrap", d.wordWrap),
            tabSize = getInt("tabSize", d.tabSize),
            useSpaces = getBoolean("useSpaces", d.useSpaces),
            autoClose = getBoolean("autoClose", d.autoClose),
            autoIndent = getBoolean("autoIndent", d.autoIndent),
            autoSave = getBoolean("autoSave", d.autoSave),
            pinchZoom = getBoolean("pinchZoom", d.pinchZoom),
            showWhitespace = getBoolean("showWhitespace", d.showWhitespace),
            bracketHighlight = getBoolean("bracketHighlight", d.bracketHighlight),
            symbolBar = getBoolean("symbolBar", d.symbolBar),
            symbolBarStyle = enum("symbolBarStyle", d.symbolBarStyle),
            symbols = getString("symbols", d.symbols) ?: d.symbols,
            showHidden = getBoolean("showHidden", d.showHidden),
            fileSort = enum("fileSort", d.fileSort),
            foldersFirst = getBoolean("foldersFirst", d.foldersFirst),
            confirmDelete = getBoolean("confirmDelete", d.confirmDelete),
            restoreTabs = getBoolean("restoreTabs", d.restoreTabs),
        )
    }

    private fun save(s: AppSettings) = prefs.edit().apply {
        putString("themeMode", s.themeMode.name)
        putBoolean("amoled", s.amoled)
        putBoolean("dynamicColor", s.dynamicColor)
        putLong("seedColor", s.seedColor)
        putString("paletteStyle", s.paletteStyle.name)
        putString("contrast", s.contrast.name)
        putFloat("cornerScale", s.cornerScale)
        putString("projectShape", s.projectShape.name)
        putBoolean("expressiveMotion", s.expressiveMotion)
        putBoolean("haptics", s.haptics)
        putFloat("uiScale", s.uiScale)
        putString("editorFont", s.editorFont.name)
        putFloat("fontSize", s.fontSize)
        putFloat("lineHeight", s.lineHeight)
        putBoolean("ligatures", s.ligatures)
        putString("syntaxTheme", s.syntaxTheme)
        putBoolean("lineNumbers", s.lineNumbers)
        putBoolean("highlightLine", s.highlightLine)
        putBoolean("wordWrap", s.wordWrap)
        putInt("tabSize", s.tabSize)
        putBoolean("useSpaces", s.useSpaces)
        putBoolean("autoClose", s.autoClose)
        putBoolean("autoIndent", s.autoIndent)
        putBoolean("autoSave", s.autoSave)
        putBoolean("pinchZoom", s.pinchZoom)
        putBoolean("showWhitespace", s.showWhitespace)
        putBoolean("bracketHighlight", s.bracketHighlight)
        putBoolean("symbolBar", s.symbolBar)
        putString("symbolBarStyle", s.symbolBarStyle.name)
        putString("symbols", s.symbols)
        putBoolean("showHidden", s.showHidden)
        putString("fileSort", s.fileSort.name)
        putBoolean("foldersFirst", s.foldersFirst)
        putBoolean("confirmDelete", s.confirmDelete)
        putBoolean("restoreTabs", s.restoreTabs)
    }.apply()
}
