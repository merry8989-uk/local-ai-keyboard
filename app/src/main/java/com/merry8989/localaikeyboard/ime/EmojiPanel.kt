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
    private var panelTheme: KeyboardTheme = ThemeRepository.default
    private var selectedCategory = 0

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
        panelTheme = theme
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
            val isSelected = index == selectedCategory
            val bgColor = if (isSelected) panelTheme.keyBackground else panelTheme.specialKeyBackground
            val fgColor = if (isSelected) panelTheme.accent else panelTheme.keyText

            val tab = TextView(context)
            tab.text = cat.icon
            tab.gravity = Gravity.CENTER
            tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            tab.setPadding(dp(12), dp(4), dp(12), dp(4))
            tab.isClickable = true
            tab.setTextColor(fgColor)
            tab.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(6).toFloat()
            }
            tab.setOnClickListener {
                selectedCategory = index
                buildTabs()
                buildGrid()
            }

            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            lp.setMargins(dp(2), dp(4), dp(2), dp(4))
            tabRow.addView(tab, lp)
        }
    }

    private fun buildGrid() {
        grid.removeAllViews()
        val emojis = EmojiData.categories[selectedCategory].emojis
        var row: LinearLayout? = null
        emojis.forEachIndexed { i, emoji ->
            if (i % 8 == 0) {
                row = LinearLayout(context).apply { orientation = HORIZONTAL }
                grid.addView(row)
            }
            val cell = TextView(context)
            cell.text = emoji
            cell.gravity = Gravity.CENTER
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            cell.typeface = Typeface.DEFAULT
            cell.isClickable = true
            cell.setOnClickListener { onPick(emoji) }
            row!!.addView(cell, LayoutParams(0, dp(40), 1f))
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
