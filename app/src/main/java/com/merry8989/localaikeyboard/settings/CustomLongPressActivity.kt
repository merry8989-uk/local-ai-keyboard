package com.merry8989.localaikeyboard.settings

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.merry8989.localaikeyboard.lingua.LanguagePacks

/**
 * Step one of "Custom Long Press Words": pick a language, then manage the words
 * attached to each key.
 */
class CustomLongPressActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(TextView(this).apply {
            text = "Custom Long Press Words"; textSize = 22f; setPadding(0, 0, 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "Select a language to manage custom long press words. Words you add " +
                "appear in that key's long-press popup. Stored on this device only."
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })

        for (pack in LanguagePacks.all) {
            root.addView(card(pack.name, pack.nativeName) {
                startActivity(
                    Intent(this, CustomWordsActivity::class.java)
                        .putExtra(CustomWordsActivity.EXTRA_LANG_ID, pack.id)
                        .putExtra(CustomWordsActivity.EXTRA_LANG_NAME, pack.name)
                )
            })
        }

        root.addView(TextView(this).apply {
            text = "More languages appear here as they are added to the keyboard."
            textSize = 13f
            setPadding(0, dp(16), 0, 0)
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun card(title: String, native: String, onClick: () -> Unit): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            isClickable = true
            setOnClickListener { onClick() }
        }
        c.addView(TextView(this).apply {
            text = if (native.isBlank() || native == title) title else "$title ($native)"
            textSize = 17f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        c.addView(TextView(this).apply { text = "›"; textSize = 20f })
        return c
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
