package com.sprout.ide.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sprout.ide.R
import com.sprout.ide.data.AppSettings
import com.sprout.ide.data.EditorFont
import com.sprout.ide.data.ThemeMode

val LocalSettings = compositionLocalOf { AppSettings() }
val LocalDarkTheme = compositionLocalOf { false }

val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono), Font(R.font.jetbrains_mono_bold, FontWeight.Bold))
val FiraCode = FontFamily(Font(R.font.fira_code))
val SourceCodePro = FontFamily(Font(R.font.source_code_pro))

fun EditorFont.family(): FontFamily = when (this) {
    EditorFont.JetBrainsMono -> JetBrainsMono
    EditorFont.FiraCode -> FiraCode
    EditorFont.SourceCodePro -> SourceCodePro
    EditorFont.System -> FontFamily.Monospace
}

@Composable
fun SproutTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val base = remember(settings.dynamicColor, settings.seedColor, settings.paletteStyle, settings.contrast, dark) {
        if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            ColorGen.scheme(Color(settings.seedColor), dark, settings.paletteStyle, settings.contrast)
        }
    }
    val colors = if (dark && settings.amoled) ColorGen.amoled(base) else base

    val k = settings.cornerScale
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((4 * k).dp),
        small = RoundedCornerShape((8 * k).dp),
        medium = RoundedCornerShape((12 * k).dp),
        large = RoundedCornerShape((16 * k).dp),
        extraLarge = RoundedCornerShape((28 * k).dp),
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalSettings provides settings,
        LocalDarkTheme provides dark,
        LocalDensity provides Density(density.density * settings.uiScale, density.fontScale),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            shapes = shapes,
            motionScheme = if (settings.expressiveMotion) MotionScheme.expressive() else MotionScheme.standard(),
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}
