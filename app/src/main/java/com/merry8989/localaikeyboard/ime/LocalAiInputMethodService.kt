package com.merry8989.localaikeyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.LinearLayout
import com.merry8989.localaikeyboard.App
import com.merry8989.localaikeyboard.ai.AiAction
import com.merry8989.localaikeyboard.lingua.LanguageMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The keyboard. Wires the three pieces together:
 *   KeyboardView  -> keys
 *   SuggestionBar -> candidates + ✦
 *   AiPanel       -> on-device LLM actions
 * All text mutation goes through [EditorBridge].
 */
class LocalAiInputMethodService :
    InputMethodService(),
    KeyboardView.KeyListener,
    SuggestionBar.Listener,
    AiPanel.Listener {

    private lateinit var bridge: EditorBridge
    private lateinit var keyboardView: KeyboardView
    private lateinit var suggestionBar: SuggestionBar
    private lateinit var aiPanel: AiPanel

    private var language = LanguageMode.AUTO
    private var showingSymbols = false
    private var caps = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var suggestJob: Job? = null

    private val app get() = App.instance

    override fun onCreateInputView(): View {
        bridge = EditorBridge(this)

        suggestionBar = SuggestionBar(this, this)
        aiPanel = AiPanel(this, this).apply { visibility = View.GONE }
        keyboardView = KeyboardView(this, this)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(suggestionBar, LinearLayout.LayoutParams(MATCH_PARENT, dp(46)))
            addView(aiPanel, LinearLayout.LayoutParams(MATCH_PARENT, dp(150)))
            addView(keyboardView, LinearLayout.LayoutParams(MATCH_PARENT, dp(230)))
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        caps = false
        showingSymbols = false
        keyboardView.setLayout(KeyboardLayout.qwerty)
        keyboardView.setCaps(false)
        aiPanel.hide()
        refreshSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        suggestJob?.cancel()
        super.onFinishInputView(finishingInput)
    }

    // ---- KeyboardView.KeyListener -------------------------------------------

    override fun onKey(key: KeyDef, label: String) {
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
            KeyCode.SPACE -> {
                val word = bridge.currentWordBeforeCursor()
                val top = lastTopCandidate
                if (word.isNotEmpty() && top != null && !top.equals(word, ignoreCase = true)) {
                    // auto-correct to the top suggestion
                    bridge.deleteBefore(word.length)
                    bridge.commit("$top ")
                } else {
                    bridge.commit(" ")
                }
                suggestionBar.setCandidates(emptyList())
                lastTopCandidate = null
            }
            KeyCode.BACKSPACE -> {
                bridge.backspace()
                refreshSuggestions()
            }
            KeyCode.ENTER -> bridge.commit("\n")
            KeyCode.SHIFT -> {
                caps = !caps
                keyboardView.setCaps(caps)
            }
            KeyCode.SYMBOLS -> {
                showingSymbols = !showingSymbols
                keyboardView.setLayout(if (showingSymbols) KeyboardLayout.symbols else KeyboardLayout.qwerty)
            }
            KeyCode.LANG -> cycleLanguage()
            KeyCode.AI -> openAi()
        }
    }

    // ---- SuggestionBar.Listener ---------------------------------------------

    private var lastTopCandidate: String? = null

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

    // ---- internals -----------------------------------------------------------

    private fun openAi() {
        aiPanel.show()
        aiPanel.setStatus("Pick an action. Runs fully on your device.")
    }

    private fun cycleLanguage() {
        language = when (language) {
            LanguageMode.AUTO -> LanguageMode.ENGLISH
            LanguageMode.ENGLISH -> LanguageMode.HINGLISH
            LanguageMode.HINGLISH -> LanguageMode.AUTO
        }
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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT
    }
}
