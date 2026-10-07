package com.merry8989.localaikeyboard

import android.app.Application
import com.merry8989.localaikeyboard.ai.AiController
import com.merry8989.localaikeyboard.ai.MediaPipeLlmEngine
import com.merry8989.localaikeyboard.clip.ClipboardStore
import com.merry8989.localaikeyboard.ime.ThemePrefs
import com.merry8989.localaikeyboard.settings.ModelManager
import com.merry8989.localaikeyboard.spell.LearnedStore
import com.merry8989.localaikeyboard.spell.SpellEngine
import com.merry8989.localaikeyboard.spell.UserDictionary

/**
 * Process-wide singletons, shared by the keyboard service and the settings
 * screens so the model is loaded at most once per process.
 */
class App : Application() {

    val prefs: ThemePrefs by lazy { ThemePrefs(this) }

    val userDictionary: UserDictionary by lazy { UserDictionary(this) }

    val learnedStore: LearnedStore by lazy { LearnedStore(this) }

    val spellEngine: SpellEngine by lazy { SpellEngine(this, userDictionary, prefs, learnedStore) }

    val modelManager: ModelManager by lazy { ModelManager(this) }

    val clipboardStore: ClipboardStore by lazy { ClipboardStore(this) }

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
