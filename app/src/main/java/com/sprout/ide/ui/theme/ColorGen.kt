package com.sprout.ide.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.sprout.ide.data.ContrastLevel
import com.sprout.ide.data.PaletteStyle
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Seed-colour → Material 3 colour scheme. Tones are perceptual (CIE L*) and the hue/chroma come from OKLCH,
 * with chroma reduced until the colour fits in sRGB — close to what HCT-based Material tooling produces.
 */
object ColorGen {

    private class Palette(val hue: Double, val chroma: Double) {
        private val cache = HashMap<Int, Color>()
        fun tone(t: Int): Color = cache.getOrPut(t) { toneColor(t.toDouble(), hue, chroma) }
    }

    private data class Oklch(val l: Double, val c: Double, val h: Double)

    fun scheme(seed: Color, dark: Boolean, style: PaletteStyle, contrast: ContrastLevel): ColorScheme {
        val s = toOklch(seed)
        val h = s.h
        val c = s.c
        val primary: Palette
        val secondary: Palette
        val tertiary: Palette
        val neutral: Palette
        val neutralVariant: Palette
        when (style) {
            PaletteStyle.Tonal -> {
                primary = Palette(h, maxOf(c, 0.10)); secondary = Palette(h, 0.04)
                tertiary = Palette(h + 60, 0.07); neutral = Palette(h, 0.010); neutralVariant = Palette(h, 0.018)
            }
            PaletteStyle.Vibrant -> {
                primary = Palette(h, 0.24); secondary = Palette(h + 15, 0.10)
                tertiary = Palette(h + 45, 0.13); neutral = Palette(h, 0.025); neutralVariant = Palette(h, 0.035)
            }
            PaletteStyle.Expressive -> {
                primary = Palette(h + 240, 0.15); secondary = Palette(h + 15, 0.09)
                tertiary = Palette(h + 120, 0.12); neutral = Palette(h + 15, 0.02); neutralVariant = Palette(h + 15, 0.03)
            }
            PaletteStyle.Fidelity -> {
                primary = Palette(h, c); secondary = Palette(h, maxOf(c - 0.08, c * 0.4))
                tertiary = Palette(h + 180, c * 0.6); neutral = Palette(h, c / 12); neutralVariant = Palette(h, c / 8)
            }
            PaletteStyle.Monochrome -> {
                primary = Palette(h, 0.0); secondary = Palette(h, 0.0)
                tertiary = Palette(h, 0.0); neutral = Palette(h, 0.0); neutralVariant = Palette(h, 0.0)
            }
        }
        val error = Palette(29.0, 0.19)
        // contrast boost: push "on" colours and containers further apart
        val k = when (contrast) { ContrastLevel.Standard -> 0; ContrastLevel.Medium -> 1; ContrastLevel.High -> 2 }

        return if (!dark) {
            fun acc(p: Palette) = arrayOf(p.tone(40 - 8 * k), p.tone(100), p.tone(90 - 2 * k), p.tone(if (k == 2) 5 else 30 - 10 * k))
            val (pr, onPr, prC, onPrC) = acc(primary)
            val (se, onSe, seC, onSeC) = acc(secondary)
            val (te, onTe, teC, onTeC) = acc(tertiary)
            val (er, onEr, erC, onErC) = acc(error)
            lightColorScheme(
                primary = pr, onPrimary = onPr, primaryContainer = prC, onPrimaryContainer = onPrC,
                secondary = se, onSecondary = onSe, secondaryContainer = seC, onSecondaryContainer = onSeC,
                tertiary = te, onTertiary = onTe, tertiaryContainer = teC, onTertiaryContainer = onTeC,
                error = er, onError = onEr, errorContainer = erC, onErrorContainer = onErC,
                background = neutral.tone(98), onBackground = neutral.tone(10 - 5 * k / 2),
                surface = neutral.tone(98), onSurface = neutral.tone(10 - 5 * k / 2),
                surfaceVariant = neutralVariant.tone(90), onSurfaceVariant = neutralVariant.tone(30 - 8 * k),
                surfaceTint = pr, inverseSurface = neutral.tone(20), inverseOnSurface = neutral.tone(95),
                inversePrimary = primary.tone(80), outline = neutralVariant.tone(50 - 10 * k),
                outlineVariant = neutralVariant.tone(80 - 10 * k), scrim = Color.Black,
                surfaceBright = neutral.tone(98), surfaceDim = neutral.tone(87),
                surfaceContainerLowest = neutral.tone(100), surfaceContainerLow = neutral.tone(96),
                surfaceContainer = neutral.tone(94), surfaceContainerHigh = neutral.tone(92),
                surfaceContainerHighest = neutral.tone(90),
            )
        } else {
            fun acc(p: Palette) = arrayOf(p.tone(80 + 6 * k), p.tone(20 - 5 * k), p.tone(30 + 5 * k), p.tone(90 + 4 * k))
            val (pr, onPr, prC, onPrC) = acc(primary)
            val (se, onSe, seC, onSeC) = acc(secondary)
            val (te, onTe, teC, onTeC) = acc(tertiary)
            val (er, onEr, erC, onErC) = acc(error)
            darkColorScheme(
                primary = pr, onPrimary = onPr, primaryContainer = prC, onPrimaryContainer = onPrC,
                secondary = se, onSecondary = onSe, secondaryContainer = seC, onSecondaryContainer = onSeC,
                tertiary = te, onTertiary = onTe, tertiaryContainer = teC, onTertiaryContainer = onTeC,
                error = er, onError = onEr, errorContainer = erC, onErrorContainer = onErC,
                background = neutral.tone(6), onBackground = neutral.tone(90 + 4 * k),
                surface = neutral.tone(6), onSurface = neutral.tone(90 + 4 * k),
                surfaceVariant = neutralVariant.tone(30), onSurfaceVariant = neutralVariant.tone(80 + 6 * k),
                surfaceTint = pr, inverseSurface = neutral.tone(90), inverseOnSurface = neutral.tone(20),
                inversePrimary = primary.tone(40), outline = neutralVariant.tone(60 + 10 * k),
                outlineVariant = neutralVariant.tone(30 + 10 * k), scrim = Color.Black,
                surfaceBright = neutral.tone(24), surfaceDim = neutral.tone(6),
                surfaceContainerLowest = neutral.tone(4), surfaceContainerLow = neutral.tone(10),
                surfaceContainer = neutral.tone(12), surfaceContainerHigh = neutral.tone(17),
                surfaceContainerHighest = neutral.tone(22),
            )
        }
    }

    /** Pure-black surfaces for OLED screens, keeping containers slightly lifted so cards stay visible. */
    fun amoled(scheme: ColorScheme): ColorScheme = scheme.copy(
        background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = scheme.surfaceContainerLowest,
        surfaceContainer = scheme.surfaceContainerLow,
        surfaceContainerHigh = scheme.surfaceContainer,
        surfaceContainerHighest = scheme.surfaceContainerHigh,
    )

    fun hueOf(color: Color): Double = toOklch(color).h

    /** A pleasant accent for arbitrary hue (used for file-type badges and project avatars). */
    fun accent(hue: Double, dark: Boolean): Pair<Color, Color> =
        if (dark) toneColor(30.0, hue, 0.09) to toneColor(90.0, hue, 0.09)
        else toneColor(90.0, hue, 0.09) to toneColor(25.0, hue, 0.09)

    // --- colour math -------------------------------------------------------------------------------

    private fun lstarToOkL(t: Double): Double {
        val y = if (t > 8.0) ((t + 16) / 116).pow(3) else t / 903.2963
        return cbrt(y)
    }

    private fun toneColor(tone: Double, hue: Double, chroma: Double): Color {
        if (tone <= 0.0) return Color.Black
        if (tone >= 100.0) return Color.White
        val l = lstarToOkL(tone)
        var lo = 0.0
        var hi = chroma
        if (inGamut(l, hi, hue)) return oklchToColor(l, hi, hue)
        repeat(18) {
            val mid = (lo + hi) / 2
            if (inGamut(l, mid, hue)) lo = mid else hi = mid
        }
        return oklchToColor(l, lo, hue)
    }

    private fun linear(l: Double, c: Double, hDeg: Double): DoubleArray {
        val h = Math.toRadians(hDeg)
        val a = c * cos(h)
        val b = c * sin(h)
        val l_ = l + 0.3963377774 * a + 0.2158037573 * b
        val m_ = l - 0.1055613458 * a - 0.0638541728 * b
        val s_ = l - 0.0894841775 * a - 1.2914855480 * b
        val l3 = l_ * l_ * l_
        val m3 = m_ * m_ * m_
        val s3 = s_ * s_ * s_
        return doubleArrayOf(
            4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
            -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
            -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3,
        )
    }

    private fun inGamut(l: Double, c: Double, h: Double): Boolean =
        linear(l, c, h).all { it >= -0.0005 && it <= 1.0005 }

    private fun gammaEncode(x: Double): Float {
        val v = x.coerceIn(0.0, 1.0)
        return (if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055).toFloat()
    }

    private fun gammaDecode(x: Float): Double =
        if (x <= 0.04045f) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)

    private fun oklchToColor(l: Double, c: Double, h: Double): Color {
        val rgb = linear(l, c, h)
        return Color(gammaEncode(rgb[0]), gammaEncode(rgb[1]), gammaEncode(rgb[2]))
    }

    private fun toOklch(color: Color): Oklch {
        val r = gammaDecode(color.red)
        val g = gammaDecode(color.green)
        val b = gammaDecode(color.blue)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val A = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val B = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        var h = Math.toDegrees(atan2(B, A))
        if (h < 0) h += 360.0
        return Oklch(L, sqrt(A * A + B * B), h)
    }
}
