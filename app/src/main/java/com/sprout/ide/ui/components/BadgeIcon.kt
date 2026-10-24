package com.sprout.ide.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.sprout.ide.cloud.Badge
import com.sprout.ide.cloud.parseHex

/** Icon keys usable in badges/{id}.icon — keep in sync with BADGE_ICONS in admin/panel.py. */
val BadgeIcons: Map<String, ImageVector> = mapOf(
    "eco" to Icons.Rounded.Eco,
    "code" to Icons.Rounded.Code,
    "terminal" to Icons.Rounded.Terminal,
    "bug" to Icons.Rounded.BugReport,
    "shield" to Icons.Rounded.Shield,
    "lock_open" to Icons.Rounded.LockOpen,
    "favorite" to Icons.Rounded.Favorite,
    "star" to Icons.Rounded.Star,
    "bolt" to Icons.Rounded.Bolt,
    "rocket" to Icons.Rounded.RocketLaunch,
    "verified" to Icons.Rounded.Verified,
    "trophy" to Icons.Rounded.EmojiEvents,
    "premium" to Icons.Rounded.WorkspacePremium,
    "diamond" to Icons.Rounded.Diamond,
    "egg" to Icons.Rounded.Egg,
    "fire" to Icons.Rounded.LocalFireDepartment,
    "palette" to Icons.Rounded.Palette,
    "pets" to Icons.Rounded.Pets,
    "music" to Icons.Rounded.MusicNote,
    "science" to Icons.Rounded.Science,
)

/** Shape keys usable in badges/{id}.shape — keep in sync with BADGE_SHAPES in admin/panel.py. */
@Composable
fun badgeShape(key: String): Shape = when (key) {
    "cookie4" -> MaterialShapes.Cookie4Sided
    "cookie6" -> MaterialShapes.Cookie6Sided
    "cookie7" -> MaterialShapes.Cookie7Sided
    "cookie12" -> MaterialShapes.Cookie12Sided
    "sunny" -> MaterialShapes.Sunny
    "very_sunny" -> MaterialShapes.VerySunny
    "flower" -> MaterialShapes.Flower
    "gem" -> MaterialShapes.Gem
    "pentagon" -> MaterialShapes.Pentagon
    "clover" -> MaterialShapes.Clover4Leaf
    "clover8" -> MaterialShapes.Clover8Leaf
    "burst" -> MaterialShapes.SoftBurst
    "boom" -> MaterialShapes.SoftBoom
    "puffy" -> MaterialShapes.Puffy
    "diamond" -> MaterialShapes.PuffyDiamond
    "heart" -> MaterialShapes.Heart
    "ghost" -> MaterialShapes.Ghostish
    "circle" -> MaterialShapes.Circle
    "square" -> MaterialShapes.Square
    else -> MaterialShapes.Cookie9Sided
}.toShape()

/** The badge as a picture: no text, the name lives in the tap dialog / content description. */
@Composable
fun BadgeIcon(b: Badge, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(b.image) {
        if (b.image.isBlank()) null
        else runCatching { Base64.decode(b.image, Base64.DEFAULT).let { BitmapFactory.decodeByteArray(it, 0, it.size) } }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), b.name, modifier.size(size), contentScale = ContentScale.Fit)
        return
    }
    val shape = badgeShape(b.shape)
    val colors = b.colors.map { parseHex(it) }.let { if (it.size == 1) it + it else it }
    val icon = BadgeIcons[b.icon]
    Box(
        modifier.size(size)
            .shadow(size * 0.12f, shape, ambientColor = colors.last(), spotColor = colors.last())
            .clip(shape)
            .drawWithContent {
                drawRect(angledBrush(colors, 135f, this.size))
                // glossy highlight from the top-left
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(this.size.width * 0.3f, this.size.height * 0.2f),
                        radius = this.size.minDimension * 0.75f,
                    ),
                )
                // darker rim at the bottom for depth
                drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.22f)))
                drawOutline(shape.createOutline(this.size, layoutDirection, this), Color.White.copy(alpha = 0.35f),
                    style = Stroke(width = this.size.minDimension * 0.04f))
                drawContent()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, b.name, Modifier.size(size * 0.5f), tint = Color.White)
        } else {
            Text(b.emoji, fontSize = (size.value * 0.42f).sp)
        }
    }
}
