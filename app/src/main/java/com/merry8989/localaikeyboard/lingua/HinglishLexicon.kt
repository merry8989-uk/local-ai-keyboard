package com.merry8989.localaikeyboard.lingua

import android.content.Context

/**
 * Loads the bundled Romanised-Hindi word list from assets and exposes it as a
 * frequency-ordered map. Words are stored one per line, most common first, so
 * frequency is derived from rank — no counts to maintain by hand.
 */
class HinglishLexicon(private val context: Context) {

    /** word -> pseudo-frequency (higher = more common). */
    val entries: Map<String, Int> by lazy { load() }

    private fun load(): Map<String, Int> {
        val words = readAsset("hinglish_words.txt")
        val total = words.size.coerceAtLeast(1)
        return words.withIndex().associate { (i, w) -> w to (total - i) * 40 }
    }

    private fun readAsset(name: String): List<String> =
        runCatching {
            context.assets.open(name).bufferedReader().useLines { lines ->
                lines.map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
            }
        }.getOrDefault(emptyList())

    fun contains(word: String) = entries.containsKey(word.lowercase())

    fun isHinglish(word: String): Boolean {
        val w = word.lowercase()
        if (entries.containsKey(w)) return true
        // Words the lexicon doesn't list can still *look* Hinglish by their ending.
        return HinglishMarkers.suffixes.any { w.endsWith(it) }
    }
}

/** Small heuristics that flag a token as Romanised Hindi. */
object HinglishMarkers {
    val suffixes = listOf("hai", "nahi", "kya", "ko", "ka", "ki", "ke", "mein", "se", "wala", "kar", "raha", "rahi")
}
