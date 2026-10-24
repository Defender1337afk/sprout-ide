package com.sprout.ide.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.sprout.ide.data.AppSettings
import com.sprout.ide.lang.Language
import com.sprout.ide.lang.Languages
import java.io.File

class EditorTab(file: File, initial: String) {
    var file by mutableStateOf(file)
    val language: Language get() = Languages.forFile(file.name)

    var value by mutableStateOf(TextFieldValue(initial))
        private set
    var savedText by mutableStateOf(initial)
    val dirty: Boolean get() = value.text != savedText

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    private var lastRecord = 0L

    /** Called from the text field: applies smart-editing rules and records undo history. */
    fun onValueChange(new: TextFieldValue, s: AppSettings) {
        if (new.text == value.text) { value = new; return }
        val processed = SmartEdit.process(value, new, s, language)
        val typedNewline = processed.text.getOrNull(value.selection.min) == '\n' && processed.text.length > value.text.length
        record(processed.text.length - value.text.length !in -1..1 || typedNewline)
        value = processed
    }

    /** Programmatic edit (symbol bar, menu actions): always its own undo step. */
    fun replace(new: TextFieldValue) {
        if (new.text != value.text) record(force = true)
        value = new
    }

    fun select(range: TextRange) { value = value.copy(selection = range) }

    /** Text changed outside the editor (a collaborator saved): keep the cursor position, stay undoable. */
    fun replaceExternal(text: String) {
        if (text == value.text) { savedText = text; return }
        record(force = true)
        value = TextFieldValue(text, TextRange(value.selection.start.coerceAtMost(text.length)))
        savedText = text
    }

    private fun record(force: Boolean) {
        val now = System.currentTimeMillis()
        if (force || now - lastRecord > 900 || undoStack.isEmpty()) {
            undoStack.addLast(value)
            if (undoStack.size > 400) undoStack.removeFirst()
        }
        lastRecord = now
        redoStack.clear()
        sync()
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(value)
        value = prev
        lastRecord = 0
        sync()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(value)
        value = next
        lastRecord = 0
        sync()
    }

    private fun sync() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun reloadFromDisk() {
        val text = runCatching { file.readText() }.getOrDefault("")
        value = TextFieldValue(text)
        savedText = text
        undoStack.clear(); redoStack.clear(); sync()
    }
}
