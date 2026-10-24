package com.sprout.ide.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sprout.ide.AppViewModel
import com.sprout.ide.R
import com.sprout.ide.Screen
import com.sprout.ide.data.ProjectInfo
import com.sprout.ide.data.ProjectTemplate
import com.sprout.ide.lang.Languages
import com.sprout.ide.ui.components.FileBadge
import com.sprout.ide.ui.components.ProfileAvatar
import com.sprout.ide.ui.components.ShapeAvatar
import com.sprout.ide.ui.components.languageColors
import com.sprout.ide.ui.components.shape
import com.sprout.ide.ui.theme.LocalSettings
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(vm: AppViewModel) {
    val settings = LocalSettings.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var fabExpanded by remember { mutableStateOf(false) }
    var createWith by remember { mutableStateOf<ProjectTemplate?>(null) }
    var renaming by remember { mutableStateOf<ProjectInfo?>(null) }
    var deleting by remember { mutableStateOf<ProjectInfo?>(null) }
    var exporting by remember { mutableStateOf<ProjectInfo?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var signInPrompt by remember { mutableStateOf(false) }

    val importZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importProject(uri) { name ->
                scope.launch { snackbar.showSnackbar(if (name != null) "Импортировано: $name" else "Не удалось импортировать") }
            }
        }
    }
    val exportZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val p = exporting
        if (uri != null && p != null) vm.exportZip(p.dir, uri) { ok ->
            scope.launch { snackbar.showSnackbar(if (ok) "Экспортировано: ${p.name}.zip" else "Не удалось экспортировать") }
        }
        exporting = null
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Sprout IDE", fontWeight = FontWeight.Bold) },
                subtitle = {
                    Text(
                        when (val n = vm.projects.size) {
                            0 -> "Нет проектов"
                            else -> "$n ${plural(n, "проект", "проекта", "проектов")}"
                        }
                    )
                },
                navigationIcon = {
                    Image(
                        painterResource(R.drawable.ic_logo), null,
                        Modifier.padding(start = 8.dp, end = 4.dp).size(40.dp),
                    )
                },
                actions = {
                    IconButton(onClick = { if (vm.isGuest) signInPrompt = true else vm.navigate(Screen.Collabs) }) {
                        Icon(Icons.Rounded.Groups, "Совместные проекты")
                    }
                    IconButton(onClick = { if (vm.isGuest) signInPrompt = true else vm.navigate(Screen.Community) }) {
                        Icon(Icons.Rounded.Public, "Сообщество")
                    }
                    IconButton(onClick = { vm.navigate(Screen.Settings) }) { Icon(Icons.Rounded.Settings, "Настройки") }
                    val me = vm.me
                    if (me != null) {
                        ProfileAvatar(me, 40.dp, Modifier.padding(end = 8.dp).clip(CircleShape).clickable { vm.navigate(Screen.Profile(me.uid)) })
                    } else if (vm.isGuest) {
                        FilledTonalButton(onClick = { vm.showOnboarding() }, modifier = Modifier.padding(end = 8.dp)) { Text("Войти") }
                    }
                },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabExpanded,
                button = {
                    ToggleFloatingActionButton(checked = fabExpanded, onCheckedChange = { fabExpanded = it }) {
                        val icon = if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Add
                        Icon(rememberVectorPainter(icon), null, Modifier.animateIcon({ checkedProgress }))
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = { fabExpanded = false; createWith = ProjectTemplate.Empty },
                    icon = { Icon(Icons.Rounded.CreateNewFolder, null) },
                    text = { Text("Новый проект") },
                )
                FloatingActionButtonMenuItem(
                    onClick = { fabExpanded = false; createWith = ProjectTemplate.Web },
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("Веб-сайт") },
                )
                FloatingActionButtonMenuItem(
                    onClick = { fabExpanded = false; importZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                    icon = { Icon(Icons.Rounded.Unarchive, null) },
                    text = { Text("Импорт ZIP / файла") },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    "Быстрый старт",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ProjectTemplate.entries.filter { it != ProjectTemplate.Empty }) { t ->
                        TemplateCard(t) { createWith = t }
                    }
                }
            }
            item {
                Text(
                    "Проекты",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            when {
                vm.loadingProjects && vm.projects.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                }
                vm.projects.isEmpty() -> item { EmptyState { createWith = ProjectTemplate.Empty } }
                else -> items(vm.projects, key = { it.dir.path }) { p ->
                    ProjectCard(
                        p, settings.projectShape.shape(),
                        onOpen = { vm.openProject(p.dir) },
                        onRename = { renaming = p },
                        onDelete = { if (settings.confirmDelete) deleting = p else vm.deleteProject(p) },
                        onExport = { exporting = p; exportZip.launch("${p.name}.zip") },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (signInPrompt) SignInPrompt(vm) { signInPrompt = false }
    createWith?.let { t ->
        NewProjectDialog(initial = t, onDismiss = { createWith = null }) { name, template ->
            createWith = null
            vm.createProject(name, template)
        }
    }
    renaming?.let { p ->
        TextInputDialog("Переименовать проект", p.name, "Переименовать", onDismiss = { renaming = null }) {
            vm.renameProject(p, it); renaming = null
        }
    }
    deleting?.let { p ->
        ConfirmDialog("Удалить «${p.name}»?", "Все файлы проекта будут удалены без возможности восстановления.", "Удалить",
            onDismiss = { deleting = null }) { vm.deleteProject(p); deleting = null }
    }
}

@Composable
private fun TemplateCard(t: ProjectTemplate, onClick: () -> Unit) {
    val lang = Languages.all.first { it.id == t.languageId }
    val (bg, fg) = languageColors(lang)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
        modifier = Modifier.width(132.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            FileBadge(lang, 36.dp)
            Spacer(Modifier.height(18.dp))
            Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(t.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProjectCard(
    p: ProjectInfo,
    shape: androidx.compose.ui.graphics.Shape,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lang = Languages.all.firstOrNull { it.id == p.mainLanguage } ?: Languages.Plain
    val (bg, fg) = languageColors(lang)
    var menu by remember { mutableStateOf(false) }
    ElevatedCard(
        onClick = onOpen,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapeAvatar(p.name.take(1).uppercase(), shape, bg, fg, 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (p.shared) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Groups, "Совместный", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(
                    "${lang.name} · ${p.files} ${plural(p.files, "файл", "файла", "файлов")} · " +
                        DateUtils.getRelativeTimeSpanString(p.modified, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Меню") }
                DropdownMenu(menu, { menu = false }, shape = MaterialTheme.shapes.large) {
                    DropdownMenuItem({ Text("Переименовать") }, { menu = false; onRename() }, leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) })
                    DropdownMenuItem({ Text("Экспорт в ZIP") }, { menu = false; onExport() }, leadingIcon = { Icon(Icons.Rounded.Archive, null) })
                    DropdownMenuItem(
                        { Text("Удалить", color = MaterialTheme.colorScheme.error) }, { menu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(140.dp).clip(MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_logo), null, Modifier.size(170.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Пока пусто", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Создайте проект из шаблона выше или нажмите «+», чтобы начать писать код.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onCreate) { Text("Создать проект") }
    }
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> one
        m10 in 2..4 && m100 !in 12..14 -> few
        else -> many
    }
}

/** Shown when a guest taps something that needs an account. */
@Composable
fun SignInPrompt(vm: AppViewModel, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Public, null) },
        title = { Text("Нужен аккаунт") },
        text = { Text("Профиль, сообщество и публикация проектов доступны после входа через Google. Как гость вы можете создавать и редактировать файлы.") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onDismiss(); vm.showOnboarding() }) { Text("Войти") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Позже") } },
    )
}
