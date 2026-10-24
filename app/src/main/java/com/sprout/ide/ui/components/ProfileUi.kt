package com.sprout.ide.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sprout.ide.cloud.Cloud
import com.sprout.ide.cloud.Perks
import com.sprout.ide.cloud.UserProfile
import com.sprout.ide.cloud.parseHex
import kotlin.math.cos
import kotlin.math.sin

/** Linear gradient across a box at an arbitrary angle (degrees), optionally shifted by [phase] 0..1. */
fun angledBrush(colors: List<Color>, angle: Float, size: Size, phase: Float = 0f): Brush {
    val rad = Math.toRadians(angle.toDouble())
    val dx = cos(rad).toFloat()
    val dy = sin(rad).toFloat()
    val half = (kotlin.math.abs(size.width * dx) + kotlin.math.abs(size.height * dy)) / 2f
    val c = Offset(size.width / 2, size.height / 2)
    val shift = Offset(dx, dy) * (phase * half * 2)
    val cs = if (colors.size == 1) colors + colors else colors
    return if (phase == 0f) Brush.linearGradient(cs, c - Offset(dx, dy) * half, c + Offset(dx, dy) * half)
    else Brush.linearGradient(cs + cs.first(), c - Offset(dx, dy) * half + shift, c + Offset(dx, dy) * half + shift,
        tileMode = androidx.compose.ui.graphics.TileMode.Mirror)
}

@Composable
fun rememberCloudImage(uid: String, kind: String, enabled: Boolean, version: Any? = null): Bitmap? {
    var bmp by remember(uid, kind, enabled, version) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uid, kind, enabled, version) { if (enabled) bmp = Cloud.image(uid, kind) }
    return bmp
}

@Composable
fun rememberUrlImage(url: String): Bitmap? {
    var bmp by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) { if (url.isNotBlank()) bmp = Cloud.urlImage(url) }
    return bmp
}

@Composable
fun GradientBanner(
    colors: List<String>,
    angle: Int,
    animated: Boolean,
    image: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val cs = colors.map { parseHex(it) }.ifEmpty { listOf(MaterialTheme.colorScheme.primary) }
    val phase = if (animated) {
        val t = rememberInfiniteTransition(label = "banner")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "p").value
    } else 0f
    Box(modifier.drawBehind { drawRect(angledBrush(cs, angle.toFloat(), size, phase)) }) {
        if (image != null) {
            Image(image.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun ProfileBanner(p: UserProfile, modifier: Modifier = Modifier, version: Any? = null) {
    val img = rememberCloudImage(p.uid, "banner", p.bannerImage, version)
    GradientBanner(p.bannerColors, p.bannerAngle, p.has(Perks.ANIMATED_BANNER), img, modifier)
}

@Composable
fun ProfileAvatar(p: UserProfile, size: Dp, modifier: Modifier = Modifier, version: Any? = null) {
    val custom = rememberCloudImage(p.uid, "avatar", p.avatarImage, version)
    val google = rememberUrlImage(if (!p.avatarImage) p.photoUrl else "")
    val img = custom ?: google
    val ring = p.has(Perks.AVATAR_RING)
    val ringColors = (p.nameColors.takeIf { it.size >= 2 } ?: p.bannerColors).map { parseHex(it) }
    val spin = if (ring) {
        rememberInfiniteTransition(label = "ring").animateFloat(0f, 360f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "r").value
    } else 0f
    val shape = MaterialShapes.Cookie12Sided.toShape()
    Box(
        modifier.size(size).then(
            if (ring) Modifier.drawBehind {
                drawCircle(angledBrush(ringColors, spin, this.size), radius = this.size.minDimension / 2)
            }.padding(size * 0.05f) else Modifier
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize().clip(shape).background(parseHex(p.accent)).border(3.dp, MaterialTheme.colorScheme.surface, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (img != null) Image(img.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(
                (p.displayName.ifBlank { p.username }).take(1).uppercase(),
                color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
fun ProfileName(p: UserProfile, style: TextStyle, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val name = p.displayName.ifBlank { "@" + p.username }
        if (p.has(Perks.NAME_GRADIENT) && p.nameColors.size >= 2) {
            Text(name, style = style.copy(brush = Brush.linearGradient(p.nameColors.map { parseHex(it) })), fontWeight = FontWeight.Black)
        } else {
            Text(name, style = style, fontWeight = FontWeight.Black)
        }
        if (p.verified) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Verified, "Подтверждён", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(style.fontSize.value.dp * 1.1f))
        }
    }
}

fun readableOn(c: Color): Color = if (0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue > 0.55f) Color(0xFF111111) else Color.White
