package com.merry8989.localaikeyboard.ai

import com.merry8989.localaikeyboard.lingua.LanguageMode

/**
 * Prompt builders for each action. Kept short and imperative so a small (1–2B)
 * model stays on task. Every template asks for *only* the result text, because
 * the output is inserted straight into the editor.
 */
object PromptTemplates {

    fun build(action: AiAction, input: String, mode: LanguageMode, tone: String = "neutral"): String {
        val langHint = when (mode) {
            LanguageMode.HINGLISH -> "The text may be Hinglish (Romanised Hindi); keep that language and style."
            LanguageMode.ENGLISH -> "Write in English."
            LanguageMode.AUTO -> "Keep the same language the text is written in (English or Hinglish)."
        }
        return when (action) {
            AiAction.FIX -> """
                Correct the spelling and grammar of the text below.
                $langHint
                Keep the meaning unchanged. Return ONLY the corrected text.
                ---
                $input
            """.trimIndent()

            AiAction.REPHRASE -> """
                Rewrite the text below in a $tone tone.
                $langHint
                Keep the meaning. Return ONLY the rewritten text.
                ---
                $input
            """.trimIndent()

            AiAction.CONTINUE -> """
                Continue the text below naturally, in the same voice and language.
                Write 1–2 more sentences. Return ONLY the continuation.
                ---
                $input
            """.trimIndent()

            AiAction.EMAIL -> """
                Write a professional email. Put the subject on the first line as
                "Subject: ...", then a blank line, then the body.
                $langHint
                Base it on these notes:
                ---
                $input
            """.trimIndent()

            AiAction.ESSAY -> """
                Write a well-structured essay of 3–4 short paragraphs on the topic below.
                $langHint
                Start with an introduction, then the body, then a conclusion.
                Return ONLY the essay.
                ---
                $input
            """.trimIndent()

            AiAction.MESSAGE -> """
                Write a short, friendly chat message expressing the intent below.
                $langHint
                Keep it to 1–2 sentences. Return ONLY the message.
                ---
                $input
            """.trimIndent()
        }
    }

    /** Rough output budget per action, so essays get more room than a fix. */
    fun maxTokensFor(action: AiAction): Int = when (action) {
        AiAction.FIX, AiAction.MESSAGE -> 256
        AiAction.REPHRASE, AiAction.CONTINUE -> 320
        AiAction.EMAIL -> 512
        AiAction.ESSAY -> 768
    }
}
