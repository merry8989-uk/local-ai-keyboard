package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The in-keyboard clipboard: shows what's on the clipboard plus recent items
 * seen while the keyboard was open. Tapping one inserts it.
 */
class ClipboardPanel(
    context: Context,
    private val onPick: (String) -> Unit,
) : LinearLayout(context) {

    private val list = LinearLayout(context)
    private var panelTheme: KeyboardTheme = ThemeRepository.default

    init {
        orientation = VERTICAL
        list.orientation = VERTICAL
        addView(
            ScrollView(context).apply { addView(list) },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun show(theme: KeyboardTheme, items: List<String>) {
        panelTheme = theme
        setBackgroundColor(theme.background)
        build(items)
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun isShowing() = visibility == View.VISIBLE

    private fun build(items: List<String>) {
        list.removeAllViews()
        if (items.isEmpty()) {
            list.addView(label("Nothing on the clipboard yet."))
            return
        }
        items.forEachIndexed { index, text ->
            val row = TextView(context)
            row.text = text.replace("\n", " ").take(120)
            row.setTextColor(panelTheme.keyText)
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            row.setPadding(dp(12), dp(10), dp(12), dp(10))
            row.background = GradientDrawable().apply {
                setColor(panelTheme.keyBackground)
                cornerRadius = dp(8).toFloat()
            }
            row.isClickable = true
            row.setOnClickListener { onPick(text) }
            val m = dp(4)
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            lp.setMargins(m, m, m, m)
            list.addView(row, lp)
            if (index == 0) {
                list.addView(label("Tap an item to paste it"))
            }
        }
    }

    private fun label(text: String) = TextView(context).apply {
        this.text = text
        gravity = Gravity.START
        setTextColor(panelTheme.accent)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(dp(12), dp(4), dp(12), dp(8))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
