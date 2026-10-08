package com.merry8989.localaikeyboard.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.merry8989.localaikeyboard.ime.ThemePrefs

/**
 * The hub. Everything here is on-device: there is nothing to download and the
 * app requests no network permission.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: ThemePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ThemePrefs(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }

        root.addView(heading("Local AI Keyboard"))
        root.addView(sub("An offline keyboard. Your typing stays on this phone."))

        // ---- Set up
        root.addView(section("Set up"))
        root.addView(button("Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(button("Choose input method") {
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
        })

        // ---- Built-in AI
        root.addView(section("Built-in AI"))
        root.addView(sub(
            "Ready to use — nothing to download. The ✦ actions (Fix, Rephrase, Continue, " +
                "Email, Essay, Message) run entirely on the device using a lightweight " +
                "built-in text engine, not a cloud service."
        ))

        // ---- Categories
        root.addView(section("Settings"))
        root.addView(row("Languages", "English on by default, Hindi built in") {
            startActivity(Intent(this, LanguagesActivity::class.java))
        })
        root.addView(row("Theme", "Pick from 24 built-in themes") {
            startActivity(Intent(this, ThemeActivity::class.java))
        })
        root.addView(row("Keyboard", "Number row, outlines, key size") {
            startActivity(Intent(this, KeyboardSettingsActivity::class.java))
        })
        root.addView(row("Corrections and suggestions", "Spell check, grammar, suggestions") {
            startActivity(Intent(this, CorrectionsSuggestionsActivity::class.java))
        })
        root.addView(toggle("Sound on keypress", prefs.soundEnabled) { prefs.soundEnabled = it })
        root.addView(toggle("Vibrate on keypress", prefs.vibrateEnabled) { prefs.vibrateEnabled = it })
        root.addView(row("Gestures", "Swipe to delete a word") {
            info("Hold ⌫ to keep deleting. Swipe left on ⌫ to delete a whole word.")
        })
        root.addView(row("Clipboard", "Paste what you copied") {
            info("Tap the 📋 button in the suggestion bar to paste. History is kept in memory only.")
        })
        root.addView(toggle("Clipboard history", prefs.clipboardHistory) { prefs.clipboardHistory = it })
        root.addView(row("Emoji", "Tap ☺ on the keyboard") {
            info("Tap ☺ for prebuilt emoji. Long-press ☺ for stickers & media.")
        })
        root.addView(row("Personal dictionary", "Words learned on this device") {
            startActivity(Intent(this, DictionaryActivity::class.java))
        })
        root.addView(row("Stickers & media", "Add photos, GIFs, PNGs") {
            startActivity(Intent(this, StickerActivity::class.java))
        })

        root.addView(section("About"))
        root.addView(sub("Local AI Keyboard 0.8.0 — fully offline. No accounts, no telemetry, no network."))

        return ScrollView(this).apply { addView(root) }
    }

    // ---- view helpers --------------------------------------------------------

    private fun heading(t: String) = TextView(this).apply {
        text = t; textSize = 24f; setPadding(0, 0, 0, dp(6))
    }

    private fun section(t: String) = TextView(this).apply {
        text = t; textSize = 16f; setPadding(0, dp(20), 0, dp(6))
    }

    private fun sub(t: String) = TextView(this).apply {
        text = t; textSize = 14f; setPadding(0, 0, 0, dp(8))
    }

    private fun button(t: String, onClick: () -> Unit) = android.widget.Button(this).apply {
        text = t; gravity = Gravity.START; setOnClickListener { onClick() }
    }

    private fun row(title: String, subtitle: String, onClick: () -> Unit): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            isClickable = true
            setOnClickListener { onClick() }
        }
        c.addView(TextView(this).apply { text = title; textSize = 17f })
        c.addView(TextView(this).apply { text = subtitle; textSize = 13f })
        return c
    }

    private fun toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        c.addView(TextView(this).apply { text = title; textSize = 16f },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        c.addView(SwitchCompat(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, value -> onChange(value) }
        })
        return c
    }

    private fun info(body: String) = Toast.makeText(this, body, Toast.LENGTH_LONG).show()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
