package com.merry8989.localaikeyboard.settings

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.merry8989.localaikeyboard.App
import com.merry8989.localaikeyboard.ime.ThemePrefs
import com.merry8989.localaikeyboard.lingua.LanguagePack
import com.merry8989.localaikeyboard.lingua.LanguagePacks

/**
 * Languages. English is built in and always on; other packs can be toggled on.
 * A pack can also be downloaded (see LanguagePacks for the URL field).
 */
class LanguagesActivity : AppCompatActivity() {

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

        root.addView(TextView(this).apply {
            text = "Languages"; textSize = 22f; setPadding(0, 0, 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "English is on by default and always enabled. Turn on more languages " +
                "to include their words in suggestions. Everything is stored on this device."
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })

        for (pack in LanguagePacks.all) root.addView(row(pack))

        root.addView(TextView(this).apply {
            text = "More languages can be added by dropping a word list into the app's " +
                "assets folder, or by pointing a language pack at a hosted list to download."
            textSize = 13f
            setPadding(0, dp(16), 0, 0)
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun row(pack: LanguagePack): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply { text = pack.name; textSize = 17f })
        col.addView(TextView(this).apply {
            text = when {
                pack.alwaysOn -> "Built in · always on"
                pack.isBundled -> "Built in"
                pack.isDownloadable -> "Downloadable"
                else -> "Not available"
            }
            textSize = 13f
        })
        c.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        c.addView(SwitchCompat(this).apply {
            isChecked = prefs.enabledLanguages.contains(pack.id)
            isEnabled = !pack.alwaysOn && (pack.isBundled || pack.isDownloadable)
            setOnCheckedChangeListener { _, checked ->
                val current = prefs.enabledLanguages.toMutableSet()
                if (checked) current.add(pack.id) else current.remove(pack.id)
                prefs.enabledLanguages = current
                App.instance.spellEngine.reload()
            }
        })
        return c
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
