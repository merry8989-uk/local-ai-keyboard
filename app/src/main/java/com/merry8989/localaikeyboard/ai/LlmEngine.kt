package com.merry8989.localaikeyboard.ai

import kotlinx.coroutines.flow.Flow

/**
 * Backend-agnostic on-device text generation. Implementations must be
 * single-threaded (the underlying runtimes are not thread-safe); callers use
 * [com.merry8989.localaikeyboard.util.AiDispatcher].
 */
interface LlmEngine {

    /** True once [load] has succeeded. */
    val isReady: Boolean

    /** Load (or reload) a model from a local file path. */
    suspend fun load(modelPath: String)

    /** Stream generated text for [prompt], token chunk by chunk. */
    fun generate(prompt: String, maxTokens: Int = 256): Flow<String>

    /** Release native resources. */
    fun close()
}
