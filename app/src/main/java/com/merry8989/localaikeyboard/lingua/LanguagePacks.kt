package com.merry8989.localaikeyboard.lingua

import android.content.Context

/**
 * A language the keyboard can suggest in. English ships built in and is always
 * on. Other packs can be bundled in assets or downloaded as a plain word list
 * (one word per line, most common first).
 */
data class LanguagePack(
    val id: String,
    val name: String,
    val nativeName: String,
    val assetFile: String? = null,
    val downloadUrl: String? = null,
    val alwaysOn: Boolean = false,
) {
    val isBundled: Boolean get() = assetFile != null
    val isDownloadable: Boolean get() = downloadUrl != null
}

object LanguagePacks {

    /** English: built in, always enabled. */
    val ENGLISH = LanguagePack(
        id = "en",
        name = "English",
        nativeName = "English",
        assetFile = "en_words.txt",
        alwaysOn = true,
    )

    /** Hinglish: built in, on by default. */
    val HINGLISH = LanguagePack(
        id = "hi-roman",
        name = "Hinglish (Romanised Hindi)",
        nativeName = "Hinglish",
        assetFile = "hinglish_words.txt",
    )

    /**
     * Additional packs. To add a language, drop a word list into
     * `app/src/main/assets/<file>` (one word per line) and register it here —
     * or set [LanguagePack.downloadUrl] to a hosted list and the app will fetch
     * it on request. No made-up word lists are shipped.
     */
    val all: List<LanguagePack> = listOf(ENGLISH, HINGLISH)

    fun byId(id: String): LanguagePack? = all.firstOrNull { it.id == id }

    /** Words for a pack, read from assets (or empty if not available). */
    fun loadWords(context: Context, pack: LanguagePack): List<String> {
        val asset = pack.assetFile ?: return emptyList()
        return runCatching {
            context.assets.open(asset).bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            }
        }.getOrDefault(emptyList())
    }
}
