package com.sinhala.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val VOWELS = mapOf(
    "aa" to "\u0D86", "aae" to "\u0D88", "ae" to "\u0D87", "ai" to "\u0D93", "au" to "\u0D96",
    "ii" to "\u0D8A", "uu" to "\u0D8C", "a" to "\u0D85", "i" to "\u0D89", "u" to "\u0D8B",
    "e" to "\u0D91", "E" to "\u0D92", "o" to "\u0D94", "O" to "\u0D95"
)
private val CONS = mapOf(
    "kh" to "\u0D9B", "gh" to "\u0D9D", "ng" to "\u0D82", "ch" to "\u0DA1", "jh" to "\u0DA3",
    "ny" to "\u0DA4", "Th" to "\u0DA8", "Dh" to "\u0DAA", "th" to "\u0DAE", "dh" to "\u0DB0",
    "ph" to "\u0DB5", "bh" to "\u0DB7", "sh" to "\u0DC1",
    "k" to "\u0D9A", "g" to "\u0D9C", "c" to "\u0DA0", "j" to "\u0DA2",
    "T" to "\u0DA7", "D" to "\u0DA9", "N" to "\u0DAB", "t" to "\u0DAD", "d" to "\u0DAF",
    "n" to "\u0DB1", "p" to "\u0DB4", "b" to "\u0DB6", "m" to "\u0DB8", "y" to "\u0DBA",
    "r" to "\u0DBB", "l" to "\u0DBD", "L" to "\u0DC5", "v" to "\u0DC0", "w" to "\u0DC0",
    "S" to "\u0DC2", "s" to "\u0DC3", "h" to "\u0DC4", "f" to "\u0DC6"
)
private val SIGNS = mapOf(
    "aa" to "\u0DCF", "aae" to "\u0DD1", "ae" to "\u0DD0", "ai" to "\u0DDB", "au" to "\u0DDE",
    "ii" to "\u0DD3", "uu" to "\u0DD6", "a" to "", "i" to "\u0DD2", "u" to "\u0DD4",
    "e" to "\u0DD9", "E" to "\u0DDA", "o" to "\u0DDC", "O" to "\u0DDD"
)
private val VK = VOWELS.keys.sortedByDescending { it.length }
private val CK = CONS.keys.sortedByDescending { it.length }
private val SK = SIGNS.keys.sortedByDescending { it.length }

private fun transliterate(w: String): String {
    val o = StringBuilder()
    var i = 0
    while (i < w.length) {
        var c: String? = null
        for (k in CK) if (w.startsWith(k, i)) { c = k; break }
        if (c != null) {
            o.append(CONS[c])
            i += c.length
            var s: String? = null
            for (k in SK) if (w.startsWith(k, i)) { s = k; break }
            if (s != null) { o.append(SIGNS[s]); i += s.length }
            else o.append("\u0DCA")
        } else {
            var v: String? = null
            for (k in VK) if (w.startsWith(k, i)) { v = k; break }
            if (v != null) { o.append(VOWELS[v]); i += v.length }
            else { o.append(w[i]); i++ }
        }
    }
    return o.toString()
}

@Composable
fun KeyboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0A0A14),
            surface = Color(0xFF1A1A24),
            onSurface = Color(0xFFF0F0F5)
        ),
        content = content
    )
}

@Composable
fun KeyboardUI(
    onCommit: (String) -> Unit,
    onComposing: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onKeyFeedback: () -> Unit
) {
    val themeId by Prefs.theme.collectAsState()
    val defaultLang by Prefs.language.collectAsState()
    val theme = Themes.byId(themeId)

    var mode by remember { mutableStateOf(defaultLang) }
    var buffer by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf(false) }

    LaunchedEffect(defaultLang) {
        mode = defaultLang
    }

    fun finalize() {
        if (buffer.isNotEmpty()) {
            onCommit(transliterate(buffer))
            buffer = ""
        }
    }

    val rows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"),
        listOf("LANG", "space", "ENTER")
    )

    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(theme.kbBg))
            .padding(horizontal = 5.dp, vertical = 6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { key ->
                        val weight = when (key) {
                            "space" -> 4.2f
                            "SHIFT", "DEL", "ENTER" -> 1.4f
                            "LANG" -> 1.7f
                            else -> 1f
                        }
                        val isMod = key in listOf("SHIFT", "DEL", "ENTER")
                        val isLang = key == "LANG"
                        val isSpace = key == "space"

                        val display = when {
                            key == "LANG" -> if (mode == "en") "EN/SI" else "SI/EN"
                            key == "SHIFT" -> "^"
                            key == "DEL" -> "<"
                            key == "ENTER" -> "->"
                            mode == "en" && shift && key.length == 1 && key[0].isLetter() ->
                                key.uppercase()
                            else -> key
                        }

                        KeyButton(
                            label = display,
                            theme = theme,
                            modifier = Modifier.weight(weight),
                            isMod = isMod,
                            isLang = isLang,
                            isSpace = isSpace,
                            shiftActive = key == "SHIFT" && shift,
                            onPress = {
                                onKeyFeedback()
                                when (key) {
                                    "DEL" -> {
                                        if (mode == "si" && buffer.isNotEmpty()) {
                                            buffer = buffer.dropLast(1)
                                            onComposing(
                                                if (buffer.isEmpty()) "" else transliterate(buffer)
                                            )
                                        } else onDelete()
                                    }
                                    "SHIFT" -> shift = !shift
                                    "LANG" -> {
                                        finalize()
                                        mode = if (mode == "en") "si" else "en"
                                        shift = false
                                    }
                                    "space" -> {
                                        finalize()
                                        onCommit(" ")
                                    }
                                    "ENTER" -> {
                                        finalize()
                                        onEnter()
                                    }
                                    else -> {
                                        if (mode == "si") {
                                            buffer += key.lowercase()
                                            onComposing(transliterate(buffer))
                                        } else {
                                            onCommit(if (shift) key.uppercase() else key.lowercase())
                                            if (shift) shift = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    theme: KbTheme,
    modifier: Modifier = Modifier,
    isMod: Boolean = false,
    isLang: Boolean = false,
    isSpace: Boolean = false,
    shiftActive: Boolean = false,
    onPress: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val bgBrush: Brush = when {
        pressed -> Brush.linearGradient(theme.accentGradient)
        isLang -> Brush.linearGradient(theme.langKeyBg)
        isMod || isSpace -> Brush.linearGradient(listOf(theme.keyBgMod, theme.keyBgMod))
        else -> Brush.linearGradient(listOf(theme.keyBg, theme.keyBg))
    }
    val borderColor = when {
        pressed -> Color.Transparent
        isLang -> theme.accent
        shiftActive -> theme.accent
        else -> theme.border
    }
    val textColor = when {
        pressed -> Color.White
        isMod && theme.keyTextMod != theme.keyText -> theme.keyTextMod
        else -> theme.keyText
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(9.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        onPress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = when {
                isSpace -> 11.sp
                isMod -> 14.sp
                isLang -> 11.sp
                else -> 17.sp
            },
            maxLines = 1,
            softWrap = false,
            fontWeight = if (isLang) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
