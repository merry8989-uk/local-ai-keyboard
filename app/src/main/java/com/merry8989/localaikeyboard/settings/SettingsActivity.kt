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
        root.addView(row("Keyboard", "Number row, key size") {
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


        // ---- Explore more apps by us
        root.addView(section("Explore more apps by us"))
        root.addView(sub("Other apps we build. Free, no ads, no accounts."))
        for (app in EXPLORE_APPS) {
            root.addView(row(app.name, app.tagline) { openUrl(app.url) })
        }

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

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
    }

    /** Cross-promotion list. Never contains this app itself. */
    private data class ExploreApp(val name: String, val tagline: String, val url: String)

    private val EXPLORE_APPS = listOf(
        ExploreApp(
            name = "Chronos",
            tagline = "Exact alarms with holiday skipping, world clock, timer presets, lap stopwatch",
            url = "https://merry8989-uk.github.io/chronos-clock/",
        ),
        ExploreApp(
            name = "Universal AI Calculator",
            tagline = "Standard, scientific, unit converter, programmer, date tools + 47 calculators",
            url = "https://merry8989-uk.github.io/universal-ai-calculator/",
        ),
        ExploreApp(
            name = "ProGallery",
            tagline = "Full-featured gallery: MediaStore indexing, duplicate detection, photo/video editing",
            url = "https://github.com/merry8989-uk/pro-gallery-android",
        ),
        ExploreApp(
            name = "Kavach",
            tagline = "Indian-first AI caller ID, smart blocking, call recording, AI call assistant",
            url = "https://merry8989-uk.github.io/kavach/",
        ),
        ExploreApp(
            name = "Low Distraction Launcher",
            tagline = "Minimal text-only launcher, no icons, no feeds, no internet access",
            url = "https://merry8989-uk.github.io/lowdistraction-launcher/",
        ),
        ExploreApp(
            name = "FloatScript",
            tagline = "Transparent floating teleprompter for Android",
            url = "https://github.com/merry8989-uk/floatscript",
        ),
        ExploreApp(
            name = "Camara",
            tagline = "Camera + editor: filters, stickers, beauty tools, teleprompter, image & video editing",
            url = "https://merry8989-uk.github.io/camara-app/",
        ),
        ExploreApp(
            name = "Powerbase",
            tagline = "Self-hosted diagnostics client: dashboards, log explorer, crash groups, devices, alerts",
            url = "https://merry8989-uk.github.io/powerbase-app/",
        ),
        ExploreApp(
            name = "Arrow Puzzle Advanced",
            tagline = "12,000 guaranteed-solvable arrow puzzles (6,000 Classic + 6,000 Escape)",
            url = "https://raw.githubusercontent.com/merry8989-uk/arrow-puzzle-advanced/main/dist/ArrowPuzzleAdvanced-5.11.apk",
        ),
        ExploreApp(
            name = "New Arrow Puzzle",
            tagline = "Offline HTML5 arrow-clearing game, 12,000 verified levels, hard mode from 25",
            url = "https://merry8989-uk.github.io/new-arrow-puzzle/",
        ),
        ExploreApp(
            name = "PDF Toolkit",
            tagline = "Merge, split, rotate, watermark, convert, protect PDFs in the browser. No uploads",
            url = "https://merry8989-uk.github.io/pdf-toolkit/",
        ),
        ExploreApp(
            name = "Doc Viewer & Editor",
            tagline = "Open, edit, convert, compress, merge docs, images, sheets, PDFs, ebooks",
            url = "https://merry8989-uk.github.io/doc-viewer-editor/",
        ),
        ExploreApp(
            name = "Dhoondh",
            tagline = "Multi-tab search engine: AI, images, videos, papers, books, PDF, Drive, Archive",
            url = "https://merry8989-uk.github.io/dhoondh/",
        ),
        ExploreApp(
            name = "Pulse",
            tagline = "Social feed for text, photos and video. Single-file static site",
            url = "https://merry8989-uk.github.io/pulse-feed/",
        ),
        ExploreApp(
            name = "KaamSetu",
            tagline = "Job vacancy portal with employer posting and admin moderation",
            url = "https://merry8989-uk.github.io/kaamsetu-job-portal/",
        ),
        ExploreApp(
            name = "48 Laws of Power",
            tagline = "Hindi notes, mindmap, examples and usage for the 48 laws",
            url = "https://merry8989-uk.github.io/48-laws-of-power/",
        ),
        ExploreApp(
            name = "On This Day",
            tagline = "Daily bilingual Hindi + English history briefing, static site",
            url = "https://merry8989-uk.github.io/on-this-day/",
        ),
        ExploreApp(
            name = "USB Screen Mirror",
            tagline = "Low-latency Android-to-Windows USB mirroring, scrcpy wrapper with GUI",
            url = "https://github.com/merry8989-uk/usb-screen-mirror",
        ),
        ExploreApp(
            name = "Sangam",
            tagline = "India-hosted social + video platform on its own stack",
            url = "https://github.com/merry8989-uk/sangam",
        ),
        ExploreApp(
            name = "Powerbase Server",
            tagline = "Self-hosted observability: log ingestion, explorer, traces, metrics, alerting",
            url = "https://github.com/merry8989-uk/powerbase-server",
        ),
    )
}
