package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The strip of alternate characters shown when a key is long-pressed.
 * Lives inside the input view (no popup window), so it never fights the editor
 * for focus.
 */
class AlternateStrip(
    context: Context,
    private val onPick: (String) -> Unit,
) : LinearLayout(context) {

    private val scroller = HorizontalScrollView(context)
    private val row = LinearLayout(context)
    private var theme: KeyboardTheme = ThemeRepository.default

    init {
        orientation = VERTICAL
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        scroller.addView(row)
        scroller.isHorizontalScrollBarEnabled = false
        addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun show(variants: List<String>, theme: KeyboardTheme) {
        this.theme = theme
        setBackgroundColor(theme.specialKeyBackground)
        row.removeAllViews()
        for (v in variants) {
            row.addView(chip(v))
        }
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    private fun chip(text: String): TextView = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(theme.keyText)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        typeface = Typeface.DEFAULT
        val pad = dp(14)
        setPadding(pad, dp(6), pad, dp(6))
        background = GradientDrawable().apply {
            setColor(theme.keyBackground)
            cornerRadius = dp(8).toFloat()
        }
        val m = dp(4)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
            setMargins(m, m, m, m)
        }
        isClickable = true
        setOnClickListener {
            onPick(text)
            hide()
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
