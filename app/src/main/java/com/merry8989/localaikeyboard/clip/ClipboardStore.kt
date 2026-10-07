package com.merry8989.localaikeyboard.clip

import android.content.ClipboardManager
import android.content.Context

/**
 * Reads the device clipboard and keeps a short, session-only history of what
 * was copied while the keyboard was open. Nothing is written to disk.
 *
 * On Android 10+ only the focused app (or the active keyboard) may read the
 * clipboard, which is exactly the situation here — the keyboard is on screen.
 */
class ClipboardStore(private val context: Context) {

    private val recent = ArrayDeque<String>()
    private var registered = false

    private val listener = ClipboardManager.OnPrimaryClipChangedListener { capture() }

    fun start() {
        if (registered) return
        cm()?.addPrimaryClipChangedListener(listener)
        registered = true
        capture()
    }

    fun stop() {
        if (!registered) return
        cm()?.removePrimaryClipChangedListener(listener)
        registered = false
    }

    fun current(): String? = readClip()

    fun recentItems(): List<String> = recent.toList()

    fun clearHistory() = recent.clear()

    private fun capture() {
        val text = readClip() ?: return
        if (text.isBlank()) return
        if (recent.firstOrNull() == text) return
        recent.addFirst(text)
        while (recent.size > MAX_HISTORY) recent.removeLast()
    }

    private fun readClip(): String? {
        val cm = cm() ?: return null
        if (!cm.hasPrimaryClip()) return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()
    }

    private fun cm() = context.getSystemService(ClipboardManager::class.java)

    private companion object {
        const val MAX_HISTORY = 20
    }
}
