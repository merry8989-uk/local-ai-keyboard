package com.merry8989.localaikeyboard.spell

/**
 * A compact SymSpell: fast approximate lookup via symmetric delete variants.
 * Build once, then [lookup] is a hash probe plus a bounded edit-distance check.
 *
 * Reference: Garbe, "1000x faster spelling correction" (SymSpell).
 */
class SymSpell(
    private val maxEditDistance: Int = 2,
    private val prefixLength: Int = 7,
) {
    private val counts = HashMap<String, Long>()
    private val deletes = HashMap<String, MutableList<String>>()

    data class Suggestion(val term: String, val distance: Int, val count: Long)

    fun createDictionary(entries: Map<String, Int>) {
        for ((termRaw, count) in entries) {
            if (count <= 0) continue
            val term = termRaw.lowercase()
            counts[term] = count.toLong()
            val variants = HashSet<String>()
            edits(term.take(prefixLength), maxEditDistance, variants)
            for (v in variants) {
                deletes.getOrPut(v) { ArrayList() }.add(term)
            }
        }
    }

    private fun edits(term: String, distance: Int, out: MutableSet<String>) {
        out.add(term)
        if (distance <= 0) return
        for (i in term.indices) {
            val deleted = term.removeRange(i, i + 1).toString()
            edits(deleted, distance - 1, out)
        }
    }

    fun contains(word: String) = counts.containsKey(word.lowercase())

    fun lookup(input: String, maxEdit: Int = maxEditDistance, limit: Int = 10): List<Suggestion> {
        val word = input.lowercase()
        if (word.isBlank()) return emptyList()

        val variants = HashSet<String>()
        edits(word.take(prefixLength), maxEdit, variants)

        val best = HashMap<String, Suggestion>()
        for (v in variants) {
            val terms = deletes[v] ?: continue
            for (term in terms) {
                val d = levenshtein(word, term, maxEdit)
                if (d > maxEdit) continue
                val count = counts[term] ?: 0L
                val prev = best[term]
                if (prev == null || d < prev.distance || (d == prev.distance && count > prev.count)) {
                    best[term] = Suggestion(term, d, count)
                }
            }
        }
        return best.values
            .sortedWith(compareBy({ it.distance }, { -it.count }))
            .take(limit)
    }

    private fun levenshtein(a: String, b: String, max: Int): Int {
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
