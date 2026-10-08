package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The smart bar above the keys.
 *
 * Left edge carries a \u22EE (⋮) menu button that opens the overflow panel.
 * The rest of the bar is a horizontally scrollable row of word candidates.
 * Swiping left on the bar asks for more "similar words" (30+).
 *
 * The quick action buttons (\u263A emoji, clipboard, \u2726 AI) are shown only
 * when there is nothing to suggest; the moment suggestions appear they take
 * the whole bar, so the row never mixes actions with words.
 */
class SuggestionBar(
    context: Context,
    private val listener: Listener,
) : HorizontalScrollView(context) {

    interface Listener {
        fun onCandidate(text: String)
        fun onAi()
        fun onEmoji()
        fun onClipboard()
        fun onMenu()
        fun onSwipeMore()
    }

    private val row = LinearLayout(context)
    private lateinit var menuButton: TextView
    private lateinit var quickBar: LinearLayout
    private val chips = mutableListOf<TextView>()
    private var lastCandidates: List<String> = emptyList()
    private var theme: KeyboardTheme = ThemeRepository.default

    private val gestures = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                if (e1 != null && e2 != null && e1.x - e2.x > dp(36)) {
                    listener.onSwipeMore()
                    return true
                }
                return false
            }
        },
    )

    init {
        isHorizontalScrollBarEnabled = false
        fillViewport = false
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        addView(row, FrameLayout.LayoutParams(WRAP, MATCH))

        menuButton = actionButton("\u22EE") { listener.onMenu() }
        row.addView(menuButton, LinearLayout.LayoutParams(dp(40), MATCH))

        quickBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(quickBar, LinearLayout.LayoutParams(WRAP, MATCH))
        quickBar.addView(
            actionButton("\u263A") { listener.onEmoji() },
            LinearLayout.LayoutParams(dp(40), MATCH),
        )
        quickBar.addView(
            actionButton("\uD83D\uDCCB") { listener.onClipboard() },
            LinearLayout.LayoutParams(dp(40), MATCH),
        )
        quickBar.addView(
            actionButton("\u2726") { listener.onAi() },
            LinearLayout.LayoutParams(dp(40), MATCH),
        )

        applyTheme()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun actionButton(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            isClickable = true
            setOnClickListener { onClick() }
        }

    private fun chip(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isClickable = true
            setOnClickListener { onClick() }
        }

    fun setTheme(theme: KeyboardTheme) {
        this.theme = theme
        applyTheme()
    }

    private fun applyTheme() {
        setBackgroundColor(theme.specialKeyBackground)
        menuButton.setTextColor(theme.keyText)
        for (i in 0 until quickBar.childCount) {
            (quickBar.getChildAt(i) as? TextView)?.setTextColor(theme.keyText)
        }
        for (c in chips) c.setTextColor(theme.keyText)
    }

    fun setCandidates(candidates: List<String>) {
        lastCandidates = candidates
        for (c in chips) row.removeView(c)
        chips.clear()
        for (text in candidates) {
            val tv = chip(text) { listener.onCandidate(text) }
            row.addView(tv, LinearLayout.LayoutParams(WRAP, MATCH))
            chips += tv
        }
        // Hide the quick actions while suggestions are showing.
        quickBar.visibility = if (candidates.isEmpty()) View.VISIBLE else View.GONE
        scrollTo(0, 0)
        applyTheme()
    }

    fun isTopCandidate(text: String) = lastCandidates.firstOrNull() == text

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    }
}
