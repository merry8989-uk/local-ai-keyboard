package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.merry8989.localaikeyboard.R

/**
 * The strip above the keys: up to three word candidates plus the ✦ AI button.
 * It is intentionally dumb — it renders whatever it is handed and reports taps.
 */
class SuggestionBar(
    context: Context,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onCandidate(text: String)
        fun onAi()
    }

    private val slots = mutableListOf<TextView>()
    private var lastCandidates: List<String> = emptyList()

    init {
        orientation = HORIZONTAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.suggestion_bg))
        val pad = dp(6)
        setPadding(pad, pad, pad, pad)
        repeat(3) {
            val tv = TextView(context).apply {
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.key_text))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.DEFAULT
                setPadding(dp(8), dp(6), dp(8), dp(6))
            }
            addView(tv, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            slots += tv
        }
        addView(buildAiButton(), LayoutParams(dp(48), LayoutParams.MATCH_PARENT))
    }

    private fun buildAiButton(): TextView = TextView(context).apply {
        text = "✦"
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.accent))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        isClickable = true
        setOnClickListener { listener.onAi() }
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

    /** True if [text] is the current top (auto-correct) candidate. */
    fun isTopCandidate(text: String) = lastCandidates.firstOrNull() == text

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
