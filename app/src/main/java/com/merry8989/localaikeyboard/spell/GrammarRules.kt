package com.merry8989.localaikeyboard.spell

/**
 * Fast, deterministic, offline grammar/typo fixes for the most common slips.
 * This is the cheap layer that runs on every commit; deeper rewrites are the
 * LLM's job (the "Fix" action).
 */
object GrammarRules {

    private val repeatedWord = Regex("\\b(\\w+)\\s+\\1\\b", RegexOption.IGNORE_CASE)
    private val spaceBeforePunct = Regex("\\s+([,.;:!?])")
    private val multiSpace = Regex("[ \\t]{2,}")
    private val loneI = Regex("\\bi\\b")

    fun apply(input: String): String {
        if (input.isBlank()) return input
        var t = input
        t = multiSpace.replace(t, " ")
        t = spaceBeforePunct.replace(t, "$1")
        t = repeatedWord.replace(t) { m -> m.groupValues[1] }          // "the the" -> "the"
        t = loneI.replace(t, "I")                                       // "i think" -> "I think"
        t = fixArticles(t)
        t = capitaliseSentences(t)
        return t
    }

    private fun fixArticles(t: String): String =
        Regex("\\ba\\s+([aeiouAEIOU]\\w*)").replace(t) { "an ${it.groupValues[1]}" }
            .let { Regex("\\ban\\s+([^aeiouAEIOU\\s]\\w*)").replace(it) { m -> "a ${m.groupValues[1]}" } }

    private fun capitaliseSentences(t: String): String {
        val sb = StringBuilder(t)
        var capitaliseNext = true
        for (i in sb.indices) {
            val c = sb[i]
            if (capitaliseNext && c.isLetter()) {
                sb[i] = c.uppercaseChar()
                capitaliseNext = false
            }
            if (c == '.' || c == '!' || c == '?') capitaliseNext = true
        }
        return sb.toString()
    }
}
