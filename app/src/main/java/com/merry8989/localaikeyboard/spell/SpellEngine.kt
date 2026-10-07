package com.merry8989.localaikeyboard.spell

import android.content.Context
import com.merry8989.localaikeyboard.ime.ThemePrefs
import com.merry8989.localaikeyboard.lingua.HinglishLexicon
import com.merry8989.localaikeyboard.lingua.LanguageMode
import com.merry8989.localaikeyboard.lingua.LanguagePacks
import com.merry8989.localaikeyboard.lingua.PhoneticMatcher

/**
 * The typing-time brain: one SymSpell index built from the enabled language
 * packs, a phonetic pass for Romanised Hindi, the user's learned words, and
 * next-word prediction from [LearnedStore].
 */
class SpellEngine(
    private val context: Context,
    private val userDict: UserDictionary,
    private val prefs: ThemePrefs,
    private val learned: LearnedStore,
) {

    private val hinglish = HinglishLexicon(context)
    private var sym: SymSpell = buildIndex()

    /** Rebuild the dictionary after the enabled languages change. */
    fun reload() {
        sym = buildIndex()
    }

    fun enabledLanguages(): Set<String> = prefs.enabledLanguages

    private fun buildIndex(): SymSpell {
        val entries = HashMap<String, Int>()
        val enabled = prefs.enabledLanguages
        for (pack in LanguagePacks.all) {
            if (pack.id !in enabled) continue
            val words = LanguagePacks.loadWords(context, pack)
            val total = words.size.coerceAtLeast(1)
            words.forEachIndexed { i, w ->
                entries[w] = (entries[w] ?: 0) + (total - i) * 100
            }
        }
        return SymSpell(maxEditDistance = 2, prefixLength = 7).also { it.createDictionary(entries) }
    }

    /** Ranked suggestions for the word currently being typed. */
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

        if (mode != LanguageMode.ENGLISH) {
            for ((term, freq) in hinglish.entries) {
                if (term == w) continue
                if (PhoneticMatcher.similar(term, w)) {
                    results[term] = (results[term] ?: 0.0) + freq + 5_000_000
                }
            }
        }

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

    /**
     * Next-word predictions given the word just before the cursor. Learns from
     * what you actually type.
     */
    fun predictNext(previousWord: String): List<String> {
        if (previousWord.isBlank()) return emptyList()
        return learned.nextWords(previousWord, limit = 3)
            .map { matchCase(previousWord, it) }
    }

    /** Remember a word the user typed. */
    fun learn(word: String) {
        val w = word.trim()
        if (w.length >= 3 && !sym.contains(w) && !hinglish.contains(w)) userDict.add(w)
    }

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
}
