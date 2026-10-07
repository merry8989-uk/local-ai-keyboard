package com.merry8989.localaikeyboard.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.merry8989.localaikeyboard.ime.ThemePrefs

/**
 * Key appearance: corner rounding, outline width and colour, and keyboard height.
 * All stored locally in preferences.
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
        root.addView(TextView(this).apply {
            text = "Customise how the keys are drawn. Applies everywhere on the device."
            textSize = 14f
            setPadding(0, 0, 0, dp(8))
        })

        root.addView(slider("Corner radius", 0, 24, prefs.cornerRadiusDp, " dp") {
            prefs.cornerRadiusDp = it
        })
        root.addView(slider("Outline width", 0, 4, prefs.outlineWidthDp, " dp") {
            prefs.outlineWidthDp = it
        })
        root.addView(slider("Keyboard height", 200, 340, prefs.keyHeightDp, " dp") {
            prefs.keyHeightDp = it
        })

        root.addView(TextView(this).apply {
            text = "Outline colour"; textSize = 16f; setPadding(0, dp(12), 0, dp(4))
        })
        root.addView(swatches())

        root.addView(Button(this).apply {
            text = "Reset to defaults"
            setOnClickListener {
                prefs.cornerRadiusDp = 8
                prefs.outlineWidthDp = 0
                prefs.outlineColor = 0x00000000
                prefs.keyHeightDp = 260
                recreate()
            }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun slider(
        title: String,
        min: Int,
        max: Int,
        value: Int,
        unit: String,
        onChange: (Int) -> Unit,
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

    private fun swatches(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(12))
        }
        val colors = listOf(
            0x00000000,
            0xFFFFFFFF.toInt(),
            0xFF000000.toInt(),
            0xFFFF6B35.toInt(),
            0xFF38BDF8.toInt(),
            0xFF4ADE80.toInt(),
            0xFFFF2E97.toInt(),
            0xFFF59E0B.toInt(),
        )
        for (col in colors) {
            val selected = prefs.outlineColor == col
            row.addView(View(this).apply {
                background = GradientDrawable().apply {
                    setColor(col)
                    cornerRadius = dp(6).toFloat()
                    setStroke(dp(if (selected) 3 else 1), if (selected) 0xFFFFFFFF.toInt() else 0x66FFFFFF)
                }
                isClickable = true
                setOnClickListener {
                    prefs.outlineColor = col
                    if (col != 0 && prefs.outlineWidthDp == 0) prefs.outlineWidthDp = 2
                    recreate()
                }
            }, LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
        return row
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
