package com.sprout.ide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
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
import com.sprout.ide.AppViewModel
import com.sprout.ide.cloud.Collab
import com.sprout.ide.cloud.Collabs
import com.sprout.ide.cloud.UserProfile
import com.sprout.ide.ui.components.ShapeAvatar
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CollabsScreen(vm: AppViewModel) {
    val me = vm.me ?: return
    var invites by remember { mutableStateOf<List<Collab>?>(null) }
    var mine by remember { mutableStateOf<List<Collab>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    var managing by remember { mutableStateOf<Collab?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        invites = runCatching { Collabs.invites(me.uid) }.getOrDefault(emptyList())
        mine = runCatching { Collabs.mine(me.uid) }.getOrDefault(emptyList())
    }
    fun open(c: Collab, accept: Boolean) {
        busy = c.id
        vm.openCollab(c, accept) { err ->
            busy = null
            if (err != null) scope.launch { snackbar.showSnackbar(err) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
                title = {
                    Column {
                        Text("Совместные проекты", fontWeight = FontWeight.Bold)
                        Text("Пишите один проект вместе", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 40.dp)) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Groups, null, Modifier.size(32.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Чтобы позвать соавторов, откройте свой проект → боковая панель → «Совместная работа». " +
                                "Изменения участников приходят сразу после сохранения.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            val inv = invites
            if (!inv.isNullOrEmpty()) {
                item { SectionTitle("Приглашения") }
                items(inv, key = { "i" + it.id }) { c ->
                    CollabCard(c, me) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { open(c, accept = true) }, enabled = busy == null, shapes = ButtonDefaults.shapes()) {
                                if (busy == c.id) LoadingIndicator(Modifier.size(20.dp)) else Text("Принять")
                            }
                            OutlinedButton(onClick = {
                                scope.launch { runCatching { Collabs.decline(c, me.uid) }; reload++ }
                            }) { Text("Отклонить") }
                        }
                    }
                }
            }
            item { SectionTitle("Мои совместные проекты") }
            val list = mine
            when {
                list == null -> item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
                list.isEmpty() -> item {
                    Text("Пока нет. Создайте проект и пригласите друзей по @юзернейму.",
                        Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> items(list, key = { it.id }) { c ->
                    CollabCard(c, me) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { open(c, accept = false) }, enabled = busy == null, shapes = ButtonDefaults.shapes()) {
                                if (busy == c.id) LoadingIndicator(Modifier.size(20.dp)) else Text("Открыть")
                            }
                            FilledTonalButton(onClick = { managing = c }) { Text("Участники") }
                        }
                    }
                }
            }
        }
    }
    managing?.let { c ->
        ManageCollabDialog(vm, c, localDir = null, onDismiss = { managing = null; reload++ })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
}

@Composable
private fun CollabCard(c: Collab, me: UserProfile, actions: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeAvatar(c.title.take(1).uppercase(), MaterialShapes.Cookie9Sided.toShape(),
                    MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "владелец @${c.name(c.ownerId)}" + if (c.ownerId == me.uid) " (вы)" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        c.members.joinToString("  ") { "@" + c.name(it) },
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            actions()
        }
    }
}

/** Members, invitations, leave / stop sharing. [localDir] is the open project folder, when called from the editor. */
@Composable
fun ManageCollabDialog(vm: AppViewModel, initial: Collab, localDir: File?, onDismiss: () -> Unit) {
    val me = vm.me ?: return
    var c by remember { mutableStateOf(initial) }
    var username by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) } // "leave" | "delete"
    val scope = rememberCoroutineScope()
    val owner = c.ownerId == me.uid

    fun refresh() = scope.launch { Collabs.get(c.id)?.let { c = it } }
    fun run(block: suspend () -> Unit) {
        busy = true; error = null
        scope.launch {
            runCatching { block() }.onFailure { error = it.message }
            busy = false
            refresh()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Groups, null) },
        title = { Text(c.title) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text("Участники · ${c.members.size}", style = MaterialTheme.typography.labelLarge)
                c.members.forEach { uid ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("@" + c.name(uid), Modifier.weight(1f), fontWeight = if (uid == me.uid) FontWeight.Bold else FontWeight.Normal)
                        if (uid == c.ownerId) Icon(Icons.Rounded.Star, "Владелец", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        else if (owner) IconButton(onClick = { run { Collabs.remove(c, uid) } }, enabled = !busy) {
                            Icon(Icons.Rounded.Close, "Убрать", Modifier.size(18.dp))
                        }
                    }
                }
                if (c.invited.isNotEmpty()) {
                    Spacer(Modifier.size(8.dp))
                    Text("Приглашены, ещё не приняли", style = MaterialTheme.typography.labelLarge)
                    c.invited.forEach { uid ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("@" + c.name(uid), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (owner) IconButton(onClick = { run { Collabs.remove(c, uid) } }, enabled = !busy) {
                                Icon(Icons.Rounded.Close, "Отменить приглашение", Modifier.size(18.dp))
                            }
                        }
                    }
                }
                if (owner) {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    OutlinedTextField(
                        username, { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(20) },
                        label = { Text("Пригласить по юзернейму") }, prefix = { Text("@") }, singleLine = true,
                        shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { run { Collabs.invite(c, username); username = "" } }, enabled = !busy && username.length >= 3) {
                                Icon(Icons.Rounded.PersonAdd, "Пригласить")
                            }
                        },
                    )
                    Text("Человек увидит приглашение в разделе «Совместные проекты».",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                TextButton(onClick = { confirm = if (owner) "delete" else "leave" }, enabled = !busy) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (owner) "Закончить совместную работу" else "Покинуть проект", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )

    confirm?.let { what ->
        ConfirmDialog(
            if (what == "delete") "Закончить совместную работу?" else "Покинуть проект?",
            if (what == "delete") "Общая копия будет удалена у всех. Локальные файлы на устройствах участников останутся."
            else "Вы перестанете получать изменения. Локальная копия останется у вас как обычный проект.",
            if (what == "delete") "Закончить" else "Покинуть",
            onDismiss = { confirm = null },
        ) {
            confirm = null
            run {
                if (what == "delete") Collabs.delete(c) else Collabs.leave(c, me.uid)
                val dir = localDir ?: vm.workspace.root.listFiles().orEmpty()
                    .firstOrNull { com.sprout.ide.cloud.CollabMarker.read(it)?.id == c.id }
                if (dir != null) vm.detachCollab(dir)
                onDismiss()
            }
        }
    }
}
