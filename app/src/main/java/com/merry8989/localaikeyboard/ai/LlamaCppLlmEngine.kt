package com.merry8989.localaikeyboard.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Alternative backend for users who prefer GGUF quantised models (Q4_K_M) and
 * no Google runtime dependency, via llama.cpp compiled for arm64 and driven
 * through JNI.
 *
 * This is a deliberate stub: it documents the integration points so the backend
 * can be swapped in without touching the rest of the app. To make it real:
 *   1. Add the prebuilt `libllama` + your JNI bridge as a CMake target under
 *      `app/src/main/cpp/` and enable `externalNativeBuild` in build.gradle.
 *   2. Implement `nativeLoad` / `nativeGenerate` below.
 *   3. Point [com.merry8989.localaikeyboard.ai.AiController] at this engine.
 */
class LlamaCppLlmEngine : LlmEngine {

    private var loaded = false

    override val isReady: Boolean get() = loaded

    override suspend fun load(modelPath: String) {
        // TODO: nativeLoad(modelPath, threads = Runtime.getRuntime().availableProcessors())
        loaded = false
        throw UnsupportedOperationException(
            "LlamaCppLlmEngine is a stub. Wire up the JNI bridge (see class docs) or use MediaPipeLlmEngine."
        )
    }

    override fun generate(prompt: String, maxTokens: Int): Flow<String> = flow {
        // TODO: loop over nativeGenerate(prompt, maxTokens) token ids, decode, emit.
        throw UnsupportedOperationException("Not implemented")
    }

    override fun close() {
        // TODO: nativeFree()
        loaded = false
    }
}
