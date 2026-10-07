package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.merry8989.localaikeyboard.R
import com.merry8989.localaikeyboard.ai.AiAction

/**
 * Inline panel shown in place of suggestions when the user taps ✦.
 * Offers the AI actions, streams the model output, and lets the user accept it.
 * Rendered inside the input view (not a dialog) so it needs no extra window
 * permissions and never steals focus from the editor.
 */
class AiPanel(
    context: Context,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onAction(action: AiAction)
        fun onAccept(text: String)
        fun onDismiss()
    }

    private val resultView = TextView(context)
    private val scroller = ScrollView(context)
    private val acceptBtn = chip("✓ Insert") { listener.onAccept(resultView.text.toString()) }
    private val cancelBtn = chip("✕") { listener.onDismiss() }

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.kb_bg))
        val pad = dp(6)
        setPadding(pad, pad, pad, pad)

        // Action chips row
        val actions = LinearLayout(context).apply { orientation = HORIZONTAL }
        AiAction.entries.forEach { action ->
            actions.addView(chip(action.label) { listener.onAction(action) }, weightLp())
        }
        addView(actions, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))

        // Streaming result area
        resultView.apply {
            setTextColor(ContextCompat.getColor(context, R.color.key_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        scroller.addView(resultView)
        addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        // Accept / cancel row
        val footer = LinearLayout(context).apply { orientation = HORIZONTAL }
        footer.addView(cancelBtn, weightLp(0.6f))
        footer.addView(acceptBtn, weightLp(2f))
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
    }

    fun show() {
        resultView.text = ""
        visibility = View.VISIBLE
        acceptBtn.isEnabled = false
    }

    fun hide() {
        visibility = View.GONE
    }

    fun setStreaming(partial: String) {
        resultView.text = partial
        acceptBtn.isEnabled = partial.isNotBlank()
    }

    fun setStatus(message: String) {
        resultView.text = message
        acceptBtn.isEnabled = false
    }

    private fun chip(text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.key_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            isClickable = true
            setOnClickListener { onClick() }
        }

    private fun weightLp(w: Float = 1f) = LayoutParams(0, LayoutParams.MATCH_PARENT, w)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
