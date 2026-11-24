package com.sinhala.keyboard

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb

class KeyboardService : InputMethodService() {

    private var audioManager: AudioManager? = null
    private var mode = "en"
    private var shift = false
    private var romanBuffer = ""
    private var clipboardManager: ClipboardManager? = null

    private val bgColor = Color.parseColor("#0A0A0A")
    private val keyBgColor = Color.parseColor("#2C2C2E")
    private val keyPressedColor = Color.parseColor("#505050")
    private val modKeyBgColor = Color.parseColor("#1C1C1E")
    private val textColor = Color.WHITE
    private val accentColor = Color.parseColor("#8B5CF6")

    private val vowels = mapOf("aa" to "ආ", "aae" to "ඈ", "ae" to "ඇ", "ai" to "ඓ", "au" to "ඖ", "ii" to "ඊ", "uu" to "ඌ", "a" to "අ", "i" to "ඉ", "u" to "උ", "e" to "එ", "E" to "ඒ", "o" to "ඔ", "O" to "ඕ")
    private val cons = mapOf("kh" to "ඛ", "gh" to "ඝ", "ng" to "ං", "ch" to "ඡ", "jh" to "ඣ", "ny" to "ඤ", "Th" to "ඨ", "Dh" to "ඪ", "th" to "ථ", "dh" to "ධ", "ph" to "ඵ", "bh" to "භ", "sh" to "ශ", "k" to "ක", "g" to "ග", "c" to "ච", "j" to "ජ", "T" to "ට", "D" to "ඩ", "N" to "ණ", "t" to "ත", "d" to "ද", "n" to "න", "p" to "ප", "b" to "බ", "m" to "ම", "y" to "ය", "r" to "ර", "l" to "ල", "L" to "ළ", "v" to "ව", "w" to "ව", "S" to "ෂ", "s" to "ස", "h" to "හ", "f" to "ෆ")
    private val signs = mapOf("aa" to "ා", "aae" to "ෑ", "ae" to "ැ", "ai" to "ෛ", "au" to "ෞ", "ii" to "ී", "uu" to "ූ", "a" to "", "i" to "ි", "u" to "ු", "e" to "ෙ", "E" to "ේ", "o" to "ො", "O" to "ෝ")

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // ⭐ අලුතෙන් යමක් Copy වෙනකොට ඒක අපේ history එකට එකතු කරනවා
        clipboardManager?.addPrimaryClipChangedListener {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    Prefs.addClipboardItem(text)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        Prefs.refresh()
        mode = Prefs.language.value

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(0, 10, 0, 20)
        }

        root.addView(createToolbar())

        val keyboardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 0)
        }

        val rows = listOf(
            listOf("q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5", "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0"),
            listOf("a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&", "h" to "*", "j" to "-", "k" to "+", "l" to "("),
            listOf("SHIFT" to "", "z" to ")", "x" to "\"", "c" to ":", "v" to ";", "b" to "!", "n" to "?", "m" to "/", "DEL" to ""),
            listOf("123" to "", "GLOBE" to "", "comma" to "", "space" to "Sinhala Keyboard", "period" to "", "ENTER" to "")
        )

        rows.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { setMargins(0, 0, 0, 10) }
            }

            row.forEach { (key, subText) ->
                val weight = when (key) {
                    "space" -> 4.5f
                    "SHIFT", "DEL", "ENTER" -> 1.5f
                    "123", "GLOBE", "comma", "period" -> 1.2f
                    else -> 1f
                }
                rowLayout.addView(createKeyView(key, subText, weight))
            }
            keyboardContainer.addView(rowLayout)
        }

        root.addView(keyboardContainer)
        return root
    }

    private fun createToolbar(): LinearLayout {
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 0, 16, 15)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120)
        }

        val langBtn = TextView(this).apply {
            text = if (mode == "en") "En" else "සිං"
            setTextColor(textColor)
            textSize = 16f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(keyBgColor); setStroke(3, Color.GRAY) }
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(0, 0, 20, 0) }
            setOnClickListener { 
                commitBuffer(); mode = if (mode == "en") "si" else "en"; Prefs.setLanguage(mode); setInputView(onCreateInputView()) 
            }
        }
        
        val emojiBtn = TextView(this).apply {
            text = "😊"
            textSize = 24f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(keyBgColor) }
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(0, 0, 20, 0) }
        }

        // ⭐ අලුත් Clipboard බටන් එක
        val clipBtn = TextView(this).apply {
            text = "📋"
            textSize = 24f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(keyBgColor) }
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(0, 0, 20, 0) }
            setOnClickListener { showClipboardHistory() }
        }

        val settingsBtn = TextView(this).apply {
            text = "⚙️"
            textSize = 24f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(keyBgColor) }
            layoutParams = LinearLayout.LayoutParams(120, 120)
        }

        toolbar.addView(langBtn)
        toolbar.addView(emojiBtn)
        toolbar.addView(clipBtn)
        toolbar.addView(settingsBtn)
        return toolbar
    }

    // ⭐ Clipboard History එක පෙන්නන Popup එක
    private fun showClipboardHistory() {
        val history = Prefs.clipboardHistory.value
        if (history.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No copied items yet.\nCopy something from any app!"
                setTextColor(Color.WHITE)
                setPadding(40, 40, 40, 40)
                textSize = 16f
                gravity = Gravity.CENTER
            }
            AlertDialog.Builder(this)
                .setTitle("Clipboard History")
                .setView(tv)
                .setPositiveButton("Close", null)
                .show()
            return
        }

        // History එක තියෙනවා නම් List එකක් හදනවා
        val scrollView = ScrollView(this)
        val listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        history.forEach { text ->
            val itemView = TextView(this).apply {
                this.text = text
                setTextColor(Color.BLACK)
                textSize = 16f
                setPadding(30, 30, 30, 30)
                setBackgroundColor(Color.parseColor("#F0F0F0"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 15)
                }
                
                // ඒක ක්ලික් කරාම type වෙනවා
                setOnClickListener {
                    commitBuffer()
                    currentInputConnection?.commitText(text, 1)
                    // Popup එක වහන්න
                    (parent?.parent as? AlertDialog)?.dismiss()
                }
            }
            listLayout.addView(itemView)
        }
        scrollView.addView(listLayout)

        AlertDialog.Builder(this)
            .setTitle("Clipboard History (Tap to paste)")
            .setView(scrollView)
            .setNegativeButton("Clear All") { _, _ ->
                Prefs.clipboardHistory.value = emptyList()
                Prefs.setTheme(Prefs.theme.value) // trick to save empty list
                // වෙනම clear method එකක් නැති නිසා මෙහෙම කළා
                prefs?.edit()?.putString("clipboard_history", "")?.apply()
            }
            .setPositiveButton("Close", null)
            .show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(key: String, subText: String, weight: Float): FrameLayout {
        val isMod = key in listOf("SHIFT", "DEL", "ENTER", "123", "GLOBE", "comma", "period", "space")
        val container = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT, weight).apply { setMargins(6, 0, 6, 0) }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f
                setColor(if (isMod) modKeyBgColor else keyBgColor)
            }
            isClickable = true
        }

        val mainText = TextView(this).apply {
            text = getKeyLabel(key)
            setTextColor(textColor)
            textSize = if (key == "space") 14f else 22f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        if (subText.isNotEmpty()) {
            val subTextView = TextView(this).apply {
                text = subText
                setTextColor(Color.LTGRAY)
                textSize = 12f
                gravity = Gravity.TOP or Gravity.END
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 15, 15, 0)
                    gravity = Gravity.TOP or Gravity.END
                }
            }
            container.addView(subTextView)
        }

        container.addView(mainText)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(50).start()
                    (v.background as GradientDrawable).setColor(if (key == "space") accentColor else keyPressedColor)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                    (v.background as GradientDrawable).setColor(if (isMod) modKeyBgColor else keyBgColor)
                    if (event.action == MotionEvent.ACTION_UP) handleKeyPress(key)
                }
            }
            true
        }

        return container
    }

    private fun getKeyLabel(key: String): String {
        return when (key) {
            "SHIFT" -> if (shift) "⬆" else "⇧"
            "DEL" -> "⌫"
            "ENTER" -> "↵"
            "123" -> "123"
            "GLOBE" -> "🌐"
            "comma" -> ","
            "period" -> "."
            "space" -> "Sinhala Keyboard"
            else -> if (mode == "en" && shift) key.uppercase() else key
        }
    }

    private fun handleKeyPress(key: String) {
        doFeedback()
        val ic = currentInputConnection ?: return

        when (key) {
            "SHIFT" -> { shift = !shift; setInputView(onCreateInputView()) }
            "DEL" -> {
                if (mode == "si" && romanBuffer.isNotEmpty()) {
                    romanBuffer = romanBuffer.dropLast(1)
                    ic.setComposingText(transliterate(romanBuffer), 1)
                } else ic.deleteSurroundingText(1, 0)
            }
            "space" -> { commitBuffer(); ic.commitText(" ", 1) }
            "ENTER" -> { commitBuffer(); sendEnterKey(ic) }
            "comma" -> { commitBuffer(); ic.commitText(",", 1) }
            "period" -> { commitBuffer(); ic.commitText(".", 1) }
            "123", "GLOBE" -> { /* පස්සේ හදමු */ }
            else -> {
                if (mode == "si") {
                    romanBuffer += key.lowercase()
                    ic.setComposingText(transliterate(romanBuffer), 1)
                } else {
                    ic.commitText(if (shift) key.uppercase() else key.lowercase(), 1)
                    if (shift) { shift = false; setInputView(onCreateInputView()) }
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

    private fun transliterate(word: String): String {
        var out = ""; var i = 0
        val sortedCons = cons.keys.sortedByDescending { it.length }
        val sortedVowels = vowels.keys.sortedByDescending { it.length }
        val sortedSigns = signs.keys.sortedByDescending { it.length }
        while (i < word.length) {
            val c = sortedCons.firstOrNull { word.startsWith(it, i) }
            if (c != null) {
                out += cons[c]; i += c.length
                val s = sortedSigns.firstOrNull { word.startsWith(it, i) }
                if (s != null) { out += signs[s]; i += s.length } else out += "්"
            } else {
                val v = sortedVowels.firstOrNull { word.startsWith(it, i) }
                if (v != null) { out += vowels[v]; i += v.length } else { out += word[i]; i++ }
            }
        }
        return out
    }

    private fun sendEnterKey(ic: android.view.inputmethod.InputConnection) {
        val opts = currentInputEditorInfo?.imeOptions ?: 0
        if ((opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) ic.commitText("\n", 1)
        else {
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
        if (Prefs.soundOn.value) { try { audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.5f) } catch (_: Exception) {} }
    }
}
