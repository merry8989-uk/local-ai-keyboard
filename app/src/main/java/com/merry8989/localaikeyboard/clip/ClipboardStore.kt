package com.merry8989.localaikeyboard.clip

import android.content.ClipboardManager
import android.content.Context

/**
 * Reads the device clipboard and keeps a short, session-only history of what
 * was copied. Nothing is written to disk.
 *
 * On Android 10+ only the focused app (or the active keyboard) may read the
 * clipboard — which is exactly our situation, since the keyboard is on screen.
 */
class ClipboardStore(private val context: Context) {

    private val recent = ArrayDeque<String>()
    private var registered = false

    /** Called (on the main thread) whenever the clipboard changes. */
    var onChange: (() -> Unit)? = null

    private val listener = ClipboardManager.OnPrimaryClipChangedListener { refresh() }

    fun start() {
        if (!registered) {
            cm()?.addPrimaryClipChangedListener(listener)
            registered = true
        }
        refresh()
    }

    fun stop() {
        if (!registered) return
        cm()?.removePrimaryClipChangedListener(listener)
        registered = false
    }

    /** Re-read the clipboard right now and fold it into the history. */
    fun refresh() {
        val text = readClip() ?: return
        if (text.isBlank()) return
        if (recent.firstOrNull() != text) {
            recent.addFirst(text)
            while (recent.size > MAX_HISTORY) recent.removeLast()
        }
        onChange?.invoke()
    }

    fun current(): String? = readClip()

    fun recentItems(): List<String> = recent.toList()

    /** Current clipboard first, then the rest of the history, de-duplicated. */
    fun allItems(): List<String> {
        val out = LinkedHashSet<String>()
        current()?.let { if (it.isNotBlank()) out.add(it) }
        out.addAll(recent)
        return out.toList()
    }

    fun clearHistory() = recent.clear()

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
