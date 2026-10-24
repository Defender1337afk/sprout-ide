package com.sprout.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sprout.ide.data.ProjectShape
import com.sprout.ide.lang.Language
import com.sprout.ide.ui.theme.ColorGen
import com.sprout.ide.ui.theme.JetBrainsMono
import com.sprout.ide.ui.theme.LocalDarkTheme

@Composable
fun ProjectShape.shape(): Shape = when (this) {
    ProjectShape.Cookie9 -> MaterialShapes.Cookie9Sided.toShape()
    ProjectShape.Cookie12 -> MaterialShapes.Cookie12Sided.toShape()
    ProjectShape.Cookie4 -> MaterialShapes.Clover4Leaf.toShape()
    ProjectShape.Sunny -> MaterialShapes.Sunny.toShape()
    ProjectShape.Flower -> MaterialShapes.Flower.toShape()
    ProjectShape.Pill -> MaterialShapes.Pill.toShape()
    ProjectShape.Gem -> MaterialShapes.Gem.toShape()
    ProjectShape.Ghost -> MaterialShapes.Ghostish.toShape()
    ProjectShape.Pentagon -> MaterialShapes.Pentagon.toShape()
    ProjectShape.Circle -> MaterialShapes.Circle.toShape()
    ProjectShape.Square -> MaterialShapes.Square.toShape()
}

@Composable
fun languageColors(lang: Language): Pair<Color, Color> = ColorGen.accent(lang.hue, LocalDarkTheme.current)

/** Coloured language badge (KT, PY, …) used for files and tabs. */
@Composable
fun FileBadge(lang: Language, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val (bg, fg) = languageColors(lang)
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            lang.badge,
            color = fg,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * if (lang.badge.length > 2) 0.3f else 0.38f).sp,
            maxLines = 1,
        )
    }
}

@Composable
fun ShapeAvatar(text: String, shape: Shape, bg: Color, fg: Color, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(shape).background(bg), contentAlignment = Alignment.Center) {
        Text(text, color = fg, fontWeight = FontWeight.Black, fontSize = (size.value * 0.36f).sp, maxLines = 1)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 28.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Grouped list in M3 Expressive style: big outer radius, small inner radius between items. */
@Composable
fun SettingsGroup(content: @Composable SettingsGroupScope.() -> Unit) {
    val scope = SettingsGroupScope()
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) { scope.content() }
}

class SettingsGroupScope

@Composable
fun GroupItem(
    index: Int,
    count: Int,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val big = 24.dp * 1f
    val small = 6.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) big else small, topEnd = if (index == 0) big else small,
        bottomStart = if (index == count - 1) big else small, bottomEnd = if (index == count - 1) big else small,
    )
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clip(shape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) { content() }
}

@Composable
fun ItemRow(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
