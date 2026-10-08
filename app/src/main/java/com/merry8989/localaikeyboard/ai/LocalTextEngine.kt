package com.merry8989.localaikeyboard.ai

import com.merry8989.localaikeyboard.lingua.LanguageMode
import com.merry8989.localaikeyboard.spell.GrammarRules
import com.merry8989.localaikeyboard.spell.LearnedStore
import com.merry8989.localaikeyboard.spell.SpellEngine

/**
 * A small, fully on-device text helper.
 *
 * IMPORTANT: this is **not** a neural language model. It applies spelling and
 * grammar rules and fills in templates. That is deliberate — it needs no model
 * file, no download and no network, which is why the app can be lightweight and
 * completely offline. It fixes and tidies text; it does not "write" like an LLM.
 */
class LocalTextEngine(
    private val spell: SpellEngine,
    private val learned: LearnedStore,
) {

    fun run(action: AiAction, input: String, mode: LanguageMode): String = when (action) {
        AiAction.FIX -> fix(input)
        AiAction.REPHRASE -> rephrase(input)
        AiAction.CONTINUE -> continueText(input)
        AiAction.EMAIL -> email(input)
        AiAction.ESSAY -> essay(input)
        AiAction.MESSAGE -> message(input)
    }

    // ---- fix -----------------------------------------------------------------

    private fun fix(input: String): String {
        val tokens = input.split(Regex("(\\s+)"))
        val corrected = tokens.joinToString("") { token ->
            if (token.isBlank()) token else correctWord(token)
        }
        return GrammarRules.apply(corrected)
    }

    private fun correctWord(token: String): String {
        val lead = token.takeWhile { !it.isLetter() }
        val trail = token.reversed().takeWhile { !it.isLetter() }.reversed()
        val end = token.length - trail.length
        if (end <= lead.length) return token
        val core = token.substring(lead.length, end)
        if (core.length < 3 || spell.isKnown(core)) return token
        val suggestion = spell.suggest(core, LanguageMode.AUTO).firstOrNull() ?: return token
        return lead + suggestion + trail
    }

    // ---- rephrase ------------------------------------------------------------

    private val informalToFormal = listOf(
        "can't" to "cannot",
        "won't" to "will not",
        "don't" to "do not",
        "doesn't" to "does not",
        "didn't" to "did not",
        "isn't" to "is not",
        "aren't" to "are not",
        "i'm" to "I am",
        "it's" to "it is",
        "that's" to "that is",
        "gonna" to "going to",
        "wanna" to "want to",
        "kinda" to "kind of",
        "btw" to "by the way",
        "asap" to "as soon as possible",
        "pls" to "please",
        "plz" to "please",
        "thx" to "thank you",
        "u" to "you",
    )

    private fun rephrase(input: String): String {
        var t = GrammarRules.apply(input.trim())
        for ((from, to) in informalToFormal) {
            t = t.replace(Regex("\\b" + Regex.escape(from) + "\\b", RegexOption.IGNORE_CASE), to)
        }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    // ---- continue ------------------------------------------------------------

    private fun continueText(input: String): String {
        val last = input.trim().split(Regex("\\s+")).lastOrNull()
            ?.trim { !it.isLetter() }
            ?.lowercase()
            ?: return ""
        val next = learned.nextWords(last, limit = 3)
        if (next.isEmpty()) return ""
        return next.joinToString(" ").replaceFirstChar { it.uppercase() } + "."
    }

    // ---- writing scaffolds ---------------------------------------------------

    private fun email(input: String): String {
        val notes = input.trim()
        val subject = notes.split(Regex("[\\n.]")).firstOrNull()?.trim().orEmpty().take(60)
        return buildString {
            append("Subject: ").append(if (subject.isEmpty()) "Quick note" else subject).append("\n\n")
            append("Dear Sir/Madam,\n\n")
            append(notes).append("\n\n")
            append("Please let me know if you need anything further.\n\n")
            append("Best regards,\n")
        }
    }

    private fun essay(input: String): String {
        val topic = input.trim().ifEmpty { "this topic" }
        return buildString {
            append("Introduction\n")
            append(topic).append(" is a subject worth understanding carefully.\n\n")
            append("Body\n")
            append("There are several aspects to consider. ")
            append("First, it matters because it touches everyday life. ")
            append("Second, opinions differ, and both sides have merit.\n\n")
            append("Conclusion\n")
            append("In short, ").append(topic).append(" deserves careful thought.\n")
        }
    }

    private fun message(input: String): String {
        val t = input.trim().replace(Regex("\\s+"), " ")
        return if (t.isEmpty()) "" else t.replaceFirstChar { it.uppercase() }
    }
}
