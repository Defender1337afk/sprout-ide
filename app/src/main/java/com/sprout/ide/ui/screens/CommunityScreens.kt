package com.sprout.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.ui.graphics.Color
import com.sprout.ide.AppViewModel
import com.sprout.ide.Screen
import com.sprout.ide.cloud.Cloud
import com.sprout.ide.cloud.Comment
import com.sprout.ide.cloud.Rating
import com.sprout.ide.cloud.Social
import com.sprout.ide.cloud.License
import com.sprout.ide.cloud.PublicFile
import com.sprout.ide.cloud.PublicProject
import com.sprout.ide.cloud.UserProfile
import com.sprout.ide.lang.Highlighter
import com.sprout.ide.lang.Languages
import com.sprout.ide.lang.SyntaxThemes
import com.sprout.ide.ui.components.FileBadge
import com.sprout.ide.ui.components.ProfileAvatar
import com.sprout.ide.ui.components.ProfileName
import com.sprout.ide.ui.theme.LocalSettings
import com.sprout.ide.ui.theme.family
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CommunityScreen(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var projects by remember(tab) { mutableStateOf<List<PublicProject>?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var people by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tab, reload) {
        if (tab == 2) return@LaunchedEffect
        error = null
        runCatching { Cloud.recentProjects(featuredOnly = tab == 1) }
            .onSuccess { projects = it }.onFailure { error = it.message; projects = emptyList() }
        refreshing = false
    }
    LaunchedEffect(query) {
        delay(300)
        people = runCatching { Cloud.searchUsers(query) }.getOrDefault(emptyList())
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
                    title = { Text("Сообщество", fontWeight = FontWeight.Bold) },
                )
                PrimaryTabRow(selectedTabIndex = tab) {
                    listOf("Новые", "Избранные", "Люди").forEachIndexed { i, t ->
                        Tab(tab == i, { tab = i }, text = { Text(t) })
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (tab == 2) {
                Column {
                    OutlinedTextField(
                        query, { query = it }, placeholder = { Text("Поиск по юзернейму") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = CircleShape,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    )
                    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                        items(people, key = { it.uid }) { p ->
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.navigate(Screen.Profile(p.uid)) }.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProfileAvatar(p, 52.dp)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    ProfileName(p, MaterialTheme.typography.titleMedium)
                                    Text("@${p.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (people.isEmpty() && query.isNotBlank()) item {
                            Text("Никого не нашли", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; reload++ }) {
                    val list = projects
                    when {
                        list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                        list.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                            item {
                                Text(error ?: if (tab == 1) "Избранных проектов пока нет" else "Ещё никто ничего не опубликовал — будьте первым!",
                                    Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(list, key = { it.id }) { p -> PublicProjectCard(p, showAuthor = true) { vm.navigate(Screen.PublicProject(p.id)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicProjectScreen(vm: AppViewModel, id: String) {
    var project by remember { mutableStateOf<PublicProject?>(null) }
    var files by remember { mutableStateOf<List<PublicFile>?>(null) }
    var open by remember { mutableStateOf<PublicFile?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(id) {
        runCatching {
            project = Cloud.project(id)
            files = Cloud.files(id)
        }.onFailure { failed = true }
        if (project == null) failed = true
    }
    val p = project
    val own = p != null && p.ownerId == vm.me?.uid
    var rating by remember { mutableStateOf<Rating?>(null) }
    var myStars by remember { mutableIntStateOf(0) }
    var comments by remember { mutableStateOf<List<Comment>?>(null) }
    var socialVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(id, socialVersion) {
        rating = runCatching { Social.rating(id) }.getOrNull()
        vm.me?.let { me -> myStars = runCatching { Social.myRating(id, me.uid) }.getOrDefault(0) }
        comments = runCatching { Social.comments(id) }.getOrDefault(emptyList())
    }
    fun social(block: suspend () -> Unit) = scope.launch {
        runCatching { block() }.onFailure { snackbar.showSnackbar(it.message ?: "Ошибка") }
        socialVersion++
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { if (open != null) open = null else vm.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
                },
                title = {
                    Column {
                        Text(open?.path?.substringAfterLast('/') ?: p?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        if (p != null) Text("@${p.ownerUsername} · ${p.license}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (own) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Удалить") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val f = open
            when {
                failed && p == null -> Text("Проект не найден", Modifier.align(Alignment.Center))
                p == null || files == null -> LoadingIndicator(Modifier.align(Alignment.Center))
                f != null -> CodeViewer(f)
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                    item {
                        Column(Modifier.padding(20.dp)) {
                            Text(p.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { vm.navigate(Screen.Profile(p.ownerId)) }) {
                                Text("@${p.ownerUsername}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                                    Text("Открытый код · ${p.license}", style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                            }
                            if (p.description.isNotBlank()) {
                                Spacer(Modifier.height(12.dp)); Text(p.description, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (p.tags.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(p.tags.joinToString("  ") { "#$it" }, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    vm.cloneProject(p.title, files!!) { dir ->
                                        scope.launch { snackbar.showSnackbar("Склонировано в «${dir.name}»") }
                                        vm.openProject(dir)
                                    }
                                },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Rounded.CallSplit, null); Spacer(Modifier.width(8.dp)); Text("Клонировать к себе")
                            }
                            Spacer(Modifier.height(16.dp))
                            RatingBlock(rating, myStars, canRate = vm.me != null && !own) { stars ->
                                val me = vm.me ?: return@RatingBlock
                                myStars = stars
                                social { Social.rate(id, me.uid, stars) }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Файлы · ${files!!.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(files!!, key = { it.path }) { file ->
                        Row(
                            Modifier.fillMaxWidth().clickable { open = file }.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FileBadge(Languages.forFile(file.path), 28.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(file.path, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("${file.content.lines().size} стр.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        Text("Комментарии · ${comments?.size ?: 0}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp))
                        val me = vm.me
                        if (me != null) CommentInput { text -> social { Social.addComment(id, me, text) } }
                        else Text("Войдите, чтобы комментировать", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val canModerate = own || vm.me?.role == "admin" || vm.me?.role == "moderator"
                    items(comments.orEmpty(), key = { "c" + it.id }) { c ->
                        CommentRow(c, canDelete = canModerate || c.authorId == vm.me?.uid,
                            onAuthor = { vm.navigate(Screen.Profile(c.authorId)) },
                            onDelete = { social { Social.deleteComment(id, c.id) } })
                    }
                }
            }
        }
    }
    if (confirmDelete && p != null) {
        ConfirmDialog("Снять с публикации?", "Проект «${p.title}» исчезнет из профиля и сообщества. Локальная копия останется.", "Удалить",
            onDismiss = { confirmDelete = false }) {
            confirmDelete = false
            scope.launch {
                runCatching { Cloud.deleteProject(p.id) }
                    .onSuccess { vm.back() }
                    .onFailure { snackbar.showSnackbar(it.message ?: "Ошибка") }
            }
        }
    }
}

@Composable
private fun CodeViewer(f: PublicFile) {
    val s = LocalSettings.current
    val colors = SyntaxThemes.byId(s.syntaxTheme).build(MaterialTheme.colorScheme)
    val text = remember(f, colors) { Highlighter.annotate(f.content, Languages.forFile(f.path), colors) }
    Box(Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
        Text(text, Modifier.padding(14.dp), color = colors.text, fontFamily = s.editorFont.family(), fontSize = s.fontSize.sp,
            lineHeight = (s.fontSize * s.lineHeight).sp, softWrap = false)
    }
}

@Composable
fun PublishDialog(vm: AppViewModel, dir: File, onDismiss: () -> Unit, onDone: (String?) -> Unit) {
    val me = vm.me ?: return
    val existing = vm.publishedId(dir)
    val existingLicense = remember(dir) {
        File(dir, "LICENSE").takeIf { it.exists() }?.readText()?.let { t ->
            License.entries.firstOrNull { t.contains(it.spdx) } ?: if (t.startsWith("MIT License")) License.MIT else null
        }
    }
    var title by remember { mutableStateOf(dir.name) }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var license by remember { mutableStateOf(existingLicense) }
    var agree by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val files = remember(dir) { Cloud.collectFiles(dir) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (existing != null) "Обновить публикацию" else "Опубликовать проект") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        "В Sprout публикуются только проекты с открытым кодом: все ${files.size} текстовых файлов будут доступны всем для просмотра и клонирования.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(title, { title = it.take(60) }, label = { Text("Название") }, singleLine = true,
                    shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(description, { description = it.take(500) }, label = { Text("Описание") }, minLines = 2,
                    shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(tags, { tags = it.take(120) }, label = { Text("Теги через запятую") }, singleLine = true,
                    shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Лицензия (обязательно)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    License.entries.forEach { l ->
                        FilterChip(license == l, { license = l }, label = { Text(l.title) })
                    }
                }
                if (existingLicense != null && license != existingLicense) {
                    Text("В проекте уже есть LICENSE (${existingLicense.title}) — он останется как есть.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(agree, { agree = it })
                    Text("Я автор кода и открываю его под выбранной лицензией", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && agree && license != null && title.isNotBlank(),
                onClick = {
                    busy = true; error = null
                    vm.saveAll()
                    scope.launch {
                        runCatching {
                            Cloud.publish(me, dir, existing, title, description, license!!, tags.split(','))
                        }.onSuccess { id -> vm.setPublishedId(dir, id); onDone(id) }
                            .onFailure { e ->
                                // publication was deleted elsewhere → publish as new next time
                                if (existing != null && e.message?.contains("NOT_FOUND") == true) vm.setPublishedId(dir, null)
                                error = e.message ?: "Ошибка публикации"
                            }
                        busy = false
                    }
                },
            ) { if (busy) LoadingIndicator(Modifier.size(24.dp)) else Text(if (existing != null) "Обновить" else "Опубликовать") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Отмена") } },
    )
}

@Composable
private fun RatingBlock(rating: Rating?, myStars: Int, canRate: Boolean, onRate: (Int) -> Unit) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
                Text(
                    if (rating == null || rating.count == 0L) "—" else "%.1f".format(rating.average),
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black,
                )
                Text(
                    if (rating == null) "" else "${rating.count} ${plural(rating.count.toInt(), "оценка", "оценки", "оценок")}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (canRate) (if (myStars == 0) "Оцените проект" else "Ваша оценка") else "Рейтинг",
                    style = MaterialTheme.typography.labelLarge)
                Row {
                    (1..5).forEach { i ->
                        val filled = if (canRate) i <= myStars else rating != null && i <= Math.round(rating.average)
                        IconButton(onClick = { onRate(if (i == myStars) 0 else i) }, enabled = canRate, modifier = Modifier.size(40.dp)) {
                            Icon(
                                if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder, "$i",
                                tint = if (filled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentInput(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        text, { text = it.take(1000) }, placeholder = { Text("Написать комментарий…") },
        shape = MaterialTheme.shapes.extraLarge, maxLines = 5,
        trailingIcon = {
            IconButton(onClick = { onSend(text); text = "" }, enabled = text.isNotBlank()) {
                Icon(Icons.AutoMirrored.Rounded.Send, "Отправить")
            }
        },
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
    )
}

@Composable
private fun CommentRow(c: Comment, canDelete: Boolean, onAuthor: () -> Unit, onDelete: () -> Unit) {
    var author by remember(c.authorId) { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(c.authorId) { author = runCatching { Cloud.profile(c.authorId) }.getOrNull() }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val a = author
        if (a != null) ProfileAvatar(a, 40.dp, Modifier.clickable(onClick = onAuthor))
        else Box(Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onAuthor)) {
                if (a != null) ProfileName(a, MaterialTheme.typography.titleSmall) else Text("@" + c.authorUsername, fontWeight = FontWeight.Bold)
                c.createdAt?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        android.text.format.DateUtils.getRelativeTimeSpanString(it.toDate().time).toString(),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(top = 4.dp)) {
                Text(c.text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (canDelete) IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, "Удалить", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
