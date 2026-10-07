package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.merry8989.localaikeyboard.R

/**
 * A programmatic keyboard. Rows of [KeyDef] become weighted TextViews inside
 * horizontal LinearLayouts, stacked vertically. Kept dependency-free so it can
 * be reused inside any IME.
 */
class KeyboardView(
    context: Context,
    private val listener: KeyListener,
) : LinearLayout(context) {

    fun interface KeyListener {
        fun onKey(key: KeyDef, label: String)
    }

    private var caps = false
    private var layout: List<List<KeyDef>> = KeyboardLayout.qwerty

    init {
        orientation = VERTICAL
        setBackgroundColor(color(R.color.kb_bg))
        val pad = dp(4)
        setPadding(pad, pad, pad, pad)
        render()
    }

    fun setLayout(rows: List<List<KeyDef>>) {
        layout = rows
        render()
    }

    fun setCaps(on: Boolean) {
        caps = on
        render()
    }

    fun isCaps() = caps

    private fun render() {
        removeAllViews()
        for (rowDefs in layout) {
            addView(buildRow(rowDefs), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    private fun buildRow(defs: List<KeyDef>): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        for (def in defs) {
            row.addView(buildKey(def), LayoutParams(0, LayoutParams.MATCH_PARENT, def.weight))
        }
        return row
    }

    private fun buildKey(def: KeyDef): TextView {
        val label = displayLabel(def)
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(color(R.color.key_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length > 1) 13f else 20f)
            typeface = Typeface.DEFAULT
            isClickable = true
            isFocusable = false
            background = ContextCompat.getDrawable(
                context,
                if (def.isSpecial) R.drawable.key_special else R.drawable.key_normal
            )
            val m = dp(3)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, def.weight).apply {
                setMargins(m, m, m, m)
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                listener.onKey(def, label)
            }
        }
    }

    private fun displayLabel(def: KeyDef): String = when {
        def.code == KeyCode.CHAR && caps -> def.label.uppercase()
        def.code == KeyCode.SHIFT && caps -> "⇪"
        else -> def.label
    }

    private fun color(res: Int) = ContextCompat.getColor(context, res)
    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}
