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
 * The keyboard. Wires the pieces together:
 *   KeyboardView  -> keys, themes, long-press alternates, backspace hold
 *   SuggestionBar -> candidates + ✦
 *   AiPanel       -> on-device LLM actions
 *   StickerPanel  -> local stickers (copied to clipboard)
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
    private lateinit var stickerPanel: StickerPanel
    private lateinit var themePrefs: ThemePrefs
    private lateinit var stickerStore: StickerStore

    private var language = LanguageMode.AUTO
    private var showingSymbols = false
    private var caps = false
    private var lastTopCandidate: String? = null

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
        altStrip = AlternateStrip(this) { char -> commitAlternate(char) }
            .apply { visibility = View.GONE }
        aiPanel = AiPanel(this, this).apply { visibility = View.GONE }
        stickerPanel = StickerPanel(this, stickerStore, ::onStickerPicked, ::openStickerManager)
            .apply { visibility = View.GONE }
        keyboardView = KeyboardView(this, this).apply { this.theme = t }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(suggestionBar, LinearLayout.LayoutParams(MATCH_PARENT, dp(46)))
            addView(altStrip, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)))
            addView(aiPanel, LinearLayout.LayoutParams(MATCH_PARENT, dp(150)))
            addView(stickerPanel, LinearLayout.LayoutParams(MATCH_PARENT, dp(96)))
            addView(keyboardView, LinearLayout.LayoutParams(MATCH_PARENT, dp(260)))
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        caps = false
        showingSymbols = false
        val t = theme
        suggestionBar.setTheme(t)
        keyboardView.theme = t
        keyboardView.setLayout(KeyboardLayout.qwerty)
        keyboardView.setCaps(false)
        aiPanel.hide()
        altStrip.hide()
        stickerPanel.hide()
        refreshSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        suggestJob?.cancel()
        backspaceJob?.cancel()
        super.onFinishInputView(finishingInput)
    }

    // ---- KeyboardView.Listener ----------------------------------------------

    override fun onKey(key: KeyDef, label: String) {
        altStrip.hide()
        when (key.code) {
            KeyCode.CHAR -> {
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
            KeyCode.ENTER -> bridge.commit("\n")
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
                stickerPanel.hide()
                openAi()
            }
            KeyCode.STICKER -> toggleStickers()
        }
    }

    override fun onAlternates(variants: List<String>) {
        aiPanel.hide()
        stickerPanel.hide()
        altStrip.show(variants, theme)
    }

    override fun onBackspaceHoldStart() {
        backspaceJob?.cancel()
        backspaceJob = scope.launch {
            bridge.backspace()          // one immediately
            delay(380)                  // then repeat while held
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

    // ---- SuggestionBar.Listener ---------------------------------------------

    override fun onCandidate(text: String) {
        val word = bridge.currentWordBeforeCursor()
        bridge.deleteBefore(word.length)
        bridge.commit("$text ")
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

    // ---- stickers ------------------------------------------------------------

    private fun toggleStickers() {
        aiPanel.hide()
        altStrip.hide()
        if (stickerPanel.isShowing()) stickerPanel.hide() else stickerPanel.show(theme)
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

    // ---- internals -----------------------------------------------------------

    private fun commitAlternate(char: String) {
        bridge.commit(char)
        refreshSuggestions()
    }

    private fun handleSpace() {
        val word = bridge.currentWordBeforeCursor()
        val top = lastTopCandidate
        if (word.isNotEmpty() && top != null && !top.equals(word, ignoreCase = true)) {
            bridge.deleteBefore(word.length)
            bridge.commit("$top ")
        } else {
            bridge.commit(" ")
        }
        suggestionBar.setCandidates(emptyList())
        lastTopCandidate = null
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
            if (word.length < 1) {
                suggestionBar.setCandidates(emptyList())
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
