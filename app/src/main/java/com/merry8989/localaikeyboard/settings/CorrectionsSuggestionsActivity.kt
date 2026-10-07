package com.merry8989.localaikeyboard.settings

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.merry8989.localaikeyboard.ime.ThemePrefs

/**
 * "Corrections and suggestions": what the keyboard corrects, underlines and
 * suggests while you type. Everything runs on the device.
 */
class CorrectionsSuggestionsActivity : AppCompatActivity() {

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
            text = "Corrections and suggestions"; textSize = 22f; setPadding(0, 0, 0, dp(8))
        })

        root.addView(section("Automatic corrections"))
        root.addView(switchRow("Auto-correction", "Correct words while typing", prefs.autoCorrect) {
            prefs.autoCorrect = it
        })
        root.addView(switchRow("Auto-capitalisation",
            "Capitalise the first word of each sentence", prefs.autoCapitalize) {
            prefs.autoCapitalize = it
        })
        root.addView(switchRow("Double-space for period",
            "Tapping twice on spacebar inserts a period", prefs.doubleSpacePeriod) {
            prefs.doubleSpacePeriod = it
        })
        root.addView(switchRow("Auto-space after punctuation",
            "Insert a space after . , ? ! ; :", prefs.autoSpaceAfterPunctuation) {
            prefs.autoSpaceAfterPunctuation = it
        })

        root.addView(section("Spelling and grammar"))
        root.addView(switchRow("Spell check", "Underline misspelt words in red", prefs.spellCheck) {
            prefs.spellCheck = it
        })
        root.addView(switchRow("Grammar check", "Underline grammatical errors in blue", prefs.grammarCheck) {
            prefs.grammarCheck = it
        })

        root.addView(section("Suggestions"))
        root.addView(switchRow("Smart Compose", "Suggest words inline while typing", prefs.smartCompose) {
            prefs.smartCompose = it
        })
        root.addView(switchRow("Don't suggest offensive words",
            "Filter offensive words from suggestions", prefs.blockOffensive) {
            prefs.blockOffensive = it
        })
        root.addView(switchRow("Suggestion strip", "Show suggestions and access features",
            prefs.suggestionStrip) { prefs.suggestionStrip = it })
        root.addView(switchRow("Word suggestions", "Show in suggestion strip", prefs.wordSuggestions) {
            prefs.wordSuggestions = it
        })
        root.addView(switchRow("Next-word suggestions",
            "Use previous context to make suggestions", prefs.nextWordSuggestions) {
            prefs.nextWordSuggestions = it
        })
        root.addView(switchRow("Learn new words", "Add words you type to your dictionary",
            prefs.learnWords) { prefs.learnWords = it })

        root.addView(section("Contacts"))
        root.addView(switchRow("Suggest contacts",
            "Not available: this keyboard never reads your contacts", false, enabled = false) { })
        root.addView(TextView(this).apply {
            text = "Contact suggestions are deliberately off — the keyboard does not request " +
                "the contacts permission and never reads your address book."
            textSize = 12f
            setPadding(0, dp(4), 0, 0)
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun section(t: String) = TextView(this).apply {
        text = t; textSize = 16f; setPadding(0, dp(20), 0, dp(6))
    }

    private fun switchRow(
        title: String,
        subtitle: String,
        checked: Boolean,
        enabled: Boolean = true,
        onChange: (Boolean) -> Unit,
    ): View {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply { text = title; textSize = 16f })
        if (subtitle.isNotBlank()) col.addView(TextView(this).apply { text = subtitle; textSize = 12f })
        c.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        c.addView(SwitchCompat(this).apply {
            isChecked = checked
            isEnabled = enabled
            setOnCheckedChangeListener { _, v -> onChange(v) }
        })
        return c
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
