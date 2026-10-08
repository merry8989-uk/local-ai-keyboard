package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The strip above the keys: up to three word candidates, plus the 📋 clipboard
 * button and the ✦ built-in AI button.
 */
class SuggestionBar(
    context: Context,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onCandidate(text: String)
        fun onAi()
        fun onClipboard()
    }

    private val slots = mutableListOf<TextView>()
    private lateinit var clipboardButton: TextView
    private lateinit var aiButton: TextView
    private var lastCandidates: List<String> = emptyList()
    private var theme: KeyboardTheme = ThemeRepository.default

    init {
        orientation = HORIZONTAL
        val pad = dp(6)
        setPadding(pad, pad, pad, pad)
        repeat(3) {
            val tv = TextView(context).apply {
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.DEFAULT
                setPadding(dp(8), dp(6), dp(8), dp(6))
            }
            addView(tv, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            slots += tv
        }

        clipboardButton = actionButton("📋") { listener.onClipboard() }
        addView(clipboardButton, LayoutParams(dp(44), LayoutParams.MATCH_PARENT))

        aiButton = actionButton("✦") { listener.onAi() }
        addView(aiButton, LayoutParams(dp(44), LayoutParams.MATCH_PARENT))

        applyTheme()
    }

    private fun actionButton(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            isClickable = true
            setOnClickListener { onClick() }
        }

    fun setTheme(theme: KeyboardTheme) {
        this.theme = theme
        applyTheme()
    }

    private fun applyTheme() {
        setBackgroundColor(theme.specialKeyBackground)
        for (tv in slots) tv.setTextColor(theme.keyText)
        clipboardButton.setTextColor(theme.keyText)
        aiButton.setTextColor(theme.accent)
    }

    fun setCandidates(candidates: List<String>) {
        lastCandidates = candidates
        for (i in slots.indices) {
            val tv = slots[i]
            val text = candidates.getOrNull(i)
            tv.text = text.orEmpty()
            tv.visibility = if (text.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            tv.setOnClickListener { text?.let { listener.onCandidate(it) } }
        }
    }

    fun isTopCandidate(text: String) = lastCandidates.firstOrNull() == text

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
