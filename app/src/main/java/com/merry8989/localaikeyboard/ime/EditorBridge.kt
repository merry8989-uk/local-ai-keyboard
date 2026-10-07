package com.merry8989.localaikeyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

/**
 * Thin, null-safe wrapper over [InputConnection]. Everything that mutates the
 * editor goes through here so the rest of the IME never touches raw
 * InputConnection calls.
 */
class EditorBridge(private val service: android.inputmethodservice.InputMethodService) {

    private val ic: InputConnection?
        get() = service.currentInputConnection

    fun commit(text: String) {
        ic?.commitText(text, 1)
    }

    fun setComposing(text: String) {
        ic?.setComposingText(text, 1)
    }

    /** Compose with styling (used for spell/grammar underlines). */
    fun setComposingRich(text: CharSequence) {
        ic?.setComposingText(text, 1)
    }

    fun finishComposing() {
        ic?.finishComposingText()
    }

    fun backspace() {
        val conn = ic ?: return
        val selected = conn.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            conn.commitText("", 1)
        } else {
            conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    fun deleteWord() {
        val before = ic?.getTextBeforeCursor(32, 0)?.toString().orEmpty()
        val wordLen = before.takeLastWhile { !it.isWhitespace() }.length
        deleteBefore(wordLen.coerceAtLeast(1))
    }

    /** Delete [n] characters before the cursor (used by auto-correct). */
    fun deleteBefore(n: Int) {
        if (n <= 0) return
        ic?.deleteSurroundingText(n, 0)
    }

    /** Word currently being typed (before the cursor). */
    fun currentWordBeforeCursor(): String {
        val before = ic?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        return before.takeLastWhile { it.isLetter() || it == '\'' }
    }

    /** Raw text before the cursor, up to [max] characters. */
    fun textBeforeCursor(max: Int): String =
        ic?.getTextBeforeCursor(max, 0)?.toString().orEmpty()

    /** Selected text if any, else the sentence around the cursor. */
    fun contextForAi(): String {
        val conn = ic ?: return ""
        conn.getSelectedText(0)?.toString()?.let { if (it.isNotBlank()) return it }
        val before = conn.getTextBeforeCursor(400, 0)?.toString().orEmpty()
        return before.substringAfterLast('.', before).trim()
    }

    /** Replace the current selection (or insert at cursor) with [text]. */
    fun replaceSelection(text: String) {
        val conn = ic ?: return
        val selected = conn.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            conn.commitText(text, 1)
        } else {
            conn.commitText(text, 1)
        }
    }

    fun editorInfo() = service.currentInputEditorInfo

    fun inputType() = service.currentInputEditorInfo?.inputType ?: 0

    fun showIme() {
        service.getSystemService(InputMethodManager::class.java)
            ?.showSoftInput(service.window?.window?.decorView, 0)
    }
}
