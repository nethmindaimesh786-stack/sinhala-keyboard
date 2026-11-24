package com.sinhala.keyboard

import androidx.compose.ui.graphics.Color

data class KbTheme(
    val id: String,
    val name: String,
    val description: String,
    val kbBg: List<Color>,
    val keyBg: Color,
    val keyBgMod: Color,
    val keyText: Color,
    val keyTextMod: Color,
    val accent: Color,
    val accentGradient: List<Color>,
    val langKeyBg: List<Color>,
    val border: Color,
    val isLight: Boolean = false
)

object Themes {
    val violet = KbTheme(
        id = "violet",
        name = "Frosted Violet",
        description = "Premium glass · Soft gradients",
        kbBg = listOf(Color(0xFF14141F), Color(0xFF08080F)),
        keyBg = Color(0x12FFFFFF),
        keyBgMod = Color(0x1FFFFFFF),
        keyText = Color(0xFFF0F0F5),
        keyTextMod = Color(0xFFD0D0DA),
        accent = Color(0xFF8B5CF6),
        accentGradient = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
        langKeyBg = listOf(Color(0x4D8B5CF6), Color(0x3806B6D4)),
        border = Color(0x17FFFFFF)
    )

    val neon = KbTheme(
        id = "neon",
        name = "Neon Cyber",
        description = "Futuristic · Glowing edges",
        kbBg = listOf(Color(0xFF050510), Color(0xFF020208)),
        keyBg = Color(0xFF0A1525),
        keyBgMod = Color(0xFF0F1A2E),
        keyText = Color(0xFF00FFFF),
        keyTextMod = Color(0xFFFF00FF),
        accent = Color(0xFF00FFFF),
        accentGradient = listOf(Color(0xFF00FFFF), Color(0xFF00AACC)),
        langKeyBg = listOf(Color(0xFF0A2030), Color(0xFF201020)),
        border = Color(0x5900FFFF)
    )

    val ios = KbTheme(
        id = "ios",
        name = "Minimal iOS",
        description = "Clean · Familiar · Fast",
        kbBg = listOf(Color(0xFFD1D3D9), Color(0xFFC5C7CD)),
        keyBg = Color(0xFFFFFFFF),
        keyBgMod = Color(0xFFADB1B8),
        keyText = Color(0xFF000000),
        keyTextMod = Color(0xFF000000),
        accent = Color(0xFF5B6CF0),
        accentGradient = listOf(Color(0xFF5B6CF0), Color(0xFF4040C0)),
        langKeyBg = listOf(Color(0xFFADB1B8), Color(0xFFADB1B8)),
        border = Color(0xFFB8BBC2),
        isLight = true
    )

    val dark = KbTheme(
        id = "dark",
        name = "OLED Dark",
        description = "Pure black · Battery saver",
        kbBg = listOf(Color(0xFF000000), Color(0xFF000000)),
        keyBg = Color(0xFF1A1A1A),
        keyBgMod = Color(0xFF2A2A2A),
        keyText = Color(0xFFFFFFFF),
        keyTextMod = Color(0xFFE0E0E0),
        accent = Color(0xFF00BCD4),
        accentGradient = listOf(Color(0xFF00BCD4), Color(0xFF0088AA)),
        langKeyBg = listOf(Color(0xFF1A3A44), Color(0xFF0F2A34)),
        border = Color(0xFF2A2A2A)
    )

    val sunrise = KbTheme(
        id = "sunrise",
        name = "Sunrise Warm",
        description = "Orange · Coral · Friendly",
        kbBg = listOf(Color(0xFF2A1810), Color(0xFF1A0E08)),
        keyBg = Color(0x22FF9955),
        keyBgMod = Color(0x33FF9955),
        keyText = Color(0xFFFFEEDD),
        keyTextMod = Color(0xFFFFCC99),
        accent = Color(0xFFFF7043),
        accentGradient = listOf(Color(0xFFFF7043), Color(0xFFFF3D00)),
        langKeyBg = listOf(Color(0x55FF7043), Color(0x44FFB74D)),
        border = Color(0x33FF9955)
    )

    val all = listOf(violet, neon, ios, dark, sunrise)

    fun byId(id: String): KbTheme = all.firstOrNull { it.id == id } ?: violet
}
