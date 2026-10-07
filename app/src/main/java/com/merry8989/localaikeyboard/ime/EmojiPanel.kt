package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * A working emoji picker: category tabs plus a grid of prebuilt emoji.
 * Tapping one inserts it into the text field (emoji are text, so this works in
 * any app).
 */
class EmojiPanel(
    context: Context,
    private val onPick: (String) -> Unit,
) : LinearLayout(context) {

    private val tabRow = LinearLayout(context)
    private val grid = LinearLayout(context)
    private var theme: KeyboardTheme = ThemeRepository.default
    private var current = 0

    init {
        orientation = VERTICAL

        tabRow.orientation = HORIZONTAL
        tabRow.gravity = Gravity.CENTER_VERTICAL
        addView(
            HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(tabRow)
            },
            LayoutParams(LayoutParams.MATCH_PARENT, dp(40))
        )

        grid.orientation = VERTICAL
        addView(
            ScrollView(context).apply { addView(grid) },
            LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        )
    }

    fun show(theme: KeyboardTheme) {
        this.theme = theme
        setBackgroundColor(theme.background)
        buildTabs()
        buildGrid()
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun isShowing() = visibility == View.VISIBLE

    private fun buildTabs() {
        tabRow.removeAllViews()
        EmojiData.categories.forEachIndexed { index, cat ->
            val tv = TextView(context).apply {
                text = cat.icon
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setPadding(dp(12), dp(4), dp(12), dp(4))
                isClickable = true
                setTextColor(if (index == current) theme.accent else theme.keyText)
                background = GradientDrawable().apply {
                    setColor(if (index == current) theme.keyBackground else theme.specialKeyBackground)
                    cornerRadius = dp(6).toFloat()
                }
                setOnClickListener {
                    current = index
                    buildTabs()
                    buildGrid()
                }
            }
            tabRow.addView(tv, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
                setMargins(dp(2), dp(4), dp(2), dp(4))
            })
        }
    }

    private fun buildGrid() {
        grid.removeAllViews()
        val emojis = EmojiData.categories[current].emojis
        var row: LinearLayout? = null
        emojis.forEachIndexed { i, emoji ->
            if (i % 8 == 0) {
                row = LinearLayout(context).apply { orientation = HORIZONTAL }
                grid.addView(row)
            }
            row!!.addView(cell(emoji), LayoutParams(0, dp(40), 1f))
        }
    }

    private fun cell(emoji: String): TextView = TextView(context).apply {
        text = emoji
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        typeface = Typeface.DEFAULT
        isClickable = true
        setOnClickListener { onPick(emoji) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
