package com.sinhala.keyboard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

object Prefs {
    private const val NAME = "kb_settings"
    private var prefs: SharedPreferences? = null

    val theme = MutableStateFlow("violet")
    val language = MutableStateFlow("en")
    val vibrateOn = MutableStateFlow(true)
    val soundOn = MutableStateFlow(false)
    val popupOn = MutableStateFlow(true)
    
    // Clipboard History
    val clipboardHistory = MutableStateFlow<List<String>>(emptyList())

    fun init(ctx: Context) {
        if (prefs == null) {
            prefs = ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        }
        refresh()
    }

    fun refresh() {
        val p = prefs ?: return
        theme.value = p.getString("theme", "violet") ?: "violet"
        language.value = p.getString("language", "en") ?: "en"
        vibrateOn.value = p.getBoolean("vibrate", true)
        soundOn.value = p.getBoolean("sound", false)
        popupOn.value = p.getBoolean("popup", true)
        
        val historyString = p.getString("clipboard_history", "") ?: ""
        clipboardHistory.value = if (historyString.isEmpty()) emptyList() else historyString.split("|||")
    }

    fun setTheme(id: String) {
        theme.value = id
        prefs?.edit()?.putString("theme", id)?.apply()
    }

    fun setLanguage(l: String) {
        language.value = l
        prefs?.edit()?.putString("language", l)?.apply()
    }

    fun setVibrate(b: Boolean) {
        vibrateOn.value = b
        prefs?.edit()?.putBoolean("vibrate", b)?.apply()
    }

    fun setSound(b: Boolean) {
        soundOn.value = b
        prefs?.edit()?.putBoolean("sound", b)?.apply()
    }

    fun setPopup(b: Boolean) {
        popupOn.value = b
        prefs?.edit()?.putBoolean("popup", b)?.apply()
    }
    
    fun addClipboardItem(text: String) {
        if (text.isBlank()) return
        val current = clipboardHistory.value.toMutableList()
        if (current.contains(text)) current.remove(text)
        current.add(0, text)
        
        val newList = if (current.size > 10) current.take(10) else current
        clipboardHistory.value = newList
        prefs?.edit()?.putString("clipboard_history", newList.joinToString("|||"))?.apply()
    }

    // ⭐ අලුතෙන් එකතු කළ Clear function එක
    fun clearClipboardHistory() {
        clipboardHistory.value = emptyList()
        prefs?.edit()?.putString("clipboard_history", "")?.apply()
    }
}
