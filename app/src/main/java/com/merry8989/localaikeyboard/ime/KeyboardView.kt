package com.merry8989.localaikeyboard.ime

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A programmatic keyboard. Handles theming, key outlines, font size, spacing,
 * long-press popups (with a configurable delay), backspace hold/swipe, and the
 * long-press actions on ☺ (stickers) and ✦ (clipboard).
 */
class KeyboardView(
    context: Context,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onKey(key: KeyDef, label: String)
        fun onAlternates(variants: List<String>)
        fun onBackspaceHoldStart()
        fun onBackspaceHoldStop()
        fun onDeleteWord()
        fun onStickerRequest()
        fun onClipboardRequest()
    }

    var theme: KeyboardTheme = ThemeRepository.default
        set(value) {
            field = value
            render()
        }

    var keyStyle: KeyStyle = KeyStyle()
        set(value) {
            field = value
            render()
        }

    /** Include related symbols in long-press popups. */
    var includeSymbols: Boolean = true

    /** Supplies custom long-press words for a key label (may be empty). */
    var customWordsProvider: ((String) -> List<String>)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var caps = false
    private var layout: List<List<KeyDef>> = KeyboardLayout.qwerty(false, false)

    init {
        orientation = VERTICAL
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
        handler.removeCallbacksAndMessages(null)
        removeAllViews()
        setBackgroundColor(theme.background)
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
        val key = TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(theme.keyText)
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                (if (label.length > 1) 13f else 20f) * (keyStyle.fontSizePercent / 100f)
            )
            typeface = Typeface.DEFAULT
            isClickable = true
            isFocusable = false
            background = keyDrawable(
                if (def.isSpecial) theme.specialKeyBackground else theme.keyBackground
            )
            val m = dp(keyStyle.keySpacingDp)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, def.weight).apply {
                setMargins(m, m, m, m)
            }
        }

        when (def.code) {
            KeyCode.BACKSPACE -> attachBackspaceTouch(key, def, label)

            KeyCode.EMOJI -> {
                key.setOnClickListener {
                    haptic(it)
                    listener.onKey(def, label)
                }
                key.setOnLongClickListener {
                    haptic(it)
                    listener.onStickerRequest()
                    true
                }
            }

            KeyCode.AI -> {
                key.setOnClickListener {
                    haptic(it)
                    listener.onKey(def, label)
                }
                key.setOnLongClickListener {
                    haptic(it)
                    listener.onClipboardRequest()
                    true
                }
            }

            else -> {
                val variants = if (def.code == KeyCode.CHAR) {
                    val base = AlternateKeys.variantsFor(label, includeSymbols)
                    val custom = customWordsProvider?.invoke(label) ?: emptyList()
                    base + custom.filter { it.isNotBlank() && it !in base }
                } else {
                    emptyList()
                }
                if (variants.size > 1) {
                    attachLongPress(key, def, label, variants)
                } else {
                    key.setOnClickListener {
                        haptic(it)
                        listener.onKey(def, label)
                    }
                }
            }
        }
        return key
    }

    /** Long-press with a configurable delay, without swallowing normal taps. */
    private fun attachLongPress(key: TextView, def: KeyDef, label: String, variants: List<String>) {
        val fired = booleanArrayOf(false)
        val runnable = Runnable {
            fired[0] = true
            haptic(key)
            listener.onAlternates(variants)
        }
        key.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    fired[0] = false
                    v.isPressed = true
                    handler.postDelayed(runnable, keyStyle.longPressDelayMs.toLong())
                    false
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(runnable)
                    v.isPressed = false
                    if (!fired[0]) {
                        haptic(v)
                        listener.onKey(def, label)
                    }
                    fired[0] = false
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(runnable)
                    v.isPressed = false
                    fired[0] = false
                    true
                }
                else -> false
            }
        }
    }

    private fun attachBackspaceTouch(key: TextView, def: KeyDef, label: String) {
        var downX = 0f
        var wordDeleted = false
        key.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.x
                    wordDeleted = false
                    view.isPressed = true
                    listener.onBackspaceHoldStart()   // deletes once immediately, then repeats
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!wordDeleted && e.x - downX < -dp(36)) {
                        listener.onBackspaceHoldStop()
                        listener.onDeleteWord()
                        wordDeleted = true
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    listener.onBackspaceHoldStop()
                    view.isPressed = false
                    true
                }
                else -> false
            }
        }
    }

    private fun displayLabel(def: KeyDef): String = when {
        def.code == KeyCode.CHAR && caps -> def.label.uppercase()
        def.code == KeyCode.SHIFT && caps -> "⇪"
        else -> def.label
    }

    private fun haptic(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun keyDrawable(color: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(keyStyle.cornerRadiusDp).toFloat()
            // Keys are drawn as plain filled shapes - no outline/border - so they
            // read only by colour, like a normal keyboard.
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
