package com.sinhala.keyboard

import android.annotation.SuppressLint
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
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class KeyboardService : InputMethodService() {

    private var audioManager: AudioManager? = null
    private var mode = "en"
    private var shift = false
    private var romanBuffer = ""
    private var clipboardManager: ClipboardManager? = null
    
    // Overlay එක පෙන්නන්න මේක පාවිච්චි කරනවා
    private var mainContainer: FrameLayout? = null

    // 🎨 Colors (Flashboard Dark Theme)
    private val bgColor = Color.parseColor("#000000")
    private val keyBgColor = Color.parseColor("#1C1C1E")
    private val keyPressedColor = Color.parseColor("#3A3A3C")
    private val modKeyBgColor = Color.parseColor("#2C2C2E")
    private val textColor = Color.WHITE
    private val subTextColor = Color.parseColor("#888888")
    private val accentColor = Color.parseColor("#8B5CF6")

    // Transliteration Maps
    private val vowels = mapOf("aa" to "ආ", "aae" to "ඈ", "ae" to "ඇ", "ai" to "ඓ", "au" to "ඖ", "ii" to "ඊ", "uu" to "ඌ", "a" to "අ", "i" to "ඉ", "u" to "උ", "e" to "එ", "E" to "ඒ", "o" to "ඔ", "O" to "ඕ")
    private val cons = mapOf("kh" to "ඛ", "gh" to "ඝ", "ng" to "ං", "ch" to "ඡ", "jh" to "ඣ", "ny" to "ඤ", "Th" to "ඨ", "Dh" to "ඪ", "th" to "ථ", "dh" to "ධ", "ph" to "ඵ", "bh" to "භ", "sh" to "ශ", "k" to "ක", "g" to "ග", "c" to "ච", "j" to "ජ", "T" to "ට", "D" to "ඩ", "N" to "ණ", "t" to "ත", "d" to "ද", "n" to "න", "p" to "ප", "b" to "බ", "m" to "ම", "y" to "ය", "r" to "ර", "l" to "ල", "L" to "ළ", "v" to "ව", "w" to "ව", "S" to "ෂ", "s" to "ස", "h" to "හ", "f" to "ෆ")
    private val signs = mapOf("aa" to "ා", "aae" to "ෑ", "ae" to "ැ", "ai" to "ෛ", "au" to "ෞ", "ii" to "ී", "uu" to "ූ", "a" to "", "i" to "ි", "u" to "ු", "e" to "ෙ", "E" to "ේ", "o" to "ො", "O" to "ෝ")

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        clipboardManager?.addPrimaryClipChangedListener {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                if (text.isNotEmpty()) Prefs.addClipboardItem(text)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        Prefs.refresh()
        mode = Prefs.language.value

        // ⭐ Crash නොවී Popup පෙන්නන්න මේ FrameLayout එක පාවිච්චි කරනවා
        val frameRoot = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        mainContainer = frameRoot

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(16))
        }

        root.addView(createToolbar())

        val keyboardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val rows = listOf(
            listOf("q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5", "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0"),
            listOf("a" to "@", "s" to "#", "d" to "$", "f" to "%", "g" to "&", "h" to "*", "j" to "-", "k" to "+", "l" to "("),
            listOf("SHIFT" to "", "z" to ")", "x" to "\"", "c" to ":", "v" to ";", "b" to "!", "n" to "?", "m" to "/", "DEL" to ""),
            listOf("123" to "", "GLOBE" to "", "comma" to "", "space" to "", "period" to "", "ENTER" to "")
        )

        rows.forEachIndexed { index, row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                val height = if (index == 3) dpToPx(54) else dpToPx(52)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
            }

            row.forEach { (key, subText) ->
                val weight = when (key) {
                    "space" -> 4.0f
                    "SHIFT", "DEL", "ENTER" -> 1.4f
                    "123", "GLOBE", "comma", "period" -> 1.1f
                    else -> 1f
                }
                rowLayout.addView(createKeyView(key, subText, weight))
            }
            keyboardContainer.addView(rowLayout)
        }

        root.addView(keyboardContainer)
        frameRoot.addView(root)
        
        return frameRoot
    }

    private fun createToolbar(): LinearLayout {
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48))
        }

        fun createToolBtn(text: String, onClick: (() -> Unit)? = null): TextView {
            return TextView(this).apply {
                this.text = text
                setTextColor(textColor)
                textSize = if(text.length > 1) 14f else 20f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(modKeyBgColor)
                }
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f).apply {
                    setMargins(dpToPx(6), 0, dpToPx(6), 0)
                }
                setOnClickListener { onClick?.invoke() }
            }
        }

        val langBtn = createToolBtn(if (mode == "en") "En" else "සිං") {
            commitBuffer()
            mode = if (mode == "en") "si" else "en"
            Prefs.setLanguage(mode)
            setInputView(onCreateInputView())
        }
        
        val emojiBtn = createToolBtn("😊")
        
        val clipBtn = createToolBtn("📋") {
            showClipboardHistory()
        }

        val settingsBtn = createToolBtn("⚙️")

        toolbar.addView(langBtn)
        toolbar.addView(emojiBtn)
        toolbar.addView(clipBtn)
        toolbar.addView(settingsBtn)

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar)
            setPadding(0, 0, 0, dpToPx(10))
        }

        return wrapper
    }

    // ⭐ අලුත් Clipboard History Popup එක (Crash නොවෙන විදිය)
    private fun showClipboardHistory() {
        val history = Prefs.clipboardHistory.value
        
        // මේක තමයි අලුත් Overlay එක
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F0000000")) // තද කළු පසුබිමක්
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            isClickable = true // යට තියෙන keys එබෙන එක නවත්තන්න
        }

        // Header එක (Title + Clear + Close)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(12))
        }

        val title = TextView(this).apply {
            text = "Clipboard"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val clearBtn = TextView(this).apply {
            text = "Clear  "
            setTextColor(Color.parseColor("#FF453A"))
            textSize = 14f
            setOnClickListener {
                Prefs.clearClipboardHistory()
                mainContainer?.removeView(overlay)
                showClipboardHistory() // ආයෙ Refresh කරන්න
            }
        }

        val closeBtn = TextView(this).apply {
            text = "Close"
            setTextColor(accentColor)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { mainContainer?.removeView(overlay) }
        }

        header.addView(title)
        header.addView(clearBtn)
        header.addView(closeBtn)
        overlay.addView(header)

        val scrollView = ScrollView(this)
        val listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        if (history.isEmpty()) {
             val emptyText = TextView(this).apply {
                 text = "No copied items yet."
                 setTextColor(Color.GRAY)
                 textSize = 14f
                 gravity = Gravity.CENTER
                 setPadding(0, dpToPx(20), 0, 0)
             }
             listLayout.addView(emptyText)
        } else {
            history.forEach { item ->
                val tv = TextView(this).apply {
                    text = item
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                    background = GradientDrawable().apply {
                        setColor(keyBgColor)
                        cornerRadius = dpToPx(8).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dpToPx(8))
                    }
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setOnClickListener {
                        commitBuffer()
                        currentInputConnection?.commitText(item, 1)
                        mainContainer?.removeView(overlay) // එබුවම Popup එක අයින් වෙනවා
                    }
                }
                listLayout.addView(tv)
            }
        }
        
        scrollView.addView(listLayout)
        overlay.addView(scrollView)

        // අපේ ප්‍රධාන Frame එකට මේක එකතු කරනවා
        mainContainer?.addView(overlay)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(key: String, subText: String, weight: Float): FrameLayout {
        val isMod = key in listOf("SHIFT", "DEL", "ENTER", "123", "GLOBE", "comma", "period", "space")
        
        val container = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT, weight).apply {
                setMargins(dpToPx(3), 0, dpToPx(3), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(if (isMod) modKeyBgColor else keyBgColor)
            }
            isClickable = true
        }

        val mainText = TextView(this).apply {
            text = getKeyLabel(key)
            setTextColor(textColor)
            textSize = if (key == "space") 11f else 20f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            if (subText.isNotEmpty()) setPadding(0, dpToPx(6), 0, 0) 
            
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        if (subText.isNotEmpty()) {
            val subTextView = TextView(this).apply {
                text = subText
                setTextColor(subTextColor)
                textSize = 10f
                gravity = Gravity.TOP or Gravity.END
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dpToPx(4), dpToPx(6), 0)
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
            "space" -> "" 
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
            "123", "GLOBE" -> { /* Later */ }
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
