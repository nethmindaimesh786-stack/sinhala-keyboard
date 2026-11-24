package com.sinhala.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardService : InputMethodService() {

    private var audioManager: AudioManager? = null
    private var currentTheme = Themes.violet

    private var mode = "en"
    private var shift = false
    private var romanBuffer = ""

    // Transliteration Maps
    private val vowels = mapOf(
        "aa" to "ආ", "aae" to "ඈ", "ae" to "ඇ", "ai" to "ඓ", "au" to "ඖ",
        "ii" to "ඊ", "uu" to "ඌ", "a" to "අ", "i" to "ඉ", "u" to "උ",
        "e" to "එ", "E" to "ඒ", "o" to "ඔ", "O" to "ඕ"
    )
    private val cons = mapOf(
        "kh" to "ඛ", "gh" to "ඝ", "ng" to "ං", "ch" to "ඡ", "jh" to "ඣ", "ny" to "ඤ",
        "Th" to "ඨ", "Dh" to "ඪ", "th" to "ථ", "dh" to "ධ", "ph" to "ඵ", "bh" to "භ", "sh" to "ශ",
        "k" to "ක", "g" to "ග", "c" to "ච", "j" to "ජ", "T" to "ට", "D" to "ඩ", "N" to "ණ",
        "t" to "ත", "d" to "ද", "n" to "න", "p" to "ප", "b" to "බ", "m" to "ම", "y" to "ය",
        "r" to "ර", "l" to "ල", "L" to "ළ", "v" to "ව", "w" to "ව", "S" to "ෂ", "s" to "ස",
        "h" to "හ", "f" to "ෆ"
    )
    private val signs = mapOf(
        "aa" to "ා", "aae" to "ෑ", "ae" to "ැ", "ai" to "ෛ", "au" to "ෞ", "ii" to "ී",
        "uu" to "ූ", "a" to "", "i" to "ි", "u" to "ු", "e" to "ෙ", "E" to "ේ", "o" to "ො", "O" to "ෝ"
    )

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        Prefs.refresh()
        currentTheme = Themes.byId(Prefs.theme.value)
        mode = Prefs.language.value

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(currentTheme.kbBg[1].value.toInt())
            setPadding(10, 15, 10, 25)
        }

        val rows = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"),
            listOf("LANG", "space", "ENTER")
        )

        rows.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 0, 0, 12)
            }

            row.forEach { key ->
                val isSpecial = key in listOf("SHIFT", "DEL", "ENTER", "LANG", "space")
                val weight = when (key) {
                    "space" -> 4.2f
                    "SHIFT", "DEL", "ENTER" -> 1.4f
                    "LANG" -> 1.7f
                    else -> 1f
                }

                val keyView = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 130, weight).apply {
                        setMargins(6, 0, 6, 0)
                    }
                    gravity = Gravity.CENTER
                    text = getKeyLabel(key)
                    setTextColor(currentTheme.keyText.value.toInt())
                    textSize = if (key == "space") 12f else if (isSpecial) 14f else 20f
                    isClickable = true
                    
                    // Background Shape (Rounded)
                    background = createKeyBackground(isSpecial, key == "LANG" || (key == "SHIFT" && shift))
                }

                // Touch Feedback (No Lag)
                keyView.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                            (v.background as GradientDrawable).setColor(currentTheme.accent.value.toInt())
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                            v.background = createKeyBackground(isSpecial, key == "LANG" || (key == "SHIFT" && shift))
                            if (event.action == MotionEvent.ACTION_UP) {
                                handleKeyPress(key)
                            }
                        }
                    }
                    true
                }

                rowLayout.addView(keyView)
            }
            root.addView(rowLayout)
        }

        return root
    }

    private fun getKeyLabel(key: String): String {
        return when (key) {
            "SHIFT" -> if (shift) "⬆" else "⇧"
            "DEL" -> "⌫"
            "ENTER" -> "↵"
            "LANG" -> if (mode == "en") "EN/සිං" else "සිං/EN"
            "space" -> "space"
            else -> if (mode == "en" && shift) key.uppercase() else key
        }
    }

    private fun createKeyBackground(isSpecial: Boolean, isHighlight: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 25f
            if (isHighlight) {
                colors = intArrayOf(currentTheme.accentGradient[0].value.toInt(), currentTheme.accentGradient[1].value.toInt())
                orientation = GradientDrawable.Orientation.TL_BR
            } else if (isSpecial) {
                setColor(currentTheme.keyBgMod.value.toInt())
                setStroke(2, currentTheme.border.value.toInt())
            } else {
                setColor(currentTheme.keyBg.value.toInt())
                setStroke(2, currentTheme.border.value.toInt())
            }
        }
    }

    private fun handleKeyPress(key: String) {
        doFeedback()
        val ic = currentInputConnection ?: return

        when (key) {
            "SHIFT" -> {
                shift = !shift
                recreateInputView()
            }
            "LANG" -> {
                commitBuffer()
                mode = if (mode == "en") "si" else "en"
                shift = false
                recreateInputView()
            }
            "DEL" -> {
                if (mode == "si" && romanBuffer.isNotEmpty()) {
                    romanBuffer = romanBuffer.dropLast(1)
                    ic.setComposingText(transliterate(romanBuffer), 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            "space" -> {
                commitBuffer()
                ic.commitText(" ", 1)
            }
            "ENTER" -> {
                commitBuffer()
                sendEnterKey(ic)
            }
            else -> {
                if (mode == "si") {
                    romanBuffer += key.lowercase()
                    ic.setComposingText(transliterate(romanBuffer), 1)
                } else {
                    ic.commitText(if (shift) key.uppercase() else key.lowercase(), 1)
                    if (shift) {
                        shift = false
                        recreateInputView()
                    }
                }
            }
        }
    }

    private fun commitBuffer() {
        if (romanBuffer.isNotEmpty()) {
            currentInputConnection?.commitText(transliterate(romanBuffer), 1)
            romanBuffer = ""
        }
    }

    private fun recreateInputView() {
        // Keyboard එකේ UI එක update වෙන්න අලුතෙන් view එක හදනවා
        setInputView(onCreateInputView())
    }

    private fun transliterate(word: String): String {
        var out = ""
        var i = 0
        val sortedCons = cons.keys.sortedByDescending { it.length }
        val sortedVowels = vowels.keys.sortedByDescending { it.length }
        val sortedSigns = signs.keys.sortedByDescending { it.length }

        while (i < word.length) {
            val c = sortedCons.firstOrNull { word.startsWith(it, i) }
            if (c != null) {
                out += cons[c]
                i += c.length
                val s = sortedSigns.firstOrNull { word.startsWith(it, i) }
                if (s != null) {
                    out += signs[s]
                    i += s.length
                } else {
                    out += "්"
                }
            } else {
                val v = sortedVowels.firstOrNull { word.startsWith(it, i) }
                if (v != null) {
                    out += vowels[v]
                    i += v.length
                } else {
                    out += word[i]
                    i++
                }
            }
        }
        return out
    }

    private fun sendEnterKey(ic: android.view.inputmethod.InputConnection) {
        val opts = currentInputEditorInfo?.imeOptions ?: 0
        if ((opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            ic.commitText("\n", 1)
        } else {
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun doFeedback() {
        if (Prefs.vibrateOn.value) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(10, 255))
                } else {
                    @Suppress("DEPRECATION")
                    (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(10)
                }
            } catch (_: Exception) {}
        }
        if (Prefs.soundOn.value) {
            try { audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.5f) } catch (_: Exception) {}
        }
    }
}
