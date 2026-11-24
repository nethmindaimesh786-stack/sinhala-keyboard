package com.sinhala.keyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

class KeyboardService : InputMethodService() {

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            isFocusable = false
            isFocusableInTouchMode = false
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                KeyboardTheme {
                    KeyboardUI(
                        onCommit = { currentInputConnection?.commitText(it, 1) },
                        onComposing = { currentInputConnection?.setComposingText(it, 1) },
                        onDelete = { currentInputConnection?.deleteSurroundingText(1, 0) },
                        onEnter = { sendEnter() },
                        onVibrate = { vibrate(it) }
                    )
                }
            }
        }
    }

    private fun sendEnter() {
        val ic = currentInputConnection ?: return
        val opts = currentInputEditorInfo?.imeOptions ?: 0
        if ((opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            ic.commitText("\n", 1)
        } else {
            ic.sendKeyEvent(android.view.KeyEvent(
                android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(android.view.KeyEvent(
                android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun vibrate(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(ms)
            }
        } catch (_: Exception) {}
    }
}
