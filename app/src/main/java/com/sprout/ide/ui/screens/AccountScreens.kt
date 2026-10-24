package com.sprout.ide.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sprout.ide.AppViewModel
import com.sprout.ide.Screen
import com.sprout.ide.cloud.Badge
import com.sprout.ide.cloud.Cloud
import com.sprout.ide.cloud.Perks
import com.sprout.ide.cloud.PublicProject
import com.sprout.ide.cloud.UserProfile
import com.sprout.ide.cloud.parseHex
import com.sprout.ide.cloud.toHex
import com.sprout.ide.lang.Languages
import com.sprout.ide.ui.components.BadgeIcon
import com.sprout.ide.ui.components.FileBadge
import com.sprout.ide.ui.components.GradientBanner
import com.sprout.ide.ui.components.ProfileAvatar
import com.sprout.ide.ui.components.ProfileBanner
import com.sprout.ide.ui.components.ProfileName
import com.sprout.ide.ui.components.rememberCloudImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// ------------------------------------------------------------------------------------------------
// Account creation (first Google sign-in)
// ------------------------------------------------------------------------------------------------

@Composable
fun CreateAccountScreen(vm: AppViewModel) {
    val user = vm.user ?: return
    var displayName by remember { mutableStateOf(user.displayName ?: "") }
    var username by remember {
        mutableStateOf(user.email?.substringBefore('@')?.lowercase()?.replace(Regex("[^a-z0-9_]"), "_")?.take(20)?.padEnd(3, '_') ?: "")
    }
    var free by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val valid = Cloud.usernameRegex.matches(username)

    LaunchedEffect(username) {
        free = null
        if (!valid) return@LaunchedEffect
        delay(400)
        free = runCatching { Cloud.isUsernameFree(username) }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Создание аккаунта", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text(user.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        val preview = UserProfile(uid = user.uid, username = username, displayName = displayName, photoUrl = user.photoUrl?.toString() ?: "")
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                ProfileBanner(preview, Modifier.fillMaxWidth().height(110.dp))
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(preview, 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        ProfileName(preview, MaterialTheme.typography.titleLarge)
                        Text("@$username", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            displayName, { displayName = it.take(40) }, label = { Text("Имя") }, singleLine = true,
            shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            username, { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
            label = { Text("Юзернейм") }, prefix = { Text("@") }, singleLine = true,
            shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    when {
                        !valid -> "3–20 символов: латиница, цифры, _"
                        free == null -> "Проверяем…"
                        free == true -> "Свободен"
                        else -> "Уже занят"
                    },
                    color = if (free == false || !valid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = { if (free == true && valid) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) },
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                busy = true; error = null
                scope.launch {
                    runCatching { Cloud.createAccount(user, username, displayName.ifBlank { username }) }
                        .onFailure { error = it.message ?: "Ошибка" }
                    busy = false
                }
            },
            enabled = valid && free == true && !busy && displayName.isNotBlank(),
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (busy) LoadingIndicator(Modifier.size(28.dp)) else Text("Создать аккаунт", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = { vm.signOut() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Выйти и продолжить как гость") }
    }
}

// ------------------------------------------------------------------------------------------------
// Profile
// ------------------------------------------------------------------------------------------------

@Composable
fun ProfileScreen(vm: AppViewModel, uid: String) {
    val own = vm.me?.uid == uid
    var loaded by remember(uid) { mutableStateOf<UserProfile?>(null) }
    var notFound by remember(uid) { mutableStateOf(false) }
    var projects by remember(uid) { mutableStateOf<List<PublicProject>?>(null) }
    var badgeInfo by remember { mutableStateOf<Badge?>(null) }
    var menu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uid) {
        if (!own) {
            val p = runCatching { Cloud.profile(uid) }.getOrNull()
            loaded = p; notFound = p == null
        }
        projects = runCatching { Cloud.projectsOf(uid) }.getOrDefault(emptyList())
    }
    val p = if (own) vm.me else loaded

    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (p == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (notFound) Text("Профиль не найден") else LoadingIndicator()
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 48.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Box {
                        ProfileBanner(p, Modifier.fillMaxWidth().height(210.dp))
                        ProfileAvatar(
                            p, 112.dp,
                            Modifier.align(Alignment.BottomStart).offset(x = 20.dp, y = 56.dp),
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, end = 16.dp), horizontalArrangement = Arrangement.End) {
                        if (own) {
                            FilledTonalButton(onClick = { vm.navigate(Screen.EditProfile) }, shapes = ButtonDefaults.shapes()) {
                                Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Изменить")
                            }
                        }
                    }
                    Spacer(Modifier.height(if (own) 8.dp else 56.dp))
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        ProfileName(p, MaterialTheme.typography.headlineMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("@${p.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                            if (p.title.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = CircleShape, color = parseHex(p.accent).copy(alpha = 0.18f)) {
                                    Text(p.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                            }
                            if (p.role == "admin" || p.role == "moderator") {
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                                    Text(if (p.role == "admin") "Админ" else "Модератор", style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                            }
                        }
                        if (p.status.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(p.status, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                        if (p.bio.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(p.bio, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (p.banned) {
                            Spacer(Modifier.height(8.dp))
                            Text("Аккаунт заблокирован", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        val links = listOfNotNull(
                            p.telegram.takeIf { it.isNotBlank() }?.let { Triple("Telegram", "@$it", "https://t.me/$it") },
                            p.github.takeIf { it.isNotBlank() }?.let { Triple("GitHub", it, "https://github.com/$it") },
                            p.website.takeIf { it.isNotBlank() }?.let { Triple("Сайт", it.removePrefix("https://").removePrefix("http://"),
                                if (it.startsWith("http")) it else "https://$it") },
                        )
                        if (links.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                links.forEach { (kind, label, url) ->
                                    AssistChip(
                                        onClick = { open(url) },
                                        label = { Text(label, maxLines = 1) },
                                        leadingIcon = {
                                            Icon(
                                                when (kind) { "Telegram" -> Icons.AutoMirrored.Rounded.Send; "GitHub" -> Icons.Rounded.Code; else -> Icons.Rounded.Language },
                                                null, Modifier.size(18.dp),
                                            )
                                        },
                                        shape = CircleShape,
                                    )
                                }
                            }
                        }
                        val badges = p.badges.mapNotNull { vm.badgeCatalog[it] }
                        if (badges.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text("Бейджи", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                badges.forEach { b ->
                                    BadgeIcon(b, 48.dp, Modifier.clip(CircleShape).clickable { badgeInfo = b })
                                }
                            }
                        }
                        p.createdAt?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "С нами с " + SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(it.toDate()),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("Проекты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                val list = projects
                when {
                    list == null -> item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
                    list.isEmpty() -> item {
                        Text(
                            if (own) "Пока ничего не опубликовано. Откройте проект и выберите «Опубликовать»." else "Нет опубликованных проектов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    else -> items(list, key = { it.id }) { pr ->
                        PublicProjectCard(pr, showAuthor = false) { vm.navigate(Screen.PublicProject(pr.id)) }
                    }
                }
            }
        }
        // floating top bar over the banner
        Row(Modifier.statusBarsPadding().padding(8.dp).fillMaxWidth()) {
            FilledTonalIconButton(onClick = { vm.back() }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Spacer(Modifier.weight(1f))
            if (own) Box {
                FilledTonalIconButton(onClick = { menu = true }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.MoreVert, "Меню") }
                DropdownMenu(menu, { menu = false }, shape = MaterialTheme.shapes.large) {
                    DropdownMenuItem({ Text("Выйти из аккаунта") }, { menu = false; vm.signOut() },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, null) })
                }
            }
        }
    }
    badgeInfo?.let { b ->
        AlertDialog(
            onDismissRequest = { badgeInfo = null },
            icon = { BadgeIcon(b, 96.dp) },
            title = { Text(b.name) },
            text = { Text(b.description.ifBlank { "Выдаётся администрацией Sprout IDE." }) },
            confirmButton = { TextButton(onClick = { badgeInfo = null }) { Text("Круто") } },
        )
    }
}

@Composable
fun PublicProjectCard(p: PublicProject, showAuthor: Boolean, onClick: () -> Unit) {
    val lang = Languages.all.firstOrNull { it.id == p.language } ?: Languages.Plain
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileBadge(lang, 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (p.featured) { Spacer(Modifier.width(6.dp)); Text("⭐") }
                    }
                    Text(
                        (if (showAuthor) "@${p.ownerUsername} · " else "") + "${lang.name} · ${p.fileCount} ${plural(p.fileCount, "файл", "файла", "файлов")}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    )
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(p.license, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            if (p.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(p.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (p.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(p.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Edit profile
// ------------------------------------------------------------------------------------------------

private val BasicGradients = listOf(
    listOf("#1F6E36", "#8EE09C"), listOf("#0F3B20", "#2E8B45"), listOf("#0D47A1", "#42A5F5"), listOf("#4A148C", "#CE93D8"),
    listOf("#B71C1C", "#FF8A65"), listOf("#E65100", "#FFD54F"), listOf("#004D40", "#80CBC4"), listOf("#212121", "#757575"),
)
private val PremiumGradients = listOf(
    listOf("#FF512F", "#DD2476", "#7B1FA2"), listOf("#00C9FF", "#92FE9D"), listOf("#FC466B", "#3F5EFB"),
    listOf("#F7971E", "#FFD200", "#FF5E62"), listOf("#11998E", "#38EF7D", "#C6FFDD"), listOf("#8E2DE2", "#4A00E0", "#00D2FF"),
    listOf("#000000", "#434343", "#2E8B45"), listOf("#FFAFBD", "#C9FFBF", "#9BE7FF"),
)

@Composable
fun EditProfileScreen(vm: AppViewModel) {
    val me = vm.me ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var displayName by remember { mutableStateOf(me.displayName) }
    var username by remember { mutableStateOf(me.username) }
    var status by remember { mutableStateOf(me.status) }
    var bio by remember { mutableStateOf(me.bio) }
    var telegram by remember { mutableStateOf(me.telegram) }
    var github by remember { mutableStateOf(me.github) }
    var website by remember { mutableStateOf(me.website) }
    var bannerColors by remember { mutableStateOf(me.bannerColors) }
    var bannerAngle by remember { mutableIntStateOf(me.bannerAngle) }
    var accent by remember { mutableStateOf(me.accent) }
    var nameColors by remember { mutableStateOf(me.nameColors.ifEmpty { listOf("#2E8B45", "#8EE09C") }) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf<String?>(null) }
    var imageVersion by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf<Pair<String, (String) -> Unit>?>(null) }

    fun upload(kind: String, uri: Uri?) {
        if (uri == null) return
        uploading = kind
        scope.launch {
            runCatching { Cloud.uploadImage(context, me.uid, kind, uri) }
                .onSuccess { imageVersion++ }
                .onFailure { snackbar.showSnackbar(it.message ?: "Не удалось загрузить") }
            uploading = null
        }
    }
    val pickBanner = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { upload("banner", it) }
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { upload("avatar", it) }

    val draft = me.copy(
        displayName = displayName, username = username, status = status, bio = bio, bannerColors = bannerColors,
        bannerAngle = bannerAngle, accent = accent, nameColors = if (me.has(Perks.NAME_GRADIENT)) nameColors else me.nameColors,
    )

    fun save() {
        saving = true
        scope.launch {
            runCatching {
                if (username != me.username) Cloud.changeUsername(me.uid, me.username, username)
                Cloud.updateProfile(
                    me.uid,
                    mapOf(
                        "displayName" to displayName.trim().take(40),
                        "status" to status.trim().take(80),
                        "bio" to bio.trim().take(me.bioLimit),
                        "telegram" to telegram.trim().removePrefix("@").removePrefix("https://t.me/").removePrefix("t.me/").take(32),
                        "github" to github.trim().removePrefix("@").removePrefix("https://github.com/").removePrefix("github.com/").take(39),
                        "website" to website.trim().take(100),
                        "bannerColors" to bannerColors.take(3),
                        "bannerAngle" to bannerAngle,
                        "accent" to accent,
                    ) + (if (me.has(Perks.NAME_GRADIENT)) mapOf("nameColors" to nameColors.take(3)) else emptyMap()),
                )
            }.onSuccess { vm.back() }.onFailure { snackbar.showSnackbar(it.message ?: "Ошибка сохранения") }
            saving = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.Rounded.Close, "Отмена") } },
                title = { Text("Профиль") },
                actions = {
                    Button(onClick = ::save, enabled = !saving && Cloud.usernameRegex.matches(username), shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.padding(end = 8.dp)) {
                        if (saving) LoadingIndicator(Modifier.size(24.dp)) else Text("Сохранить")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(bottom = 40.dp),
        ) {
            // live preview
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Column {
                    Box {
                        val bannerImg = rememberCloudImage(me.uid, "banner", me.bannerImage, imageVersion)
                        GradientBanner(bannerColors, bannerAngle, me.has(Perks.ANIMATED_BANNER), bannerImg, Modifier.fillMaxWidth().height(140.dp))
                        if (uploading != null) Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                    }
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(draft, 72.dp, version = imageVersion)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            ProfileName(draft, MaterialTheme.typography.titleLarge)
                            Text("@$username", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
            }

            EditSection("Баннер")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickBanner.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Картинка")
                }
                if (me.bannerImage) TextButton(onClick = {
                    scope.launch { Cloud.updateProfile(me.uid, mapOf("bannerImage" to false)); Cloud.invalidateImage(me.uid, "banner"); imageVersion++ }
                }) { Text("Убрать картинку") }
            }
            Text("Градиенты", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
            GradientRow(BasicGradients, locked = false) { bannerColors = it }
            Row(Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Премиум", style = MaterialTheme.typography.labelLarge)
                if (!me.has(Perks.PREMIUM_GRADIENTS)) {
                    Spacer(Modifier.width(6.dp)); Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp)); Text("выдаётся администрацией", style = MaterialTheme.typography.labelSmall)
                }
            }
            GradientRow(PremiumGradients, locked = !me.has(Perks.PREMIUM_GRADIENTS)) { bannerColors = it }
            Text("Свои цвета", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
            ColorStops(bannerColors, max = 3, onChange = { bannerColors = it }, onPick = { c, cb -> picking = c to cb })
            Text("Угол · $bannerAngle°", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
            Slider(bannerAngle.toFloat(), { bannerAngle = it.toInt() }, valueRange = 0f..360f, modifier = Modifier.padding(horizontal = 16.dp))

            EditSection("Аватар")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Загрузить фото")
                }
                if (me.avatarImage) TextButton(onClick = {
                    scope.launch { Cloud.updateProfile(me.uid, mapOf("avatarImage" to false)); Cloud.invalidateImage(me.uid, "avatar"); imageVersion++ }
                }) { Text("Фото Google") }
            }
            Text("Акцентный цвет", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("#2E8B45", "#1B5E20", "#00897B", "#1E88E5", "#3F51B5", "#7E57C2", "#D81B60", "#E53935", "#FB8C00", "#FFB300", "#6D4C41", "#455A64")) { c ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(parseHex(c)).clickable { accent = c }
                            .then(if (accent.equals(c, true)) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { if (accent.equals(c, true)) Icon(Icons.Rounded.Check, null, tint = Color.White) }
                }
                item {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { picking = accent to { c: String -> accent = c } }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, "Свой")
                    }
                }
            }

            EditSection("Градиент имени")
            if (me.has(Perks.NAME_GRADIENT)) {
                ColorStops(nameColors, max = 3, onChange = { nameColors = it }, onPick = { c, cb -> picking = c to cb })
            } else {
                LockedNote("Градиентное имя выдаётся администрацией")
            }

            EditSection("О себе")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Имя", displayName, 40) { displayName = it }
                OutlinedTextField(
                    username, { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                    label = { Text("Юзернейм") }, prefix = { Text("@") }, singleLine = true, shape = MaterialTheme.shapes.large,
                    isError = !Cloud.usernameRegex.matches(username), modifier = Modifier.fillMaxWidth(),
                )
                Field("Статус", status, 80) { status = it }
                Field("Описание", bio, me.bioLimit, singleLine = false) { bio = it }
            }

            EditSection("Ссылки")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Telegram", telegram, 40, prefix = "@") { telegram = it }
                Field("GitHub", github, 39, prefix = "github.com/") { github = it }
                Field("Сайт", website, 100) { website = it }
            }

            EditSection("Выдано администрацией")
            Column(Modifier.padding(horizontal = 20.dp)) {
                val badges = me.badges.mapNotNull { vm.badgeCatalog[it] }
                if (badges.isEmpty() && me.perks.isEmpty() && !me.verified && me.title.isBlank()) {
                    Text("Пока ничего. Бейджи и бонусы выдаёт администратор.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    badges.forEach { BadgeIcon(it, 44.dp) }
                }
                Perks.all.filter { me.has(it.first) }.forEach { (_, title) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text(title)
                    }
                }
            }
        }
    }

    picking?.let { (initial, cb) ->
        ColorPickerDialog(initial, onDismiss = { picking = null }) { cb(it); picking = null }
    }
}

@Composable
private fun EditSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp))
}

@Composable
private fun LockedNote(text: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null); Spacer(Modifier.width(12.dp)); Text(text)
        }
    }
}

@Composable
private fun Field(label: String, value: String, max: Int, singleLine: Boolean = true, prefix: String? = null, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, { onChange(it.take(max)) }, label = { Text(label) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        prefix = prefix?.let { { Text(it) } }, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth(),
        supportingText = if (!singleLine) ({ Text("${value.length} / $max") }) else null,
    )
}

@Composable
private fun GradientRow(gradients: List<List<String>>, locked: Boolean, onPick: (List<String>) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(gradients) { g ->
            Box(Modifier.size(width = 72.dp, height = 48.dp).clip(MaterialTheme.shapes.medium).clickable(enabled = !locked) { onPick(g) }) {
                GradientBanner(g, 135, false, null, Modifier.matchParentSize())
                if (locked) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ColorStops(colors: List<String>, max: Int, onChange: (List<String>) -> Unit, onPick: (String, (String) -> Unit) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.forEachIndexed { i, c ->
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(parseHex(c)).clickable {
                    onPick(c) { picked -> onChange(colors.toMutableList().also { it[i] = picked }) }
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Edit, null, tint = com.sprout.ide.ui.components.readableOn(parseHex(c)), modifier = Modifier.size(18.dp)) }
        }
        if (colors.size < max) FilledTonalIconButton(onClick = { onChange(colors + colors.last()) }) { Icon(Icons.Rounded.Add, "Добавить цвет") }
        if (colors.size > 2) IconButton(onClick = { onChange(colors.dropLast(1)) }) { Icon(Icons.Rounded.Close, "Убрать цвет") }
    }
}

@Composable
fun ColorPickerDialog(initial: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(parseHex(initial).toArgb(), it) } }
    var h by remember { mutableStateOf(hsv[0]) }
    var s by remember { mutableStateOf(hsv[1]) }
    var v by remember { mutableStateOf(hsv[2]) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Цвет") },
        text = {
            Column {
                Box(Modifier.fillMaxWidth().height(64.dp).clip(MaterialTheme.shapes.large).background(color), contentAlignment = Alignment.Center) {
                    Text(color.toHex(), color = com.sprout.ide.ui.components.readableOn(color), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text("Оттенок"); Slider(h, { h = it }, valueRange = 0f..360f)
                Text("Насыщенность"); Slider(s, { s = it })
                Text("Яркость"); Slider(v, { v = it })
            }
        },
        confirmButton = { TextButton(onClick = { onPick(color.toHex()) }) { Text("Готово") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
