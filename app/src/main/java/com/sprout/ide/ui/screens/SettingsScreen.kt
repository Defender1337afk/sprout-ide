package com.sprout.ide.ui.screens

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprout.ide.AppViewModel
import com.sprout.ide.R
import com.sprout.ide.Screen
import com.sprout.ide.ui.components.ProfileAvatar
import com.sprout.ide.data.AppSettings
import com.sprout.ide.data.ContrastLevel
import com.sprout.ide.data.EditorFont
import com.sprout.ide.data.FileSort
import com.sprout.ide.data.PaletteStyle
import com.sprout.ide.data.ProjectShape
import com.sprout.ide.data.SymbolBarStyle
import com.sprout.ide.data.ThemeMode
import com.sprout.ide.lang.Highlighter
import com.sprout.ide.lang.Languages
import com.sprout.ide.lang.SyntaxThemes
import com.sprout.ide.ui.components.GroupItem
import com.sprout.ide.ui.components.ItemRow
import com.sprout.ide.ui.components.SectionHeader
import com.sprout.ide.ui.components.SettingsGroup
import com.sprout.ide.ui.components.shape
import com.sprout.ide.ui.theme.ColorGen
import com.sprout.ide.ui.theme.LocalDarkTheme
import com.sprout.ide.ui.theme.LocalSettings
import com.sprout.ide.ui.theme.family
import kotlin.math.roundToInt

private val Seeds = listOf(
    0xFF2E8B45, 0xFF1B5E20, 0xFF3DDC84, 0xFF00897B, 0xFF0097A7, 0xFF1E88E5, 0xFF3F51B5,
    0xFF7E57C2, 0xFF8E24AA, 0xFFD81B60, 0xFFE53935, 0xFFF4511E, 0xFFFB8C00, 0xFFFFB300, 0xFF795548, 0xFF607D8B,
)

private val SAMPLE = """// Sprout IDE 🌱
fun greet(name: String): String {
    val n = 42 * 2
    return "Привет, ${'$'}name! ${'$'}n"
}"""

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s = LocalSettings.current
    val set: ((AppSettings) -> AppSettings) -> Unit = { vm.updateSettings(it) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var editSymbols by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                subtitle = { Text("Сделайте Sprout своим") },
                navigationIcon = { IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
                actions = { IconButton(onClick = { confirmReset = true }) { Icon(Icons.Rounded.RestartAlt, "Сбросить") } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 48.dp), modifier = Modifier.fillMaxSize()) {
            item { EditorPreview(s) }

            // ---------- account
            item { SectionHeader("Аккаунт") }
            item {
                SettingsGroup {
                    val me = vm.me
                    if (me != null) {
                        GroupItem(0, 2, onClick = { vm.navigate(Screen.Profile(me.uid)) }) {
                            ItemRow(me.displayName, "@${me.username} · ${vm.user?.email ?: ""}", leading = { ProfileAvatar(me, 48.dp) })
                        }
                        GroupItem(1, 2, onClick = { vm.signOut() }) {
                            ItemRow("Выйти", "Останется гостевой режим с локальными проектами",
                                leading = { Icon(Icons.AutoMirrored.Rounded.Logout, null) })
                        }
                    } else {
                        GroupItem(0, 1, onClick = { vm.showOnboarding() }) {
                            ItemRow("Гость", "Войдите через Google, чтобы получить профиль и публиковать проекты",
                                leading = { Icon(Icons.Rounded.AccountCircle, null, Modifier.size(40.dp)) })
                        }
                    }
                }
            }

            // ---------- theme
            item { SectionHeader("Тема") }
            item {
                SettingsGroup {
                    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    val count = if (dynamicAvailable) 6 else 5
                    var i = 0
                    GroupItem(i++, count) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Режим", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(10.dp))
                            Connected(ThemeMode.entries, s.themeMode, { it.title }) { m -> set { it.copy(themeMode = m) } }
                        }
                    }
                    GroupItem(i++, count) {
                        SwitchRow("AMOLED-чёрный", "Чистый чёрный фон в тёмной теме", s.amoled) { v -> set { it.copy(amoled = v) } }
                    }
                    if (dynamicAvailable) GroupItem(i++, count) {
                        SwitchRow("Цвета обоев", "Material You из системной палитры", s.dynamicColor) { v -> set { it.copy(dynamicColor = v) } }
                    }
                    GroupItem(i++, count) {
                        Column(Modifier.padding(vertical = 16.dp)) {
                            Text("Акцентный цвет", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(12.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(Seeds) { c ->
                                    Swatch(Color(c), selected = !s.dynamicColor && s.seedColor == c, style = s.paletteStyle) {
                                        set { it.copy(seedColor = c, dynamicColor = false) }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            val hue = ColorGen.hueOf(Color(s.seedColor)).toFloat()
                            Text("Свой оттенок · ${hue.roundToInt()}°", style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp))
                            Slider(
                                value = hue, valueRange = 0f..359f,
                                onValueChange = { h ->
                                    val c = android.graphics.Color.HSVToColor(floatArrayOf(h, 0.7f, 0.6f)).toLong() and 0xFFFFFFFF
                                    set { it.copy(seedColor = c, dynamicColor = false) }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    GroupItem(i++, count) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Стиль палитры", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PaletteStyle.entries.forEach { p ->
                                    FilterChip(s.paletteStyle == p, { set { it.copy(paletteStyle = p) } }, label = { Text(p.title) },
                                        leadingIcon = if (s.paletteStyle == p) ({ Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }) else null)
                                }
                            }
                        }
                    }
                    GroupItem(i, count) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Контраст", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(10.dp))
                            Connected(ContrastLevel.entries, s.contrast, { it.title }) { c -> set { it.copy(contrast = c) } }
                        }
                    }
                }
            }

            // ---------- shape & motion
            item { SectionHeader("Формы и движение") }
            item {
                SettingsGroup {
                    GroupItem(0, 5) {
                        SliderRow("Скругление углов", "×${"%.2f".format(s.cornerScale)}", s.cornerScale, 0f..2f, 7) { v -> set { it.copy(cornerScale = v) } }
                    }
                    GroupItem(1, 5) {
                        Column(Modifier.padding(vertical = 16.dp)) {
                            Text("Форма аватаров", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(12.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(ProjectShape.entries) { p ->
                                    val selected = s.projectShape == p
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                                        Box(
                                            Modifier.size(56.dp).clip(p.shape())
                                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                                                .clickable { set { it.copy(projectShape = p) } },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(p.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    GroupItem(2, 5) {
                        SliderRow("Масштаб интерфейса", "${(s.uiScale * 100).roundToInt()}%", s.uiScale, 0.8f..1.2f, 7) { v -> set { it.copy(uiScale = v) } }
                    }
                    GroupItem(3, 5) {
                        SwitchRow("Экспрессивная анимация", "Пружинистые переходы Material 3 Expressive", s.expressiveMotion) { v -> set { it.copy(expressiveMotion = v) } }
                    }
                    GroupItem(4, 5) {
                        SwitchRow("Тактильный отклик", "Вибрация при нажатии клавиш панели", s.haptics) { v -> set { it.copy(haptics = v) } }
                    }
                }
            }

            // ---------- editor
            item { SectionHeader("Редактор") }
            item {
                SettingsGroup {
                    val n = 5
                    GroupItem(0, n) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Шрифт", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EditorFont.entries.forEach { f ->
                                    FilterChip(s.editorFont == f, { set { it.copy(editorFont = f) } },
                                        label = { Text(f.title, fontFamily = f.family()) })
                                }
                            }
                        }
                    }
                    GroupItem(1, n) {
                        SliderRow("Размер шрифта", "${s.fontSize.roundToInt()} sp", s.fontSize, 9f..28f, 18) { v -> set { it.copy(fontSize = v.roundToInt().toFloat()) } }
                    }
                    GroupItem(2, n) {
                        SliderRow("Межстрочный интервал", "%.2f".format(s.lineHeight), s.lineHeight, 1.1f..2.0f, 8) { v -> set { it.copy(lineHeight = v) } }
                    }
                    GroupItem(3, n) {
                        Column(Modifier.padding(vertical = 16.dp)) {
                            Text("Цветовая схема кода", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(12.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(SyntaxThemes.all) { t ->
                                    SyntaxCard(t.title, t.build(MaterialTheme.colorScheme), s.syntaxTheme == t.id) {
                                        set { it.copy(syntaxTheme = t.id) }
                                    }
                                }
                            }
                        }
                    }
                    GroupItem(4, n) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Размер табуляции", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(10.dp))
                            Connected(listOf(2, 4, 8), s.tabSize, { "$it" }) { v -> set { it.copy(tabSize = v) } }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                val toggles = listOf(
                    Triple("Номера строк", "Колонка слева от кода", s.lineNumbers) to { v: Boolean -> set { it.copy(lineNumbers = v) } },
                    Triple("Подсветка текущей строки", null, s.highlightLine) to { v: Boolean -> set { it.copy(highlightLine = v) } },
                    Triple("Парные скобки", "Подсвечивать пару для скобки у курсора", s.bracketHighlight) to { v: Boolean -> set { it.copy(bracketHighlight = v) } },
                    Triple("Перенос строк", null, s.wordWrap) to { v: Boolean -> set { it.copy(wordWrap = v) } },
                    Triple("Показывать пробелы", "· для пробела, → для табуляции", s.showWhitespace) to { v: Boolean -> set { it.copy(showWhitespace = v) } },
                    Triple("Лигатуры", "=> != >= в шрифтах с лигатурами", s.ligatures) to { v: Boolean -> set { it.copy(ligatures = v) } },
                    Triple("Пробелы вместо Tab", null, s.useSpaces) to { v: Boolean -> set { it.copy(useSpaces = v) } },
                    Triple("Автозакрытие скобок и кавычек", null, s.autoClose) to { v: Boolean -> set { it.copy(autoClose = v) } },
                    Triple("Автоотступ", "Сохранять отступ при переносе строки", s.autoIndent) to { v: Boolean -> set { it.copy(autoIndent = v) } },
                    Triple("Автосохранение", "Сохранять файл через секунду после правки", s.autoSave) to { v: Boolean -> set { it.copy(autoSave = v) } },
                    Triple("Масштаб щипком", "Менять размер шрифта двумя пальцами", s.pinchZoom) to { v: Boolean -> set { it.copy(pinchZoom = v) } },
                    Triple("Восстанавливать вкладки", "Открывать прошлые файлы проекта", s.restoreTabs) to { v: Boolean -> set { it.copy(restoreTabs = v) } },
                )
                SettingsGroup {
                    toggles.forEachIndexed { i, (t, f) ->
                        GroupItem(i, toggles.size) { SwitchRow(t.first, t.second, t.third, f) }
                    }
                }
            }

            // ---------- symbol bar
            item { SectionHeader("Панель символов") }
            item {
                SettingsGroup {
                    GroupItem(0, 3) { SwitchRow("Показывать панель", "Быстрые символы над клавиатурой", s.symbolBar) { v -> set { it.copy(symbolBar = v) } } }
                    GroupItem(1, 3) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Стиль клавиш", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(10.dp))
                            Connected(SymbolBarStyle.entries, s.symbolBarStyle, { it.title }) { v -> set { it.copy(symbolBarStyle = v) } }
                        }
                    }
                    GroupItem(2, 3, onClick = { editSymbols = true }) {
                        ItemRow("Набор символов", s.symbols, trailing = { Icon(Icons.Rounded.Edit, null) })
                    }
                }
            }

            // ---------- files
            item { SectionHeader("Файлы") }
            item {
                SettingsGroup {
                    GroupItem(0, 4) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Сортировка", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(10.dp))
                            Connected(FileSort.entries, s.fileSort, { it.title }) { v -> set { it.copy(fileSort = v) } }
                        }
                    }
                    GroupItem(1, 4) { SwitchRow("Папки сверху", null, s.foldersFirst) { v -> set { it.copy(foldersFirst = v) } } }
                    GroupItem(2, 4) { SwitchRow("Скрытые файлы", "Показывать файлы, начинающиеся с точки", s.showHidden) { v -> set { it.copy(showHidden = v) } } }
                    GroupItem(3, 4) { SwitchRow("Подтверждать удаление", null, s.confirmDelete) { v -> set { it.copy(confirmDelete = v) } } }
                }
            }

            // ---------- about
            item { SectionHeader("О приложении") }
            item {
                SettingsGroup {
                    GroupItem(0, 1) {
                        ItemRow(
                            "Sprout IDE ${com.sprout.ide.BuildConfig.VERSION_NAME}",
                            "${Languages.all.size} языков · Material 3 Expressive",
                            leading = { Image(painterResource(R.drawable.ic_logo), null, Modifier.size(48.dp)) },
                        )
                    }
                }
            }
        }
    }

    if (editSymbols) {
        TextInputDialog("Символы через пробел", s.symbols, "Сохранить", label = "⇥ — табуляция, ← → — курсор",
            onDismiss = { editSymbols = false }) { v ->
            set { it.copy(symbols = v.ifBlank { AppSettings.DEFAULT_SYMBOLS }) }
            editSymbols = false
        }
    }
    if (confirmReset) {
        ConfirmDialog("Сбросить настройки?", "Все параметры вернутся к значениям по умолчанию.", "Сбросить",
            onDismiss = { confirmReset = false }) { vm.settingsRepo.reset(); confirmReset = false }
    }
}

@Composable
private fun <T> Connected(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, o ->
            ToggleButton(
                checked = o == selected,
                onCheckedChange = { onSelect(o) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                if (o == selected) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(label(o), maxLines = 1, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(Modifier.clickable { onChange(!checked) }) {
        ItemRow(title, subtitle, trailing = {
            Switch(
                checked, onChange,
                thumbContent = if (checked) ({ Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }) else null,
            )
        })
    }
}

@Composable
private fun SliderRow(title: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(valueText, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun Swatch(seed: Color, selected: Boolean, style: PaletteStyle, onClick: () -> Unit) {
    val dark = LocalDarkTheme.current
    val scheme = remember(seed, style, dark) { ColorGen.scheme(seed, dark, style, ContrastLevel.Standard) }
    val rotation by animateFloatAsState(if (selected) 0f else -90f, label = "swatch")
    val shape = if (selected) MaterialShapes.Cookie9Sided.toShape() else CircleShape
    Box(
        Modifier.size(58.dp).clip(shape).background(scheme.surfaceContainerHighest).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.size(40.dp).rotate(rotation).clip(CircleShape)) {
            Box(Modifier.weight(1f).fillMaxWidth().background(scheme.primary))
            Row(Modifier.weight(1f)) {
                Box(Modifier.weight(1f).fillMaxSize().background(scheme.secondaryContainer))
                Box(Modifier.weight(1f).fillMaxSize().background(scheme.tertiary))
            }
        }
        if (selected) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = scheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SyntaxCard(title: String, c: com.sprout.ide.lang.SyntaxColors, selected: Boolean, onClick: () -> Unit) {
    val sample = remember(c) { Highlighter.annotate("fun main() {\n  val s = \"hi\" // 1\n}", Languages.Kotlin, c) }
    Column(
        Modifier.width(150.dp).clip(MaterialTheme.shapes.large)
            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .background(c.background).clickable(onClick = onClick).padding(12.dp),
    ) {
        Text(sample, fontFamily = LocalSettings.current.editorFont.family(), fontSize = 11.sp, color = c.text, lineHeight = 15.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = c.keyword)
                Spacer(Modifier.width(4.dp))
            }
            Text(title, color = c.text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun EditorPreview(s: AppSettings) {
    val colors = SyntaxThemes.byId(s.syntaxTheme).build(MaterialTheme.colorScheme)
    val text = remember(colors, s.showWhitespace) { Highlighter.annotate(SAMPLE, Languages.Kotlin, colors, showWhitespace = s.showWhitespace) }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.background,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
    ) {
        Row(Modifier.padding(vertical = 14.dp)) {
            if (s.lineNumbers) {
                Column(Modifier.padding(start = 14.dp, end = 10.dp), horizontalAlignment = Alignment.End) {
                    (1..SAMPLE.lines().size).forEach {
                        Text("$it", color = colors.gutterText, fontFamily = s.editorFont.family(), fontSize = s.fontSize.sp,
                            lineHeight = (s.fontSize * s.lineHeight).sp)
                    }
                }
            }
            Text(
                text, color = colors.text, fontFamily = s.editorFont.family(), fontSize = s.fontSize.sp,
                lineHeight = (s.fontSize * s.lineHeight).sp, softWrap = s.wordWrap, maxLines = 12,
                modifier = Modifier.padding(start = if (s.lineNumbers) 0.dp else 16.dp, end = 16.dp),
            )
        }
    }
}
