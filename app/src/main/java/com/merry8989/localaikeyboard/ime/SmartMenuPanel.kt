package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The smart bar's overflow panel, opened by the \u22EE button (and the \u22EE key).
 * Settings and Language were moved here out of the keyboard; Emoji, Clipboard,
 * Text formatting and Calculator are exposed here too. Rendered inline (like the
 * other panels) so it needs no extra window permissions.
 */
class SmartMenuPanel(
    context: Context,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onMenuSettings()
        fun onMenuLanguage()
        fun onMenuEmoji()
        fun onMenuClipboard()
        fun onMenuFormat()
        fun onMenuCalculator()
        fun onMenuDismiss()
    }

    private val buttons = mutableListOf<TextView>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dp(4)
        setPadding(pad, pad, pad, pad)

        item("Settings") { listener.onMenuSettings() }
        item("Language") { listener.onMenuLanguage() }
        item("Emoji") { listener.onMenuEmoji() }
        item("Clipboard") { listener.onMenuClipboard() }
        item("Format") { listener.onMenuFormat() }
        item("Calc") { listener.onMenuCalculator() }
        item("\u2715") { listener.onMenuDismiss() }
    }

    private fun item(label: String, onClick: () -> Unit) {
        val tv = TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true
            setOnClickListener { onClick() }
        }
        addView(tv, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        buttons += tv
    }

    fun applyTheme(theme: KeyboardTheme) {
        setBackgroundColor(theme.specialKeyBackground)
        for (b in buttons) b.setTextColor(theme.keyText)
    }

    fun show(theme: KeyboardTheme) {
        applyTheme(theme)
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun isShowing() = visibility == View.VISIBLE

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
