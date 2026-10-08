package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** The text transforms the Format panel can apply. */
enum class FormatKind { UPPER, LOWER, TITLE, SENTENCE, BULLET, NUMBERED }

/**
 * Inline text-formatting panel: case transforms and list styles. Applies to the
 * selection if there is one, otherwise to the word before the cursor.
 */
class TextFormatPanel(
    context: Context,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onFormat(kind: FormatKind)
        fun onFormatDismiss()
    }

    private val buttons = mutableListOf<TextView>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dp(4)
        setPadding(pad, pad, pad, pad)

        item("ABC") { listener.onFormat(FormatKind.UPPER) }
        item("abc") { listener.onFormat(FormatKind.LOWER) }
        item("Abc") { listener.onFormat(FormatKind.TITLE) }
        item("Abc.") { listener.onFormat(FormatKind.SENTENCE) }
        item("\u2022 List") { listener.onFormat(FormatKind.BULLET) }
        item("1. List") { listener.onFormat(FormatKind.NUMBERED) }
        item("\u2715") { listener.onFormatDismiss() }
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
