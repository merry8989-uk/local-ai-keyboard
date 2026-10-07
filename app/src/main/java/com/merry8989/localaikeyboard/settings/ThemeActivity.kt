package com.merry8989.localaikeyboard.settings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.merry8989.localaikeyboard.ime.KeyboardTheme
import com.merry8989.localaikeyboard.ime.ThemePrefs
import com.merry8989.localaikeyboard.ime.ThemeRepository

/** Theme picker: 24 built-in themes, tap to apply. */
class ThemeActivity : AppCompatActivity() {

    private lateinit var prefs: ThemePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ThemePrefs(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        list.addView(TextView(this).apply {
            text = "Theme"
            textSize = 22f
            setPadding(0, 0, 0, dp(12))
        })

        for (theme in ThemeRepository.themes) list.addView(row(theme))

        return ScrollView(this).apply { addView(list) }
    }

    private fun row(theme: KeyboardTheme): View {
        val selected = prefs.themeId == theme.id

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isClickable = true
            setOnClickListener {
                prefs.themeId = theme.id
                recreate()
            }
        }

        // preview swatch: background + a key chip in the middle
        val swatch = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(theme.background)
                cornerRadius = dp(8).toFloat()
            }
        }
        swatch.addView(View(this).apply {
            background = GradientDrawable().apply {
                setColor(theme.keyBackground)
                cornerRadius = dp(4).toFloat()
            }
        }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { rightMargin = dp(6) })
        swatch.addView(View(this).apply {
            background = GradientDrawable().apply {
                setColor(theme.accent)
                cornerRadius = dp(4).toFloat()
            }
        }, LinearLayout.LayoutParams(dp(20), dp(20)))

        container.addView(swatch, LinearLayout.LayoutParams(dp(72), dp(40)))

        container.addView(TextView(this).apply {
            text = theme.name
            textSize = 17f
            setPadding(dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        container.addView(TextView(this).apply {
            text = if (selected) "✓" else ""
            textSize = 20f
        })

        return container
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
