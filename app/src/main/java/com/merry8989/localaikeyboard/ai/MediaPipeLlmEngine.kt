package com.merry8989.localaikeyboard.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import com.merry8989.localaikeyboard.util.AiDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Default backend: Google's MediaPipe LLM Inference API running a Gemma-family
 * `.task` bundle on-device. GPU is used automatically where available.
 *
 * NOTE ON VERSIONS: the MediaPipe GenAI API is young and its method names have
 * shifted between releases. If this file fails to compile against the version
 * pinned in `app/build.gradle.kts`, check the current
 * `LlmInference` / `LlmInferenceOptions` signatures in the MediaPipe docs — the
 * shape below matches tasks-genai 0.10.x.
 */
class MediaPipeLlmEngine(private val context: Context) : LlmEngine {

    private var inference: LlmInference? = null

    override val isReady: Boolean get() = inference != null

    override suspend fun load(modelPath: String) = withContext(AiDispatcher.dispatcher) {
        close()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(1024)
            .setTemperature(0.8f)
            .build()
        inference = LlmInference.createFromOptions(context, options)
    }

    /**
     * Streams the response. MediaPipe fixes the token budget at load time, so
     * [maxTokens] here is advisory; the engine is created with a generous cap.
     */
    override fun generate(prompt: String, maxTokens: Int): Flow<String> = callbackFlow {
        val engine = inference
        if (engine == null) {
            close(IllegalStateException("Model not loaded"))
            return@callbackFlow
        }
        engine.generateResponseAsync(prompt, object : ProgressListener<String> {
            override fun run(partialResult: String, done: Boolean) {
                trySend(partialResult)
                if (done) close()
            }
        })
        awaitClose { /* generation cannot be cancelled mid-flight in this API */ }
    }

    override fun close() {
        inference?.close()
        inference = null
    }
}
