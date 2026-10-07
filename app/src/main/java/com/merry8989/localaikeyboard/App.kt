package com.merry8989.localaikeyboard

import android.app.Application
import com.merry8989.localaikeyboard.ai.AiController
import com.merry8989.localaikeyboard.ai.MediaPipeLlmEngine
import com.merry8989.localaikeyboard.settings.ModelManager
import com.merry8989.localaikeyboard.spell.SpellEngine
import com.merry8989.localaikeyboard.spell.UserDictionary

/**
 * Process-wide singletons. The keyboard service and the settings screens share
 * one spell index, one user dictionary, one model manager and one AI controller,
 * so the model is loaded at most once per process.
 */
class App : Application() {

    val userDictionary: UserDictionary by lazy { UserDictionary(this) }

    val spellEngine: SpellEngine by lazy { SpellEngine(this, userDictionary) }

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
