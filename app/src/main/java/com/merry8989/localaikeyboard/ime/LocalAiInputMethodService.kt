package com.merry8989.localaikeyboard.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Toast
import com.merry8989.localaikeyboard.App
import com.merry8989.localaikeyboard.ai.AiAction
import com.merry8989.localaikeyboard.lingua.LanguageMode
import com.merry8989.localaikeyboard.settings.SettingsActivity
import com.merry8989.localaikeyboard.settings.StickerActivity
import com.merry8989.localaikeyboard.stickers.StickerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The keyboard. Wires everything together:
 *   KeyboardView  -> keys, themes, outlines, long-press alternates, backspace
 *   SuggestionBar -> candidates, next-word predictions + ✦
 *   EmojiPanel / StickerPanel / ClipboardPanel / AiPanel -> the four side panels
 */
class LocalAiInputMethodService :
    InputMethodService(),
    KeyboardView.Listener,
    SuggestionBar.Listener,
    AiPanel.Listener {

    private lateinit var bridge: EditorBridge
    private lateinit var keyboardView: KeyboardView
    private lateinit var suggestionBar: SuggestionBar
    private lateinit var aiPanel: AiPanel
    private lateinit var altStrip: AlternateStrip
    private lateinit var emojiPanel: EmojiPanel
    private lateinit var stickerPanel: StickerPanel
    private lateinit var clipboardPanel: ClipboardPanel
    private lateinit var themePrefs: ThemePrefs
    private lateinit var stickerStore: StickerStore

    private var language = LanguageMode.AUTO
    private var showingSymbols = false
    private var caps = false
    private var lastTopCandidate: String? = null

    private data class Correction(val original: String, val corrected: String)
    private var lastCorrection: Correction? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var suggestJob: Job? = null
    private var backspaceJob: Job? = null

    private val app get() = App.instance
    private val theme: KeyboardTheme get() = themePrefs.theme()

    override fun onCreate() {
        super.onCreate()
        themePrefs = ThemePrefs(this)
        stickerStore = StickerStore(this)
    }

    override fun onCreateInputView(): View {
        bridge = EditorBridge(this)
        val t = theme

        suggestionBar = SuggestionBar(this, this).apply { setTheme(t) }
        altStrip = AlternateStrip(this) { char -> commitText(char) }
            .apply { visibility = View.GONE }
        emojiPanel = EmojiPanel(this) { emoji -> commitText(emoji) }
            .apply { visibility = View.GONE }
        clipboardPanel = ClipboardPanel(this) { text -> pasteClip(text) }
            .apply { visibility = View.GONE }
        aiPanel = AiPanel(this, this).apply { visibility = View.GONE }
        stickerPanel = StickerPanel(this, stickerStore, ::onStickerPicked, ::openStickerManager)
            .apply { visibility = View.GONE }
        keyboardView = KeyboardView(this, this).apply {
            this.theme = t
            this.keyStyle = themePrefs.keyStyle()
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(suggestionBar, LinearLayout.LayoutParams(MATCH_PARENT, dp(46)))
            addView(altStrip, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)))
            addView(emojiPanel, LinearLayout.LayoutParams(MATCH_PARENT, dp(200)))
            addView(clipboardPanel, LinearLayout.LayoutParams(MATCH_PARENT, dp(150)))
            addView(aiPanel, LinearLayout.LayoutParams(MATCH_PARENT, dp(150)))
            addView(stickerPanel, LinearLayout.LayoutParams(MATCH_PARENT, dp(96)))
            addView(keyboardView, LinearLayout.LayoutParams(MATCH_PARENT, dp(themePrefs.keyHeightDp)))
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        caps = false
        showingSymbols = false
        lastCorrection = null
        val t = theme
        suggestionBar.setTheme(t)
        keyboardView.theme = t
        keyboardView.keyStyle = themePrefs.keyStyle()
        keyboardView.setLayout(KeyboardLayout.qwerty)
        keyboardView.setCaps(false)
        applyKeyHeight()
        hidePanels()
        if (themePrefs.clipboardHistory) {
            app.clipboardStore.onChange = { rebuildClipboardIfOpen() }
            app.clipboardStore.start()
        }
        refreshSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        suggestJob?.cancel()
        backspaceJob?.cancel()
        app.clipboardStore.onChange = null
        app.clipboardStore.stop()
        super.onFinishInputView(finishingInput)
    }

    // ---- KeyboardView.Listener ----------------------------------------------

    override fun onKey(key: KeyDef, label: String) {
        altStrip.hide()
        when (key.code) {
            KeyCode.CHAR -> {
                lastCorrection = null
                val out = if (caps) label else label.lowercase()
                bridge.commit(out)
                if (caps) {
                    caps = false
                    keyboardView.setCaps(false)
                }
                refreshSuggestions()
            }
            KeyCode.SPACE -> handleSpace()
            KeyCode.BACKSPACE -> bridge.backspace()
            KeyCode.ENTER -> {
                val prev = wordBeforeCurrent()
                bridge.commit("\n")
                lastCorrection = null
                app.learnedStore.observeWord(prev, "")   // no word after Enter
                applyAutoCaps()
            }
            KeyCode.SHIFT -> {
                caps = !caps
                keyboardView.setCaps(caps)
            }
            KeyCode.SYMBOLS -> {
                showingSymbols = !showingSymbols
                keyboardView.setLayout(
                    if (showingSymbols) KeyboardLayout.symbols else KeyboardLayout.qwerty
                )
            }
            KeyCode.LANG -> cycleLanguage()
            KeyCode.AI -> {
                hidePanels()
                openAi()
            }
            KeyCode.EMOJI -> toggleEmoji()
            KeyCode.STICKER -> toggleStickers()
            KeyCode.SETTINGS -> openSettings()
            KeyCode.CLIPBOARD -> toggleClipboard()
        }
    }

    override fun onAlternates(variants: List<String>) {
        hidePanels()
        altStrip.show(variants, theme)
    }

    override fun onBackspaceHoldStart() {
        if (undoCorrection()) return
        backspaceJob?.cancel()
        backspaceJob = scope.launch {
            bridge.backspace()
            delay(380)
            while (isActive) {
                bridge.backspace()
                delay(55)
            }
        }
    }

    override fun onBackspaceHoldStop() {
        backspaceJob?.cancel()
        backspaceJob = null
    }

    override fun onDeleteWord() {
        bridge.deleteWord()
    }

    override fun onStickerRequest() {
        toggleStickers()
    }

    override fun onClipboardRequest() {
        toggleClipboard()
    }

    // ---- SuggestionBar.Listener ---------------------------------------------

    override fun onCandidate(text: String) {
        val word = bridge.currentWordBeforeCursor()
        val prev = wordBeforeCurrent()
        if (word.isNotEmpty()) bridge.deleteBefore(word.length)
        bridge.commit("$text ")
        if (word.isNotEmpty()) lastCorrection = Correction(word, text)
        app.learnedStore.observeWord(prev, text)
        suggestionBar.setCandidates(emptyList())
        lastTopCandidate = null
    }

    override fun onAi() = openAi()

    // ---- AiPanel.Listener ----------------------------------------------------

    override fun onAction(action: AiAction) {
        val input = bridge.contextForAi()
        if (input.isBlank()) {
            aiPanel.setStatus("Type or select some text first.")
            return
        }
        aiPanel.setStatus("Thinking… (on-device)")
        scope.launch {
            val controller = app.aiController
            controller.ensureLoaded()
            if (!controller.isReady) {
                aiPanel.setStatus("No model installed. Open the app's Settings to download one.")
                return@launch
            }
            val builder = StringBuilder()
            try {
                controller.run(action, input, language).collect { token ->
                    builder.append(token)
                    aiPanel.setStreaming(builder.toString())
                }
            } catch (t: Throwable) {
                aiPanel.setStatus("AI error: ${t.message}")
            }
        }
    }

    override fun onAccept(text: String) {
        if (text.isNotBlank()) bridge.replaceSelection(text)
        aiPanel.hide()
    }

    override fun onDismiss() = aiPanel.hide()

    // ---- panels --------------------------------------------------------------

    private fun hidePanels() {
        aiPanel.hide()
        altStrip.hide()
        emojiPanel.hide()
        stickerPanel.hide()
        clipboardPanel.hide()
    }

    private fun toggleEmoji() {
        val show = !emojiPanel.isShowing()
        hidePanels()
        if (show) emojiPanel.show(theme)
    }

    private fun toggleStickers() {
        val show = !stickerPanel.isShowing()
        hidePanels()
        if (show) stickerPanel.show(theme)
    }

    private fun toggleClipboard() {
        val show = !clipboardPanel.isShowing()
        hidePanels()
        if (!show) return
        // Read the clipboard fresh, so anything you just copied shows up first.
        app.clipboardStore.refresh()
        clipboardPanel.show(theme, app.clipboardStore.allItems())
    }

    private fun rebuildClipboardIfOpen() {
        if (clipboardPanel.isShowing()) {
            clipboardPanel.show(theme, app.clipboardStore.allItems())
        }
    }

    private fun pasteClip(text: String) {
        bridge.commit(text)
        clipboardPanel.hide()
        refreshSuggestions()
    }

    private fun onStickerPicked(file: File) {
        stickerStore.copyToClipboard(file)
        toast("Sticker copied — paste it in your chat app")
    }

    private fun openStickerManager() {
        startActivity(
            Intent(this, StickerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun openSettings() {
        startActivity(
            Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---- internals -----------------------------------------------------------

    private fun commitText(text: String) {
        bridge.finishComposing()
        bridge.commit(text)
        refreshSuggestions()
    }

    private fun handleSpace() {
        // Double-space -> ". "
        val before = bridge.textBeforeCursor(3)
        if (themePrefs.doubleSpacePeriod && before.endsWith(" ") &&
            !before.trimEnd().endsWith(".")
        ) {
            bridge.deleteBefore(1)
            bridge.commit(". ")
            lastCorrection = null
            suggestionBar.setCandidates(emptyList())
            lastTopCandidate = null
            applyAutoCaps()
            return
        }

        val word = bridge.currentWordBeforeCursor()
        val prev = wordBeforeCurrent()
        val top = lastTopCandidate
        val finalWord: String
        if (themePrefs.autoCorrect && word.isNotEmpty() && top != null &&
            !top.equals(word, ignoreCase = true)
        ) {
            bridge.deleteBefore(word.length)
            bridge.commit("$top ")
            lastCorrection = Correction(word, top)
            finalWord = top
        } else {
            if (themePrefs.learnWords && word.length >= 3) app.spellEngine.learn(word)
            bridge.commit(" ")
            lastCorrection = null
            finalWord = word
        }
        // Learn what you actually use, for next-word prediction.
        if (finalWord.isNotEmpty()) app.learnedStore.observeWord(prev, finalWord)

        suggestionBar.setCandidates(emptyList())
        lastTopCandidate = null
        applyAutoCaps()
    }

    private fun undoCorrection(): Boolean {
        val c = lastCorrection ?: return false
        lastCorrection = null
        val before = bridge.textBeforeCursor(c.corrected.length + 1)
        if (before.endsWith(" ") && before.trimEnd().equals(c.corrected, ignoreCase = true)) {
            bridge.deleteBefore(c.corrected.length + 1)
            bridge.commit(c.original + " ")
            refreshSuggestions()
            return true
        }
        return false
    }

    /** The word immediately before the one being typed. */
    private fun wordBeforeCurrent(): String? {
        val before = bridge.textBeforeCursor(128)
        val stripped = before.dropLastWhile { it.isLetter() || it == '\'' }
        val prev = stripped.trimEnd().takeLastWhile { it.isLetter() || it == '\'' }
        return prev.ifEmpty { null }
    }

    private fun applyAutoCaps() {
        if (!themePrefs.autoCapitalize) return
        val before = bridge.textBeforeCursor(3)
        val trimmed = before.trimEnd()
        val shouldCap = trimmed.isEmpty() ||
            before.endsWith("\n") ||
            trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")
        if (shouldCap && !caps) {
            caps = true
            keyboardView.setCaps(true)
        }
    }

    private fun applyKeyHeight() {
        val lp = keyboardView.layoutParams ?: return
        lp.height = dp(themePrefs.keyHeightDp)
        keyboardView.layoutParams = lp
    }

    private fun openAi() {
        aiPanel.show()
        aiPanel.setStatus("Pick an action. Runs fully on your device.")
    }

    private fun cycleLanguage() {
        language = language.next()
        suggestionBar.setCandidates(listOf(language.tag))
        refreshSuggestions(delayMs = 400)
    }

    private fun refreshSuggestions(delayMs: Long = 120) {
        suggestJob?.cancel()
        suggestJob = scope.launch {
            delay(delayMs)
            val word = bridge.currentWordBeforeCursor()
            if (word.isEmpty()) {
                // No partial word: predict the next word from what you've used.
                val prev = wordBeforeCurrent()
                val predictions = withContext(Dispatchers.Default) {
                    if (prev == null) emptyList() else app.spellEngine.predictNext(prev)
                }
                suggestionBar.setCandidates(predictions)
                lastTopCandidate = null
                return@launch
            }
            val results = withContext(Dispatchers.Default) {
                app.spellEngine.suggest(word, language)
            }
            suggestionBar.setCandidates(results)
            lastTopCandidate = results.firstOrNull()
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT
    }
}
