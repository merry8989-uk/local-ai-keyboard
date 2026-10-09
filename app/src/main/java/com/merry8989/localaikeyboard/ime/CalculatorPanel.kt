package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Inline calculator: punch in an expression while you write, see the live
 * result, and Insert it straight into the chat / editor. While it is open the
 * keyboard is hidden (the service swaps them); the ⌨ key here returns to the
 * keyboard.
 */
class CalculatorPanel(
    context: Context,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onInsertResult(result: String)
        fun onCalculatorDismiss()
    }

    private val display = TextView(context)
    private var expr = ""

    init {
        orientation = VERTICAL
        setPadding(dp(6), dp(6), dp(6), dp(6))

        display.apply {
            gravity = Gravity.END
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setPadding(dp(8), dp(6), dp(8), dp(6))
            text = "0"
        }
        addView(display, LayoutParams(LayoutParams.MATCH_PARENT, dp(40)))

        val grid = listOf(
            listOf("7", "8", "9", "/", "C"),
            listOf("4", "5", "6", "*", "\u232B"),
            listOf("1", "2", "3", "-", "%"),
            listOf("0", ".", "(", ")", "+"),
        )
        for (rowKeys in grid) {
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            for (k in rowKeys) {
                row.addView(key(k), LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }

        val footer = LinearLayout(context).apply { orientation = HORIZONTAL }
        // ⌨ returns to the keyboard (hides the calculator).
        footer.addView(key("\u2328"), LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        footer.addView(key("="), LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        footer.addView(key("Insert"), LayoutParams(0, LayoutParams.MATCH_PARENT, 2f))
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
    }

    private fun key(label: String): TextView =
        TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            isClickable = true
            setOnClickListener { handle(label) }
        }

    private fun handle(label: String) {
        when (label) {
            "C" -> { expr = ""; update() }
            "\u232B" -> { if (expr.isNotEmpty()) expr = expr.dropLast(1); update() }
            "=" -> evaluate()
            "Insert" -> listener.onInsertResult(display.text.toString())
            "\u2328" -> listener.onCalculatorDismiss()
            else -> { expr += label; update() }
        }
    }

    private fun update() {
        display.text = if (expr.isEmpty()) "0" else expr
    }

    private fun evaluate() {
        val v = Calc.eval(expr)
        if (v == null) {
            display.text = "Error"
            return
        }
        val s = Calc.format(v)
        expr = s
        display.text = s
    }

    fun applyTheme(theme: KeyboardTheme) {
        setBackgroundColor(theme.specialKeyBackground)
        display.setTextColor(theme.keyText)
        forEachText { it.setTextColor(theme.keyText) }
    }

    private fun forEachText(action: (TextView) -> Unit) {
        fun walk(v: View) {
            if (v is TextView) action(v)
            if (v is android.view.ViewGroup) {
                for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
        }
        walk(this)
    }

    fun show(theme: KeyboardTheme) {
        expr = ""
        update()
        applyTheme(theme)
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }

    fun isShowing() = visibility == View.VISIBLE

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
