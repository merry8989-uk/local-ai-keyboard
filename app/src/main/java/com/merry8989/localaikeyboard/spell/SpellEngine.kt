package com.merry8989.localaikeyboard.spell

import android.content.Context
import com.merry8989.localaikeyboard.lingua.HinglishLexicon
import com.merry8989.localaikeyboard.lingua.LanguageMode
import com.merry8989.localaikeyboard.lingua.PhoneticMatcher

/**
 * The typing-time brain: one SymSpell index over English + Hinglish words, plus
 * a phonetic pass so Romanised Hindi variants surface. Returns at most three
 * candidates for the suggestion bar.
 */
class SpellEngine(context: Context) {

    private val hinglish = HinglishLexicon(context)
    private val sym: SymSpell = SymSpell(maxEditDistance = 2, prefixLength = 7).also { s ->
        s.createDictionary(englishWords(context) + hinglish.entries)
    }

    /**
     * Ranked suggestions for the word currently being typed.
     * [mode] biases the mix between English and Hinglish.
     */
    fun suggest(word: String, mode: LanguageMode): List<String> {
        val w = word.lowercase()
        if (w.isBlank()) return emptyList()

        val results = LinkedHashMap<String, Double>()
        for (s in sym.lookup(w, maxEdit = 2, limit = 12)) {
            if (s.term == w) continue
            var score = s.count.toDouble() - s.distance * 500_000.0
            if (mode == LanguageMode.HINGLISH && hinglish.contains(s.term)) score += 2_000_000
            if (mode == LanguageMode.ENGLISH && hinglish.contains(s.term)) score -= 2_000_000
            results[s.term] = score
        }

        // Phonetic boost: a Hinglish word that sounds like what was typed.
        if (mode != LanguageMode.ENGLISH) {
            for ((term, freq) in hinglish.entries) {
                if (term == w) continue
                if (PhoneticMatcher.similar(term, w)) {
                    results[term] = (results[term] ?: 0.0) + freq + 5_000_000
                }
            }
        }

        return results.entries
            .sortedByDescending { it.value }
            .map { matchCase(word, it.key) }
            .distinct()
            .take(3)
    }

    /** Apply the lightweight grammar rules to a finished sentence/paragraph. */
    fun correctText(text: String): String = GrammarRules.apply(text)

    private fun matchCase(typed: String, candidate: String): String =
        if (typed.firstOrNull()?.isUpperCase() == true) {
            candidate.replaceFirstChar { it.uppercase() }
        } else candidate

    private fun englishWords(context: Context): Map<String, Int> {
        val words = runCatching {
            context.assets.open("en_words.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            }
        }.getOrDefault(emptyList())
        val total = words.size.coerceAtLeast(1)
        // rank-based frequency: earlier lines are more common
        return words.withIndex().associate { (i, w) -> w to (total - i) * 100 }
    }
}
