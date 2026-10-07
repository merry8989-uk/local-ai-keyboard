package com.merry8989.localaikeyboard.ai

import android.content.Context
import com.merry8989.localaikeyboard.lingua.LanguageMode
import com.merry8989.localaikeyboard.settings.ModelManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sits between the IME and the [LlmEngine]: makes sure a model is loaded before
 * a request, and turns an [AiAction] + text into a streamed response.
 */
class AiController(
    @Suppress("unused") private val context: Context,
    private val engine: LlmEngine,
    private val models: ModelManager,
) {
    private val loadMutex = Mutex()

    val isReady: Boolean get() = engine.isReady

    /** Load the installed model once, if it isn't already in memory. */
    suspend fun ensureLoaded() {
        if (engine.isReady) return
        loadMutex.withLock {
            if (engine.isReady) return
            val path = models.activeModelPath() ?: return
            engine.load(path)
        }
    }

    /** Stream the result of [action] applied to [input]. */
    fun run(action: AiAction, input: String, mode: LanguageMode): Flow<String> {
        val prompt = PromptTemplates.build(action, input, mode)
        return engine.generate(prompt, PromptTemplates.maxTokensFor(action))
    }

    fun close() = engine.close()
}
