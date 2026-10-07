package com.merry8989.localaikeyboard.lingua

/**
 * Collapses Romanised Hindi spelling variants to a rough phonetic key so that
 * `kese`, `kaise`, `kaisay` land near each other. This is deliberately
 * heuristic — it is a starter for the "did you mean" layer, not a linguistically
 * exact transliteration scheme (that arrives with the v2 Devanagari work).
 */
object PhoneticMatcher {

    // Ordered digraph/trigraph rewrites. Longest first so "khh" style junk folds too.
    private val rules = listOf(
        "sch" to "s", "tch" to "c",
        "aa" to "a", "ee" to "i", "ii" to "i", "oo" to "u", "uu" to "u",
        "ai" to "a", "au" to "o", "ea" to "i", "ie" to "i",
        "ph" to "f", "kh" to "k", "gh" to "g", "ch" to "c", "sh" to "s",
        "th" to "t", "dh" to "d", "bh" to "b", "jh" to "j", "zh" to "j",
        "ck" to "k", "qu" to "k", "wh" to "w",
        "w" to "v",
    )

    /** A canonical key for [word]. */
    fun key(word: String): String {
        var w = word.lowercase().filter { it.isLetter() }
        for ((from, to) in rules) w = w.replace(from, to)
        // collapse runs of the same letter ("aa" already handled, this catches "nn")
        w = w.replace(Regex("(.)\\1+"), "$1")
        // drop a trailing silent h/v common in romanisations
        w = w.removeSuffix("h")
        return w
    }

    /**
     * True when two romanised words are plausibly the same Hindi word.
     * Used to boost candidates whose phonetic key matches the typed word.
     */
    fun similar(a: String, b: String): Boolean = key(a) == key(b)
}
