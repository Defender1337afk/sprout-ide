package com.sprout.ide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sprout.ide.data.ProjectTemplate
import com.sprout.ide.lang.Language
import com.sprout.ide.lang.Languages
import com.sprout.ide.ui.components.FileBadge

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirm: String,
    label: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val dot = initial.lastIndexOf('.').takeIf { it > 0 } ?: initial.length
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, dot))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value, { value = it },
                label = label?.let { { Text(it) } },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (value.text.isNotBlank()) onConfirm(value.text.trim()) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.text.trim()) }, enabled = value.text.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
fun NewProjectDialog(initial: ProjectTemplate, onDismiss: () -> Unit, onCreate: (String, ProjectTemplate) -> Unit) {
    var name by remember { mutableStateOf(if (initial == ProjectTemplate.Empty) "Мой проект" else initial.title) }
    var template by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    name, { name = it }, label = { Text("Название") }, singleLine = true,
                    shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Шаблон", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectTemplate.entries.forEach { t ->
                        val lang = Languages.all.first { it.id == t.languageId }
                        FilterChip(
                            selected = template == t,
                            onClick = { template = t },
                            label = { Text(t.title) },
                            leadingIcon = { FileBadge(lang, 20.dp) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(template.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name, template) }, enabled = name.isNotBlank()) { Text("Создать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
fun NewFileDialog(folderMode: Boolean, onDismiss: () -> Unit, onCreate: (String, Boolean) -> Unit) {
    var lang by remember { mutableStateOf<Language?>(if (folderMode) null else Languages.Python) }
    var value by remember {
        val init = if (folderMode) "folder" else "main.py"
        mutableStateOf(TextFieldValue(init, TextRange(0, init.substringBeforeLast('.').length)))
    }
    var withTemplate by remember { mutableStateOf(true) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folderMode) "Новая папка" else "Новый файл") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value,
                    { v ->
                        value = v
                        if (!folderMode) lang = Languages.forFile(v.text)
                    },
                    label = { Text("Имя") }, singleLine = true, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                if (!folderMode) {
                    Spacer(Modifier.height(14.dp))
                    Text("Язык", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Languages.all.forEach { l ->
                            FilterChip(
                                selected = lang == l,
                                onClick = {
                                    lang = l
                                    val base = value.text.substringBeforeLast('.').ifBlank { "main" }
                                    val text = "$base.${l.defaultExt}"
                                    value = TextFieldValue(text, TextRange(0, base.length))
                                },
                                label = { Text(l.name) },
                                leadingIcon = { FileBadge(l, 18.dp) },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(withTemplate, { withTemplate = it })
                        Text("Вставить шаблон «Hello, World»")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(value.text.trim(), withTemplate) }, enabled = value.text.isNotBlank()) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
