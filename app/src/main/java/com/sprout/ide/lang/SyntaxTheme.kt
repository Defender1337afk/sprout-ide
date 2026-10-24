package com.sprout.ide.lang

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

data class SyntaxColors(
    val background: Color,
    val text: Color,
    val gutter: Color,
    val gutterText: Color,
    val currentLine: Color,
    val selection: Color,
    val cursor: Color,
    val keyword: Color,
    val type: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val function: Color,
    val operator: Color,
    val constant: Color,
    val annotation: Color,
    val tag: Color,
    val attribute: Color,
    val match: Color,
    val bracket: Color,
)

class SyntaxThemeDef(val id: String, val title: String, val build: (ColorScheme) -> SyntaxColors)

private fun fixed(
    bg: Long, fg: Long, gutterBg: Long, gutterFg: Long, line: Long, sel: Long, cursor: Long,
    keyword: Long, type: Long, string: Long, number: Long, comment: Long, function: Long,
    operator: Long, constant: Long, annotation: Long, tag: Long, attr: Long,
): (ColorScheme) -> SyntaxColors = {
    SyntaxColors(
        Color(bg), Color(fg), Color(gutterBg), Color(gutterFg), Color(line), Color(sel), Color(cursor),
        Color(keyword), Color(type), Color(string), Color(number), Color(comment), Color(function),
        Color(operator), Color(constant), Color(annotation), Color(tag), Color(attr),
        match = Color(0x66FFD54F), bracket = Color(cursor).copy(alpha = 0.3f),
    )
}

object SyntaxThemes {
    val all = listOf(
        SyntaxThemeDef("material", "Material You") { c ->
            SyntaxColors(
                background = c.surfaceContainerLowest, text = c.onSurface,
                gutter = c.surfaceContainerLowest, gutterText = c.outline,
                currentLine = c.primary.copy(alpha = 0.07f), selection = c.primary.copy(alpha = 0.28f), cursor = c.primary,
                keyword = c.primary, type = c.tertiary, string = c.secondary,
                number = c.tertiary, comment = c.outline, function = c.onPrimaryContainer,
                operator = c.onSurfaceVariant, constant = c.error, annotation = c.secondary,
                tag = c.primary, attribute = c.tertiary, match = c.tertiaryContainer, bracket = c.primary.copy(alpha = 0.25f),
            )
        },
        SyntaxThemeDef("monokai", "Monokai", fixed(
            0xFF272822, 0xFFF8F8F2, 0xFF272822, 0xFF75715E, 0xFF3E3D32, 0x6649483E, 0xFFF8F8F0,
            0xFFF92672, 0xFF66D9EF, 0xFFE6DB74, 0xFFAE81FF, 0xFF88846F, 0xFFA6E22E,
            0xFFF92672, 0xFFAE81FF, 0xFFA6E22E, 0xFFF92672, 0xFFA6E22E)),
        SyntaxThemeDef("dracula", "Dracula", fixed(
            0xFF282A36, 0xFFF8F8F2, 0xFF282A36, 0xFF6272A4, 0xFF343746, 0x8844475A, 0xFFF8F8F2,
            0xFFFF79C6, 0xFF8BE9FD, 0xFFF1FA8C, 0xFFBD93F9, 0xFF6272A4, 0xFF50FA7B,
            0xFFFF79C6, 0xFFBD93F9, 0xFFF1FA8C, 0xFFFF79C6, 0xFF50FA7B)),
        SyntaxThemeDef("onedark", "One Dark", fixed(
            0xFF282C34, 0xFFABB2BF, 0xFF282C34, 0xFF4B5263, 0xFF2C313C, 0x663E4451, 0xFF528BFF,
            0xFFC678DD, 0xFFE5C07B, 0xFF98C379, 0xFFD19A66, 0xFF5C6370, 0xFF61AFEF,
            0xFF56B6C2, 0xFFD19A66, 0xFFE5C07B, 0xFFE06C75, 0xFFD19A66)),
        SyntaxThemeDef("nord", "Nord", fixed(
            0xFF2E3440, 0xFFD8DEE9, 0xFF2E3440, 0xFF4C566A, 0xFF3B4252, 0x66434C5E, 0xFF88C0D0,
            0xFF81A1C1, 0xFF8FBCBB, 0xFFA3BE8C, 0xFFB48EAD, 0xFF616E88, 0xFF88C0D0,
            0xFF81A1C1, 0xFFB48EAD, 0xFFD08770, 0xFF81A1C1, 0xFF8FBCBB)),
        SyntaxThemeDef("gruvbox", "Gruvbox", fixed(
            0xFF282828, 0xFFEBDBB2, 0xFF282828, 0xFF7C6F64, 0xFF32302F, 0x66504945, 0xFFEBDBB2,
            0xFFFB4934, 0xFFFABD2F, 0xFFB8BB26, 0xFFD3869B, 0xFF928374, 0xFF8EC07C,
            0xFFFE8019, 0xFFD3869B, 0xFF83A598, 0xFFFB4934, 0xFFFABD2F)),
        SyntaxThemeDef("forest", "Лесная ночь", fixed(
            0xFF0E1A12, 0xFFD6EAD8, 0xFF0E1A12, 0xFF4E6B55, 0xFF15261A, 0x662E5A3A, 0xFF7BE08F,
            0xFF7BE08F, 0xFF9FD6F0, 0xFFE8D58A, 0xFFF0A97B, 0xFF5E7D64, 0xFFB7F0A0,
            0xFF9BC4A2, 0xFFF0A97B, 0xFFC9A0F0, 0xFF7BE08F, 0xFFE8D58A)),
        SyntaxThemeDef("github", "GitHub Light", fixed(
            0xFFFFFFFF, 0xFF24292F, 0xFFFFFFFF, 0xFF8C959F, 0xFFF6F8FA, 0x550969DA, 0xFF0969DA,
            0xFFCF222E, 0xFF953800, 0xFF0A3069, 0xFF0550AE, 0xFF6E7781, 0xFF8250DF,
            0xFF24292F, 0xFF0550AE, 0xFF8250DF, 0xFF116329, 0xFF0550AE)),
        SyntaxThemeDef("solarized", "Solarized Light", fixed(
            0xFFFDF6E3, 0xFF657B83, 0xFFFDF6E3, 0xFF93A1A1, 0xFFEEE8D5, 0x55268BD2, 0xFF268BD2,
            0xFF859900, 0xFFB58900, 0xFF2AA198, 0xFFD33682, 0xFF93A1A1, 0xFF268BD2,
            0xFF657B83, 0xFFCB4B16, 0xFF6C71C4, 0xFF268BD2, 0xFFB58900)),
        SyntaxThemeDef("paper", "Бумага", fixed(
            0xFFF7F3EA, 0xFF2B2B2B, 0xFFF7F3EA, 0xFFA8A29A, 0xFFEFE9DC, 0x55A0C4A8, 0xFF2E8B45,
            0xFF2E6B3F, 0xFF7A4F1E, 0xFF8A3B12, 0xFF5B3EA0, 0xFF9A948A, 0xFF1F5C99,
            0xFF2B2B2B, 0xFF5B3EA0, 0xFF7A4F1E, 0xFF2E6B3F, 0xFF7A4F1E)),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}

