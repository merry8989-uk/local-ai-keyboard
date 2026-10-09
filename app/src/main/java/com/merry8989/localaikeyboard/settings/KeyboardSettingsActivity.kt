package com.merry8989.localaikeyboard.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.merry8989.localaikeyboard.ime.ThemePrefs

/**
 * Keyboard appearance and behaviour: number row, long-press popups, font size,
 * key spacing, outlines and height. All stored locally.
 */
class KeyboardSettingsActivity : AppCompatActivity() {

    private lateinit var prefs: ThemePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ThemePrefs(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(TextView(this).apply { text = "Keyboard"; textSize = 22f; setPadding(0, 0, 0, dp(8)) })

        // ---- layout
        root.addView(section("Layout"))
        root.addView(switchRow("Number row", "Show a number row above the letter layout",
            prefs.numberRow) { prefs.numberRow = it })
        root.addView(switchRow(". ? next to Enter", "Toggle between '.' and '?' next to the Enter key",
            prefs.dotOrQuestion) { prefs.dotOrQuestion = it })
        root.addView(slider("Font size", 80, 140, prefs.fontSizePercent, "%") { prefs.fontSizePercent = it })
        root.addView(slider("Key spacing", 0, 8, prefs.keySpacingDp, " dp") { prefs.keySpacingDp = it })
        root.addView(slider("Keyboard height", 200, 340, prefs.keyHeightDp, " dp") { prefs.keyHeightDp = it })

        // ---- key press
        root.addView(section("Key press"))
        root.addView(switchRow("Accents include symbol popups",
            "Adds related symbols to long-press popups", prefs.accentsIncludeSymbols) {
            prefs.accentsIncludeSymbols = it
        })
        root.addView(slider("Long key press delay", 200, 700, prefs.longPressDelayMs, " ms") {
            prefs.longPressDelayMs = it
        })
        root.addView(switchRow("Auto-space after punctuation",
            "Insert a space after . , ? ! ; :", prefs.autoSpaceAfterPunctuation) {
            prefs.autoSpaceAfterPunctuation = it
        })
        root.addView(TextView(this).apply {
            text = "Custom Long Press Words"; textSize = 16f; setPadding(0, dp(12), 0, dp(2))
        })
        root.addView(TextView(this).apply {
            text = "Add your own words to each key's long-press popup."
            textSize = 12f; setPadding(0, 0, 0, dp(6))
        })
        root.addView(Button(this).apply {
            text = "Manage custom long press words"
            gravity = Gravity.START
            setOnClickListener {
                startActivity(android.content.Intent(this@KeyboardSettingsActivity, CustomLongPressActivity::class.java))
            }
        })

        // ---- appearance
        root.addView(section("Appearance"))
        root.addView(slider("Corner radius", 0, 24, prefs.cornerRadiusDp, " dp") { prefs.cornerRadiusDp = it })

        root.addView(Button(this).apply {
            text = "Reset to defaults"
            setOnClickListener {
                prefs.cornerRadiusDp = 8
                prefs.outlineWidthDp = 0
                prefs.outlineColor = 0x00000000
                prefs.keyHeightDp = 260
                prefs.fontSizePercent = 100
                prefs.keySpacingDp = 3
                prefs.longPressDelayMs = 300
                recreate()
            }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun section(t: String) = TextView(this).apply {
        text = t; textSize = 16f; setPadding(0, dp(20), 0, dp(6))
    }

    private fun switchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply { text = title; textSize = 16f })
        col.addView(TextView(this).apply { text = subtitle; textSize = 12f })
        c.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        c.addView(SwitchCompat(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, v -> onChange(v) }
        })
        return c
    }

    private fun slider(
        title: String, min: Int, max: Int, value: Int, unit: String, onChange: (Int) -> Unit,
    ): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val label = TextView(this).apply { text = "$title: $value$unit"; textSize = 16f }
        c.addView(label)
        c.addView(SeekBar(this).apply {
            this.max = max - min
            progress = value - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = p + min
                    label.text = "$title: $v$unit"
                    if (fromUser) onChange(v)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })
        return c
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
