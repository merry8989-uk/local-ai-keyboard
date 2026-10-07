package com.merry8989.localaikeyboard.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * A single background thread for all LLM work. The MediaPipe graph and llama.cpp
 * contexts are not safe to drive from multiple threads, so everything AI goes
 * through here.
 */
object AiDispatcher {
    val dispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r -> Thread(r, "ai-engine") }.asCoroutineDispatcher()
}
