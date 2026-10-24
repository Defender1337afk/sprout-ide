package com.sprout.ide.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprout.ide.AppViewModel
import com.sprout.ide.Screen
import com.sprout.ide.data.AppSettings
import com.sprout.ide.data.SymbolBarStyle
import com.sprout.ide.editor.CodeEditor
import com.sprout.ide.editor.EditorTab
import com.sprout.ide.editor.SmartEdit
import com.sprout.ide.lang.Languages
import com.sprout.ide.lang.SyntaxThemes
import com.sprout.ide.ui.components.FileBadge
import com.sprout.ide.ui.components.ShapeAvatar
import com.sprout.ide.ui.components.languageColors
import com.sprout.ide.ui.components.shape
import com.sprout.ide.ui.theme.JetBrainsMono
import com.sprout.ide.ui.theme.LocalSettings
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File

private sealed interface TreeDialog {
    data class NewFile(val parent: File) : TreeDialog
    data class NewFolder(val parent: File) : TreeDialog
    data class Rename(val file: File) : TreeDialog
    data class Delete(val file: File) : TreeDialog
}

@Composable
fun ProjectScreen(vm: AppViewModel, dir: File) {
    val settings = LocalSettings.current
    val drawer = rememberDrawerState(if (vm.tabs.isEmpty()) DrawerValue.Open else DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val focus = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current
    var treeDialog by remember { mutableStateOf<TreeDialog?>(null) }
    var importInto by remember { mutableStateOf(dir) }
    var gotoLine by remember { mutableStateOf(false) }
    var publishOpen by remember { mutableStateOf(false) }
    var signInPrompt by remember { mutableStateOf(false) }
    var sharingOpen by remember { mutableStateOf(false) }
    var confirmShare by remember { mutableStateOf(false) }
    var sharingBusy by remember { mutableStateOf(false) }

    // search state
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var regex by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }

    val tab = vm.activeTab
    val scheme = MaterialTheme.colorScheme
    val syntax = remember(settings.syntaxTheme, scheme) { SyntaxThemes.byId(settings.syntaxTheme).build(scheme) }

    val matches = remember(tab?.value?.text, query, caseSensitive, regex, searchOpen) {
        val text = tab?.value?.text
        if (!searchOpen || query.isEmpty() || text == null) emptyList()
        else runCatching {
            val r = if (regex) Regex(query, if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
            else Regex(Regex.escape(query), if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
            r.findAll(text).filter { !it.range.isEmpty() }.take(5000).map { it.range }.toList()
        }.getOrDefault(emptyList())
    }

    BackHandler(enabled = drawer.isOpen && vm.tabs.isNotEmpty()) { scope.launch { drawer.close() } }
    BackHandler(enabled = searchOpen && !drawer.isOpen) { searchOpen = false }

    LaunchedEffect(Unit) { vm.events.collect { scope.launch { snackbar.showSnackbar(it) } } }

    // autosave on edits
    LaunchedEffect(tab) {
        val t = tab ?: return@LaunchedEffect
        snapshotFlow { t.value.text }.distinctUntilChanged().collect { if (t.dirty) vm.onEdited() }
    }

    fun tick() { if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    fun jump(forward: Boolean) {
        val t = tab ?: return
        if (matches.isEmpty()) return
        val cur = t.value.selection
        val m = if (forward) matches.firstOrNull { it.first > cur.min } ?: matches.first()
        else matches.lastOrNull { it.first < cur.min } ?: matches.last()
        t.select(TextRange(m.first, m.last + 1))
    }

    fun replaceOne() {
        val t = tab ?: return
        val sel = t.value.selection
        val m = matches.firstOrNull { it.first == sel.min && it.last + 1 == sel.max }
        if (m == null) { jump(true); return }
        val text = t.value.text.replaceRange(m.first, m.last + 1, replacement)
        t.replace(t.value.copy(text = text, selection = TextRange(m.first + replacement.length)))
        jump(true)
    }

    fun replaceAll() {
        val t = tab ?: return
        if (matches.isEmpty()) return
        val sb = StringBuilder(t.value.text)
        matches.asReversed().forEach { sb.replace(it.first, it.last + 1, replacement) }
        t.replace(t.value.copy(text = sb.toString(), selection = TextRange(0)))
        scope.launch { snackbar.showSnackbar("Заменено: ${matches.size}") }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(uri, importInto)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.exportZip(dir, uri) { ok -> scope.launch { snackbar.showSnackbar(if (ok) "Проект экспортирован" else "Ошибка экспорта") } }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = drawer.isOpen || vm.tabs.isEmpty(),
        drawerContent = {
            ModalDrawerSheet(drawerShape = MaterialTheme.shapes.extraLarge.copy(topStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp))) {
                FileTree(
                    vm = vm, root = dir, settings = settings,
                    activeFile = tab?.file,
                    onOpen = { f -> vm.openFile(f); scope.launch { drawer.close() } },
                    onDialog = { treeDialog = it },
                    onImport = { into -> importInto = into; importLauncher.launch(arrayOf("*/*")) },
                    onExport = { exportLauncher.launch("${dir.name}.zip") },
                    published = vm.publishedId(dir) != null,
                    shared = vm.collab != null,
                    onShare = {
                        scope.launch { drawer.close() }
                        when {
                            vm.me == null -> signInPrompt = true
                            vm.collab != null -> { vm.refreshCollabInfo(); sharingOpen = true }
                            else -> confirmShare = true
                        }
                    },
                    onPublish = { scope.launch { drawer.close() }; if (vm.me == null) signInPrompt = true else publishOpen = true },
                    onHome = { vm.back() },
                )
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, "Файлы") }
                    },
                    title = {
                        Column {
                            Text(
                                tab?.file?.name ?: dir.name,
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (tab != null) "${tab.language.name} · ${dir.name}" + (if (vm.collab != null) " · совместный" else "") + if (tab.dirty) " · не сохранено" else ""
                                else "Проект",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            )
                        }
                    },
                    actions = {
                        if (tab != null) {
                            IconButton(onClick = { tab.undo(); tick() }, enabled = tab.canUndo) { Icon(Icons.AutoMirrored.Rounded.Undo, "Отменить") }
                            IconButton(onClick = { tab.redo(); tick() }, enabled = tab.canRedo) { Icon(Icons.AutoMirrored.Rounded.Redo, "Повторить") }
                            IconToggleButton(searchOpen, { searchOpen = it }) { Icon(Icons.Rounded.Search, "Поиск") }
                            val runnable = tab.language.runnable || tab.file.extension.equals("svg", true)
                            if (runnable) {
                                FilledIconButton(
                                    onClick = { vm.saveAll(); vm.navigate(Screen.Preview(tab.file)) },
                                    shapes = IconButtonDefaults.shapes(),
                                ) { Icon(Icons.Rounded.PlayArrow, "Запустить") }
                            }
                            EditorMenu(vm, tab, settings, onGoto = { gotoLine = true }, onCloseTab = { vm.closeTab(vm.activeIndex) })
                        } else {
                            IconButton(onClick = { vm.navigate(Screen.Settings) }) { Icon(Icons.Rounded.Settings, "Настройки") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()
            ) {
                AnimatedVisibility(searchOpen && tab != null, enter = expandVertically(), exit = shrinkVertically()) {
                    SearchPanel(
                        query, { query = it }, replacement, { replacement = it },
                        caseSensitive, { caseSensitive = it }, regex, { regex = it },
                        showReplace, { showReplace = it },
                        count = matches.size,
                        current = tab?.let { t -> matches.indexOfFirst { it.first == t.value.selection.min } + 1 } ?: 0,
                        onPrev = { jump(false) }, onNext = { jump(true) },
                        onReplace = ::replaceOne, onReplaceAll = ::replaceAll,
                        onClose = { searchOpen = false },
                    )
                }
                vm.conflicts.firstOrNull()?.let { c ->
                    ConflictBanner(c.path, c.by, onMine = { vm.resolveConflict(c, keepMine = true) }, onTheirs = { vm.resolveConflict(c, keepMine = false) })
                }
                if (vm.tabs.isNotEmpty()) TabStrip(vm)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (tab != null) {
                        CodeEditor(
                            tab = tab, settings = settings, colors = syntax, matches = matches,
                            focusRequester = focus,
                            onFontSizeCommit = { fs -> vm.updateSettings { it.copy(fontSize = fs) } },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        NoFileState(onOpenDrawer = { scope.launch { drawer.open() } }, onNewFile = { treeDialog = TreeDialog.NewFile(dir) })
                    }
                }
                if (tab != null && settings.symbolBar) {
                    SymbolBar(settings) { sym ->
                        tick()
                        when (sym) {
                            "←" -> tab.select(TextRange((tab.value.selection.min - 1).coerceAtLeast(0)))
                            "→" -> tab.select(TextRange((tab.value.selection.max + 1).coerceAtMost(tab.value.text.length)))
                            else -> tab.replace(SmartEdit.insert(tab.value, sym, settings))
                        }
                    }
                }
            }
        }
    }

    when (val d = treeDialog) {
        is TreeDialog.NewFile -> NewFileDialog(false, { treeDialog = null }) { name, tpl ->
            vm.createFile(d.parent, name, tpl); treeDialog = null; scope.launch { drawer.close() }
        }
        is TreeDialog.NewFolder -> NewFileDialog(true, { treeDialog = null }) { name, _ ->
            vm.createFolder(d.parent, name); treeDialog = null
        }
        is TreeDialog.Rename -> TextInputDialog("Переименовать", d.file.name, "Готово", onDismiss = { treeDialog = null }) {
            vm.rename(d.file, it); treeDialog = null
        }
        is TreeDialog.Delete -> ConfirmDialog(
            "Удалить «${d.file.name}»?",
            if (d.file.isDirectory) "Папка и всё её содержимое будут удалены." else "Файл будет удалён.",
            "Удалить", onDismiss = { treeDialog = null },
        ) { vm.delete(d.file); treeDialog = null }
        null -> Unit
    }
    if (signInPrompt) SignInPrompt(vm) { signInPrompt = false }
    if (sharingOpen) vm.collabInfo?.let { info ->
        ManageCollabDialog(vm, info, localDir = dir, onDismiss = { sharingOpen = false; vm.refreshCollabInfo() })
    }
    if (confirmShare) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!sharingBusy) confirmShare = false },
            icon = { Icon(Icons.Rounded.Groups, null) },
            title = { Text("Сделать проект совместным?") },
            text = { Text("Файлы проекта загрузятся в облако. Приглашённые по @юзернейму участники смогут открыть и редактировать их; изменения приходят всем после сохранения.") },
            confirmButton = {
                androidx.compose.material3.TextButton(enabled = !sharingBusy, onClick = {
                    sharingBusy = true
                    vm.makeShared(dir) { err ->
                        sharingBusy = false
                        confirmShare = false
                        if (err != null) scope.launch { snackbar.showSnackbar(err) } else sharingOpen = true
                    }
                }) { Text(if (sharingBusy) "Загрузка…" else "Сделать совместным") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmShare = false }, enabled = !sharingBusy) { Text("Отмена") } },
        )
    }
    if (publishOpen) {
        PublishDialog(vm, dir, onDismiss = { publishOpen = false }) { id ->
            publishOpen = false
            vm.notifyTreeChanged()
            scope.launch {
                val r = snackbar.showSnackbar("Проект опубликован", actionLabel = "Открыть")
                if (r == androidx.compose.material3.SnackbarResult.ActionPerformed && id != null) vm.navigate(Screen.PublicProject(id))
            }
        }
    }
    if (gotoLine && tab != null) {
        TextInputDialog("Перейти к строке", "", "Перейти", label = "Номер строки", keyboardType = KeyboardType.Number,
            onDismiss = { gotoLine = false }) { s ->
            s.toIntOrNull()?.let { tab.select(TextRange(SmartEdit.offsetOfLine(tab.value.text, it))) }
            gotoLine = false
            focus.requestFocus()
        }
    }
}

@Composable
private fun EditorMenu(vm: AppViewModel, tab: EditorTab, settings: AppSettings, onGoto: () -> Unit, onCloseTab: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, "Ещё") }
        DropdownMenu(open, { open = false }, shape = MaterialTheme.shapes.large) {
            @Composable
            fun item(text: String, icon: ImageVector, enabled: Boolean = true, action: () -> Unit) = DropdownMenuItem(
                { Text(text) }, { open = false; action() }, leadingIcon = { Icon(icon, null) }, enabled = enabled,
            )
            item("Сохранить", Icons.Rounded.Save, tab.dirty) { vm.save(tab) }
            item("Перейти к строке", Icons.Rounded.FormatListNumbered, action = onGoto)
            HorizontalDivider()
            item("Закомментировать", Icons.Rounded.Code) { tab.replace(SmartEdit.toggleComment(tab.value, tab.language)) }
            item("Дублировать строку", Icons.Rounded.ContentCopy) { tab.replace(SmartEdit.duplicateLine(tab.value)) }
            item("Удалить строку", Icons.Rounded.Delete) { tab.replace(SmartEdit.deleteLine(tab.value)) }
            item("Строку вверх", Icons.Rounded.KeyboardArrowUp) { tab.replace(SmartEdit.moveLine(tab.value, true)) }
            item("Строку вниз", Icons.Rounded.KeyboardArrowDown) { tab.replace(SmartEdit.moveLine(tab.value, false)) }
            item("Увеличить отступ", Icons.AutoMirrored.Rounded.FormatIndentIncrease) { tab.replace(SmartEdit.indent(tab.value, settings)) }
            item("Уменьшить отступ", Icons.AutoMirrored.Rounded.FormatIndentDecrease) { tab.replace(SmartEdit.outdent(tab.value, settings)) }
            item("Выделить строку", Icons.Rounded.SelectAll) { tab.replace(SmartEdit.selectLine(tab.value)) }
            HorizontalDivider()
            item(if (settings.wordWrap) "Без переноса строк" else "Перенос строк", Icons.AutoMirrored.Rounded.WrapText) {
                vm.updateSettings { it.copy(wordWrap = !it.wordWrap) }
            }
            item(if (settings.lineNumbers) "Скрыть номера строк" else "Номера строк", Icons.Rounded.FormatListNumbered) {
                vm.updateSettings { it.copy(lineNumbers = !it.lineNumbers) }
            }
            item("Перезагрузить с диска", Icons.Rounded.Refresh) { tab.reloadFromDisk() }
            item("Закрыть вкладку", Icons.Rounded.Close, action = onCloseTab)
            item("Настройки", Icons.Rounded.Settings) { vm.navigate(Screen.Settings) }
        }
    }
}

@Composable
private fun TabStrip(vm: AppViewModel) {
    val state = rememberLazyListState()
    LaunchedEffect(vm.activeIndex) { if (vm.activeIndex >= 0) state.animateScrollToItem(vm.activeIndex) }
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        itemsIndexed(vm.tabs, key = { _, t -> t.file.path }) { i, t ->
            val selected = i == vm.activeIndex
            var menu by remember { mutableStateOf(false) }
            Box(Modifier.animateItem()) {
                Surface(
                    shape = if (selected) MaterialTheme.shapes.medium else CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clip(if (selected) MaterialTheme.shapes.medium else CircleShape)
                        .combinedClickable(onClick = { vm.activeIndex = i }, onLongClick = { menu = true }),
                ) {
                    Row(Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        FileBadge(t.language, 20.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(t.file.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        if (t.dirty) {
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                        IconButton(onClick = { vm.closeTab(i) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, "Закрыть", Modifier.size(16.dp))
                        }
                    }
                }
                DropdownMenu(menu, { menu = false }, shape = MaterialTheme.shapes.large) {
                    DropdownMenuItem({ Text("Закрыть") }, { menu = false; vm.closeTab(i) })
                    DropdownMenuItem({ Text("Закрыть остальные") }, { menu = false; vm.closeOthers(i) })
                    DropdownMenuItem({ Text("Сохранить") }, { menu = false; vm.save(t) }, enabled = t.dirty)
                }
            }
        }
    }
}

@Composable
private fun SymbolBar(settings: AppSettings, onSymbol: (String) -> Unit) {
    val symbols = remember(settings.symbols) { settings.symbols.split(' ').filter { it.isNotEmpty() } }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(if (settings.symbolBarStyle == SymbolBarStyle.Flat) 0.dp else 6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(symbols) { s ->
                val shape = when (settings.symbolBarStyle) {
                    SymbolBarStyle.Chips -> MaterialTheme.shapes.medium
                    SymbolBarStyle.Pills -> CircleShape
                    SymbolBarStyle.Flat -> MaterialTheme.shapes.small
                }
                Surface(
                    onClick = { onSymbol(s) },
                    shape = shape,
                    color = when (settings.symbolBarStyle) {
                        SymbolBarStyle.Flat -> MaterialTheme.colorScheme.surfaceContainer
                        SymbolBarStyle.Pills -> MaterialTheme.colorScheme.secondaryContainer
                        SymbolBarStyle.Chips -> MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.height(40.dp),
                ) {
                    Box(Modifier.padding(horizontal = if (s.length > 1) 12.dp else 14.dp), contentAlignment = Alignment.Center) {
                        Text(s, fontFamily = JetBrainsMono, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPanel(
    query: String, onQuery: (String) -> Unit,
    replacement: String, onReplacement: (String) -> Unit,
    caseSensitive: Boolean, onCase: (Boolean) -> Unit,
    regex: Boolean, onRegex: (Boolean) -> Unit,
    showReplace: Boolean, onShowReplace: (Boolean) -> Unit,
    count: Int, current: Int,
    onPrev: () -> Unit, onNext: () -> Unit, onReplace: () -> Unit, onReplaceAll: () -> Unit, onClose: () -> Unit,
) {
    val fieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
    )
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onShowReplace(!showReplace) }) {
                    Icon(Icons.Rounded.ExpandMore, "Замена", Modifier.rotate(if (showReplace) 180f else 0f))
                }
                TextField(
                    query, onQuery, singleLine = true, placeholder = { Text("Найти") },
                    shape = CircleShape, colors = fieldColors,
                    supportingText = null,
                    trailingIcon = { Text(if (query.isEmpty()) "" else "$current/$count", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.weight(1f).height(52.dp),
                )
                IconButton(onClick = onPrev) { Icon(Icons.Rounded.KeyboardArrowUp, "Назад") }
                IconButton(onClick = onNext) { Icon(Icons.Rounded.KeyboardArrowDown, "Далее") }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Закрыть") }
            }
            Row(Modifier.padding(start = 48.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.FilterChip(caseSensitive, { onCase(!caseSensitive) }, label = { Text("Aa") })
                androidx.compose.material3.FilterChip(regex, { onRegex(!regex) }, label = { Text(".*") })
            }
            if (showReplace) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)) {
                    TextField(
                        replacement, onReplacement, singleLine = true, placeholder = { Text("Заменить на") },
                        shape = CircleShape, colors = fieldColors, modifier = Modifier.weight(1f).height(52.dp),
                    )
                    FilledTonalIconButton(onClick = onReplace) { Icon(Icons.Rounded.SwapVert, "Заменить") }
                    TextButton(onClick = onReplaceAll) { Text("Все") }
                }
            }
        }
    }
}

@Composable
private fun NoFileState(onOpenDrawer: () -> Unit, onNewFile: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        ShapeAvatar(">_", LocalSettings.current.projectShape.shape(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, 120.dp)
        Spacer(Modifier.height(20.dp))
        Text("Нет открытых файлов", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Файлы") }
            androidx.compose.material3.Button(onClick = onNewFile) { Icon(Icons.Rounded.NoteAdd, null); Spacer(Modifier.width(8.dp)); Text("Новый файл") }
        }
    }
}

private class TreeNode(val file: File, val depth: Int)

@Composable
private fun FileTree(
    vm: AppViewModel,
    root: File,
    settings: AppSettings,
    activeFile: File?,
    onOpen: (File) -> Unit,
    onDialog: (TreeDialog) -> Unit,
    onImport: (File) -> Unit,
    onExport: () -> Unit,
    published: Boolean,
    onPublish: () -> Unit,
    shared: Boolean,
    onShare: () -> Unit,
    onHome: () -> Unit,
) {
    var expanded by remember(root) { mutableStateOf(setOf<String>()) }
    val nodes = remember(vm.treeVersion, expanded, settings.showHidden, settings.fileSort, settings.foldersFirst) {
        val out = ArrayList<TreeNode>()
        fun walk(dir: File, depth: Int) {
            vm.workspace.list(dir, settings).forEach { f ->
                out += TreeNode(f, depth)
                if (f.isDirectory && f.path in expanded) walk(f, depth + 1)
            }
        }
        walk(root, 0)
        out
    }
    val lang = remember(root, vm.treeVersion) {
        vm.workspace.projects().firstOrNull { it.dir == root }?.mainLanguage?.let { id -> Languages.all.firstOrNull { it.id == id } } ?: Languages.Plain
    }
    val (bg, fg) = languageColors(lang)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapeAvatar(root.name.take(1).uppercase(), settings.projectShape.shape(), bg, fg, 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(root.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(lang.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onHome) { Icon(Icons.Rounded.Home, "К проектам") }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalIconButton(onClick = { onDialog(TreeDialog.NewFile(root)) }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.NoteAdd, "Новый файл") }
            FilledTonalIconButton(onClick = { onDialog(TreeDialog.NewFolder(root)) }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.CreateNewFolder, "Новая папка") }
            FilledTonalIconButton(onClick = { onImport(root) }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Upload, "Импорт") }
            FilledTonalIconButton(onClick = onExport, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Archive, "Экспорт ZIP") }
            FilledTonalIconButton(onClick = { expanded = emptySet() }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Refresh, "Свернуть всё") }
        }
        androidx.compose.material3.Button(
            onClick = onPublish,
            shapes = androidx.compose.material3.ButtonDefaults.shapes(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Public, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (published) "Обновить публикацию" else "Опубликовать (open source)")
        }
        androidx.compose.material3.FilledTonalButton(
            onClick = onShare,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Groups, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (shared) "Совместная работа · участники" else "Пригласить соавторов")
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
            items(nodes, key = { it.file.path }) { node ->
                val f = node.file
                val isDir = f.isDirectory
                val open = f.path in expanded
                var menu by remember { mutableStateOf(false) }
                val selected = f == activeFile
                Box(Modifier.animateItem()) {
                    Surface(
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier.fillMaxWidth().clip(CircleShape).combinedClickable(
                            onClick = {
                                if (isDir) expanded = if (open) expanded - f.path else expanded + f.path
                                else onOpen(f)
                            },
                            onLongClick = { menu = true },
                        ),
                    ) {
                        Row(
                            Modifier.padding(start = 12.dp + 18.dp * node.depth, end = 4.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isDir) {
                                Icon(
                                    if (open) Icons.Rounded.FolderOpen else Icons.Rounded.Folder, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp),
                                )
                            } else {
                                FileBadge(Languages.forFile(f.name), 24.dp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                f.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.MoreVert, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    DropdownMenu(menu, { menu = false }, shape = MaterialTheme.shapes.large) {
                        if (isDir) {
                            DropdownMenuItem({ Text("Новый файл") }, { menu = false; onDialog(TreeDialog.NewFile(f)) }, leadingIcon = { Icon(Icons.Rounded.NoteAdd, null) })
                            DropdownMenuItem({ Text("Новая папка") }, { menu = false; onDialog(TreeDialog.NewFolder(f)) }, leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null) })
                            DropdownMenuItem({ Text("Импортировать сюда") }, { menu = false; onImport(f) }, leadingIcon = { Icon(Icons.Rounded.Upload, null) })
                        } else {
                            DropdownMenuItem({ Text("Открыть") }, { menu = false; onOpen(f) }, leadingIcon = { Icon(Icons.Rounded.FileOpen, null) })
                        }
                        DropdownMenuItem({ Text("Переименовать") }, { menu = false; onDialog(TreeDialog.Rename(f)) }, leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) })
                        DropdownMenuItem({ Text("Дублировать") }, { menu = false; vm.duplicate(f) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) })
                        DropdownMenuItem(
                            { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                            { menu = false; if (settings.confirmDelete) onDialog(TreeDialog.Delete(f)) else vm.delete(f) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictBanner(path: String, by: String, onMine: () -> Unit, onTheirs: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Конфликт в $path", fontWeight = FontWeight.Bold)
            Text("@$by сохранил свою версию, пока вы редактировали. Чью оставить?", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                androidx.compose.material3.Button(onClick = onMine) { Text("Мою") }
                androidx.compose.material3.OutlinedButton(onClick = onTheirs) { Text("@$by") }
            }
        }
    }
}
