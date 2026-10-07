package com.merry8989.localaikeyboard.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.merry8989.localaikeyboard.App
import com.merry8989.localaikeyboard.ime.ThemePrefs

/**
 * The hub. Mirrors the usual keyboard-app layout: a Set up section, an AI model
 * section, then categories (Languages, Theme, Keyboard, Typing, Gestures,
 * Clipboard, Emojis, Stickers, About).
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: ThemePrefs
    private val models get() = App.instance.modelManager

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
        root.addView(sub("An offline keyboard with on-device AI. Nothing you type ever leaves this phone."))

        // ---- Set up
        root.addView(section("Set up"))
        root.addView(button("Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(button("Choose input method") {
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
        })

        // ---- Categories
        root.addView(section("Settings"))
        root.addView(row("Languages", "English + Hinglish enabled") {
            info("Languages", "English and Hinglish (Romanised Hindi) are active. Cycle with the हि key on the keyboard.")
        })
        root.addView(row("Theme", "Pick from 24 built-in themes") {
            startActivity(Intent(this, ThemeActivity::class.java))
        })
        root.addView(row("Keyboard", "Layout, key feedback") {
            info("Keyboard", "QWERTY + a symbols page with currency, maths and extra signs. Long-press a key for accents and related characters.")
        })
        root.addView(row("Typing", "Autocorrect, sound, vibration") {
            info("Typing", "Autocorrect uses the on-device dictionary; toggle it below.")
        })
        root.addView(toggle("Auto-correct", prefs.autoCorrect) { prefs.autoCorrect = it })
        root.addView(toggle("Sound on keypress", prefs.soundEnabled) { prefs.soundEnabled = it })
        root.addView(toggle("Vibrate on keypress", prefs.vibrateEnabled) { prefs.vibrateEnabled = it })
        root.addView(row("Gestures", "Swipe to delete a word") {
            info("Gestures", "Hold ⌫ to keep deleting. Swipe left on ⌫ to delete a whole word.")
        })
        root.addView(row("Clipboard", "Private by design") {
            info("Clipboard", "This keyboard does not read or store your clipboard. Stickers you copy are written only when you tap them.")
        })
        root.addView(row("Emojis", "Use the ☺ key") {
            info("Emojis", "Tap ☺ on the keyboard to open your stickers & media.")
        })
        root.addView(row("Stickers & media", "Add photos, GIFs, PNGs") {
            startActivity(Intent(this, StickerActivity::class.java))
        })

        // ---- Model
        root.addView(section("On-device AI model"))
        val status = sub(statusText())
        root.addView(status)
        for (spec in ModelManager.AVAILABLE) {
            root.addView(button("Download ${spec.name} (${spec.approxLabel})") {
                val id = models.enqueueDownload(spec)
                if (id == -1L) toast("Download could not start.") else
                    toast("Downloading ${spec.name}. Reopen this screen when it finishes.")
            })
        }
        root.addView(button("Delete downloaded models") {
            var n = 0
            for (spec in ModelManager.AVAILABLE) if (models.delete(spec)) n++
            status.text = statusText()
            toast("Deleted $n model(s).")
        })

        root.addView(section("About"))
        root.addView(sub("Local AI Keyboard 0.1.0 — offline, privacy-first. No accounts, no telemetry."))

        return ScrollView(this).apply { addView(root) }
    }

    private fun statusText(): String {
        val installed = models.installedSpec()
        return if (installed != null) "Model ready: ${installed.name}. AI actions are available."
        else "No model installed — AI actions are disabled until you download one."
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

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
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

    private fun info(title: String, body: String) =
        Toast.makeText(this, body, Toast.LENGTH_LONG).show()

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
