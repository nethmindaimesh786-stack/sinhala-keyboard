package com.sinhala.keyboard

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
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
    private var mainContainer: FrameLayout? = null
    private var currentTheme = Themes.violet
    
    // Popup එක සඳහා
    private var activePopup: TextView? = null
    
    // RGB Animation සඳහා
    private var rgbAnimator: ValueAnimator? = null

    // Theme මාරු වෙද්දි අලුත් UI එක එන්න
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "theme") {
            Handler(Looper.getMainLooper()).post {
                setInputView(onCreateInputView())
            }
        }
    }

    // 🎨 Colors
    private val bgColor = Color.parseColor("#000000")
    private val keyBgColor = Color.parseColor("#1C1C1E")
    private val keyPressedColor = Color.parseColor("#3A3A3C")
    private val modKeyBgColor = Color.parseColor("#2C2C2E")
    private val textColor = Color.WHITE
    private val subTextColor = Color.parseColor("#888888")
    private val accentColor = Color.parseColor("#8B5CF6")

    // 🇱🇰 අලුත් සිංහල Transliteration Maps
    private val vowels = mapOf(
        "a" to "අ", "aa" to "ආ", "A" to "ඇ", "ae" to "ඇ", "Aa" to "ඈ", "aae" to "ඈ",
        "i" to "ඉ", "ii" to "ඊ", "ee" to "ඊ", "u" to "උ", "uu" to "ඌ",
        "e" to "එ", "E" to "ඒ", "o" to "ඔ", "O" to "ඕ", "oo" to "ඕ", "au" to "ඖ"
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
        "a" to "", "aa" to "ා", "A" to "ැ", "ae" to "ැ", "Aa" to "ෑ", "aae" to "ෑ",
        "i" to "ි", "ii" to "ී", "ee" to "ේ", "E" to "ේ",
        "u" to "ු", "uu" to "ූ", "e" to "ෙ",
        "o" to "ො", "O" to "ෝ", "oo" to "ෝ", "au" to "ෞ"
    )

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        Prefs.prefs?.registerOnSharedPreferenceChangeListener(prefListener)

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
        currentTheme = Themes.byId(Prefs.theme.value)

        val frameRoot = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        mainContainer = frameRoot

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // RGB නම් පසුබිම animate වෙනවා, නැත්නම් static
            if (currentTheme.id == "rgb") {
                setBackgroundColor(Color.BLACK)
            } else {
                setBackgroundColor(if (currentTheme.isLight) Color.parseColor("#D1D3D9") else bgColor)
            }
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
        
        // RGB Animation පටන් ගන්නවා
        if (currentTheme.id == "rgb") {
            startRgbAnimation(root)
        }

        return frameRoot
    }

    private fun createToolbar(): LinearLayout {
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER // මැදට ගන්නවා
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48))
        }

        fun createCircularBtn(text: String, onClick: (() -> Unit)? = null): FrameLayout {
            val container = FrameLayout(this).apply {
                // රවුම් වෙන්න width සහ height එකම අගයක් දෙනවා
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                    setMargins(dpToPx(6), 0, dpToPx(6), 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (currentTheme.id == "rgb") Color.parseColor("#33FFFFFF") else modKeyBgColor)
                }
                setOnClickListener { onClick?.invoke() }
            }
            val tv = TextView(this).apply {
                this.text = text
                setTextColor(if (currentTheme.isLight) Color.BLACK else textColor)
                textSize = if(text.length > 1) 14f else 18f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            container.addView(tv)
            return container
        }

        // Space එකක් දාලා බටන්ස් ටික සමානව බෙදනවා
        val space = { View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) } }

        toolbar.addView(space())
        toolbar.addView(createCircularBtn(if (mode == "en") "En" else "සිං") {
            commitBuffer()
            mode = if (mode == "en") "si" else "en"
            Prefs.setLanguage(mode)
            setInputView(onCreateInputView())
        })
        toolbar.addView(space())
        toolbar.addView(createCircularBtn("😊"))
        toolbar.addView(space())
        toolbar.addView(createCircularBtn("📋") { showClipboardHistory() })
        toolbar.addView(space())
        toolbar.addView(createCircularBtn("⚙️"))
        toolbar.addView(space())

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar)
            setPadding(0, 0, 0, dpToPx(10))
        }
        return wrapper
    }

    // 🌈 RGB Animation Logic
    private fun startRgbAnimation(rootView: View) {
        rgbAnimator?.cancel()
        rgbAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 6000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val hue = animation.animatedValue as Float
                val bgColor = Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.2f))
                rootView.setBackgroundColor(bgColor)
            }
            start()
        }
    }

    // 🎈 අකුරු Popup එක
    private fun showPopup(v: View, text: String) {
        activePopup?.let { mainContainer?.removeView(it) }
        val popup = TextView(this).apply {
            this.text = text
            setTextColor(Color.BLACK)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dpToPx(12).toFloat() }
            
            val vLoc = IntArray(2); v.getLocationInWindow(vLoc)
            val rootLoc = IntArray(2); mainContainer?.getLocationInWindow(rootLoc)
            val w = dpToPx(50); val h = dpToPx(50)
            
            layoutParams = FrameLayout.LayoutParams(w, h)
            x = (vLoc[0] - rootLoc[0] + (v.width / 2) - (w / 2)).toFloat()
            y = (vLoc[1] - rootLoc[1] - h - dpToPx(10)).toFloat()
            
            alpha = 0f; scaleX = 0.5f; scaleY = 0.5f
        }
        mainContainer?.addView(popup)
        activePopup = popup
        popup.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(100).start()
    }

    private fun hidePopup() {
        activePopup?.let { popup ->
            popup.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(100).withEndAction {
                mainContainer?.removeView(popup)
            }.start()
            activePopup = null
        }
    }

    private fun showClipboardHistory() {
        val history = Prefs.clipboardHistory.value
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F0000000"))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            isClickable = true
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(12))
        }

        val title = TextView(this).apply {
            text = "Clipboard"
            setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val clearBtn = TextView(this).apply {
            text = "Clear  "
            setTextColor(Color.parseColor("#FF453A")); textSize = 14f
            setOnClickListener {
                Prefs.clearClipboardHistory()
                mainContainer?.removeView(overlay)
                showClipboardHistory()
            }
        }

        val closeBtn = TextView(this).apply {
            text = "Close"
            setTextColor(accentColor); textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { mainContainer?.removeView(overlay) }
        }

        header.addView(title); header.addView(clearBtn); header.addView(closeBtn)
        overlay.addView(header)

        val scrollView = ScrollView(this)
        val listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        
        if (history.isEmpty()) {
             listLayout.addView(TextView(this).apply {
                 text = "No copied items yet."; setTextColor(Color.GRAY); textSize = 14f
                 gravity = Gravity.CENTER; setPadding(0, dpToPx(20), 0, 0)
             })
        } else {
            history.forEach { item ->
                listLayout.addView(TextView(this).apply {
                    text = item; setTextColor(Color.WHITE); textSize = 16f
                    setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                    background = GradientDrawable().apply { setColor(keyBgColor); cornerRadius = dpToPx(8).toFloat() }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dpToPx(8)) }
                    maxLines = 1; ellipsize = TextUtils.TruncateAt.END
                    setOnClickListener {
                        commitBuffer()
                        currentInputConnection?.commitText(item, 1)
                        mainContainer?.removeView(overlay)
                    }
                })
            }
        }
        
        scrollView.addView(listLayout)
        overlay.addView(scrollView)
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
                if (currentTheme.id == "rgb") setColor(if (isMod) Color.parseColor("#1AFFFFFF") else Color.parseColor("#33FFFFFF"))
                else setColor(if (isMod) modKeyBgColor else (if (currentTheme.isLight) Color.WHITE else keyBgColor))
            }
            isClickable = true
        }

        val mainText = TextView(this).apply {
            text = getKeyLabel(key)
            setTextColor(if (currentTheme.isLight) Color.BLACK else (if(currentTheme.id == "rgb") Color.WHITE else textColor))
            textSize = if (key == "space") 11f else 20f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            if (subText.isNotEmpty()) setPadding(0, dpToPx(6), 0, 0) 
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        if (subText.isNotEmpty()) {
            container.addView(TextView(this).apply {
                text = subText
                setTextColor(if (currentTheme.isLight) Color.GRAY else subTextColor)
                textSize = 10f
                gravity = Gravity.TOP or Gravity.END
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dpToPx(4), dpToPx(6), 0)
                    gravity = Gravity.TOP or Gravity.END
                }
            })
        }

        container.addView(mainText)

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(50).start()
                    (v.background as GradientDrawable).setColor(if (key == "space") accentColor else keyPressedColor)
                    // ⭐ අකුරක් ඔබද්දි Popup එක පෙන්නනවා
                    if (Prefs.popupOn.value && key != "space" && key != "ENTER" && key != "LANG") {
                        showPopup(v, getKeyLabel(key))
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                    if (currentTheme.id == "rgb") (v.background as GradientDrawable).setColor(if (isMod) Color.parseColor("#1AFFFFFF") else Color.parseColor("#33FFFFFF"))
                    else (v.background as GradientDrawable).setColor(if (isMod) modKeyBgColor else (if (currentTheme.isLight) Color.WHITE else keyBgColor))
                    
                    hidePopup() // Popup එක අයින් කරනවා
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

    // ⭐ අලුත් සිංහල Transliteration Engine එක (nee -> නේ, n -> න්, AhAla -> ඇහැල)
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
                    out += "්" // අකුරක් විතරක් ආවම හල් අකුරු (උදා: n -> න්)
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
