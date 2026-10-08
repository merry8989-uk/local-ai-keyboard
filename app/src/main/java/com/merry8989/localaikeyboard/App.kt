package com.merry8989.localaikeyboard

import android.app.Application
import com.merry8989.localaikeyboard.ai.AiController
import com.merry8989.localaikeyboard.ai.LocalTextEngine
import com.merry8989.localaikeyboard.clip.ClipboardStore
import com.merry8989.localaikeyboard.ime.ThemePrefs
import com.merry8989.localaikeyboard.spell.CustomWordsStore
import com.merry8989.localaikeyboard.spell.LearnedStore
import com.merry8989.localaikeyboard.spell.SpellEngine
import com.merry8989.localaikeyboard.spell.UserDictionary

/**
 * Process-wide singletons. Everything here is local to the device; the app
 * requests no network permission and stores no data off-device.
 */
class App : Application() {

    val prefs: ThemePrefs by lazy { ThemePrefs(this) }

    val userDictionary: UserDictionary by lazy { UserDictionary(this) }

    val learnedStore: LearnedStore by lazy { LearnedStore(this) }

    val customWordsStore: CustomWordsStore by lazy { CustomWordsStore(this) }

    val spellEngine: SpellEngine by lazy { SpellEngine(this, userDictionary, prefs, learnedStore) }

    val clipboardStore: ClipboardStore by lazy { ClipboardStore(this) }

    val aiController: AiController by lazy {
        AiController(LocalTextEngine(spellEngine, learnedStore))
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
