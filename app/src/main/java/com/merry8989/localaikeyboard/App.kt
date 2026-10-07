package com.merry8989.localaikeyboard

import android.app.Application
import com.merry8989.localaikeyboard.ai.AiController
import com.merry8989.localaikeyboard.ai.MediaPipeLlmEngine
import com.merry8989.localaikeyboard.settings.ModelManager
import com.merry8989.localaikeyboard.spell.SpellEngine

/**
 * Process-wide singletons. The keyboard service and the settings screen share
 * one spell index, one model manager and one AI controller so the model is
 * loaded at most once per process.
 */
class App : Application() {

    val spellEngine: SpellEngine by lazy { SpellEngine(this) }

    val modelManager: ModelManager by lazy { ModelManager(this) }

    val aiController: AiController by lazy {
        AiController(this, MediaPipeLlmEngine(this), modelManager)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
