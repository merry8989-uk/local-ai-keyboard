package com.merry8989.localaikeyboard.spell

import android.content.Context
import com.merry8989.localaikeyboard.lingua.HinglishLexicon
import com.merry8989.localaikeyboard.lingua.LanguageMode
import com.merry8989.localaikeyboard.lingua.PhoneticMatcher

/**
 * The typing-time brain: one SymSpell index over English + Hinglish words, plus
 * a phonetic pass for Romanised Hindi, plus the user's own learned words.
 * Returns at most three candidates for the suggestion bar.
 */
class SpellEngine(context: Context, private val userDict: UserDictionary) {

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

        // The user's own learned words get a strong boost.
        for ((term, freq) in userDict.all()) {
            if (term == w) continue
            if (editDistance(w, term, 2) <= 2) {
                results[term] = (results[term] ?: 0.0) + 3_000_000 + freq
            }
        }

        return results.entries
            .sortedByDescending { it.value }
            .map { matchCase(word, it.key) }
            .distinct()
            .take(3)
    }

    /** Remember a word the user typed (goes to the on-device dictionary). */
    fun learn(word: String) {
        val w = word.trim()
        if (w.length >= 3 && !sym.contains(w) && !hinglish.contains(w)) userDict.add(w)
    }

    /** Apply the lightweight grammar rules to a finished sentence/paragraph. */
    fun correctText(text: String): String = GrammarRules.apply(text)

    private fun matchCase(typed: String, candidate: String): String =
        if (typed.firstOrNull()?.isUpperCase() == true) {
            candidate.replaceFirstChar { it.uppercase() }
        } else candidate

    private fun editDistance(a: String, b: String, max: Int): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > max) return max + 1
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
                if (cur[j] < rowMin) rowMin = cur[j]
            }
            if (rowMin > max) return max + 1
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[b.length]
    }

    private fun englishWords(context: Context): Map<String, Int> {
        val words = runCatching {
            context.assets.open("en_words.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            }
        }.getOrDefault(emptyList())
        val total = words.size.coerceAtLeast(1)
        return words.withIndex().associate { (i, w) -> w to (total - i) * 100 }
    }
}
