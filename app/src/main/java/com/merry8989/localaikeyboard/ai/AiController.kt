package com.merry8989.localaikeyboard.ai

import com.merry8989.localaikeyboard.lingua.LanguageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Runs the built-in text actions. No model, no download, no network — so it is
 * always ready.
 */
class AiController(private val engine: LocalTextEngine) {

    val isReady: Boolean get() = true

    /**
     * Streams the result in small chunks so the panel fills in progressively.
     * Chunks are incremental (each piece is appended by the caller).
     */
    fun run(action: AiAction, input: String, mode: LanguageMode): Flow<String> = flow {
        val result = engine.run(action, input, mode)
        if (result.isEmpty()) return@flow
        val words = result.split(" ")
        for ((i, w) in words.withIndex()) {
            emit(if (i == 0) w else " $w")
        }
    }

    fun close() { /* nothing to release */ }
}
