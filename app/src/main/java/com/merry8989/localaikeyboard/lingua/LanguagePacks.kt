package com.merry8989.localaikeyboard.lingua

import android.content.Context

/**
 * A language the keyboard can suggest in. Everything ships bundled in the app —
 * there is nothing to download.
 *
 * Two kinds of pack:
 *  - a **word list** (one word per line) used for spelling suggestions;
 *  - a **transliteration map** (`roman<TAB>devanagari`) so typing the Roman form
 *    suggests the native-script word.
 */
data class LanguagePack(
    val id: String,
    val name: String,
    val nativeName: String,
    val assetFile: String? = null,
    val alwaysOn: Boolean = false,
) {
    val isBundled: Boolean get() = assetFile != null
    val isTranslit: Boolean get() = assetFile?.endsWith("translit.txt") == true
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

    /** Hindi in Devanagari, via Roman transliteration. Built in. */
    val HINDI = LanguagePack(
        id = "hi",
        name = "Hindi",
        nativeName = "हिन्दी",
        assetFile = "hindi_translit.txt",
    )

    val all: List<LanguagePack> = listOf(ENGLISH, HINGLISH, HINDI)

    fun byId(id: String): LanguagePack? = all.firstOrNull { it.id == id }

    /** Words for a pack (empty for transliteration packs). */
    fun loadWords(context: Context, pack: LanguagePack): List<String> {
        val asset = pack.assetFile ?: return emptyList()
        if (pack.isTranslit) return emptyList()
        return runCatching {
            context.assets.open(asset).bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            }
        }.getOrDefault(emptyList())
    }

    /** Roman -> native script map for a transliteration pack. */
    fun loadTranslit(context: Context, pack: LanguagePack): Map<String, String> {
        val asset = pack.assetFile ?: return emptyMap()
        if (!pack.isTranslit) return emptyMap()
        return runCatching {
            context.assets.open(asset).bufferedReader().useLines { lines ->
                lines.map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .mapNotNull { line ->
                        val parts = line.split('\t')
                        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                            parts[0].lowercase() to parts[1]
                        } else null
                    }
                    .toMap()
            }
        }.getOrDefault(emptyMap())
    }
}
