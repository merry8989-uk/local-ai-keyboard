package com.merry8989.localaikeyboard.ime

/** What a key does when tapped. */
enum class KeyCode { CHAR, SHIFT, BACKSPACE, ENTER, SPACE, SYMBOLS, LANG, AI, EMOJI, STICKER, SETTINGS }

/**
 * Logical description of one key. [output] is the text committed on tap;
 * action keys (shift, backspace, …) carry a [code] instead.
 */
data class KeyDef(
    val label: String,
    val output: String? = null,
    val code: KeyCode = KeyCode.CHAR,
    val weight: Float = 1f,
    val isSpecial: Boolean = false,
)

/**
 * Static key arrangements. Plain data, so layouts are easy to tweak.
 */
object KeyboardLayout {

    private fun row(s: String, weight: Float = 1f): List<KeyDef> =
        s.map { KeyDef(it.toString(), output = it.toString(), weight = weight) }

    private fun actionRow(left: String): List<KeyDef> = listOf(
        KeyDef(left, code = KeyCode.SYMBOLS, weight = 1.3f, isSpecial = true),
        KeyDef("हि", code = KeyCode.LANG, weight = 0.9f, isSpecial = true),
        KeyDef("✦", code = KeyCode.AI, weight = 0.9f, isSpecial = true),
        KeyDef("☺", code = KeyCode.EMOJI, weight = 0.9f, isSpecial = true),
        KeyDef("⚙", code = KeyCode.SETTINGS, weight = 0.9f, isSpecial = true),
        KeyDef("space", output = " ", code = KeyCode.SPACE, weight = 2.8f),
        KeyDef(".", output = "."),
        KeyDef("↵", code = KeyCode.ENTER, weight = 1.3f, isSpecial = true),
    )

    /** Lowercase QWERTY with the action rows. */
    val qwerty: List<List<KeyDef>> = listOf(
        row("qwertyuiop"),
        row("asdfghjkl"),
        listOf(
            KeyDef("⇧", code = KeyCode.SHIFT, weight = 1.5f, isSpecial = true)
        ) + row("zxcvbnm") + listOf(
            KeyDef("⌫", code = KeyCode.BACKSPACE, weight = 1.5f, isSpecial = true)
        ),
        actionRow("?123"),
    )

    /** Symbols / numbers page, with an extra row of signs. */
    val symbols: List<List<KeyDef>> = listOf(
        row("1234567890"),
        row("@#\$%&*-+="),
        listOf(
            KeyDef("⇧", code = KeyCode.SHIFT, weight = 1.5f, isSpecial = true)
        ) + row("()[]{}<>") + listOf(
            KeyDef("⌫", code = KeyCode.BACKSPACE, weight = 1.5f, isSpecial = true)
        ),
        row("\"'`~^|\\/;:"),
        row("₹€£¥¢©®™°"),
        actionRow("ABC"),
    )
}
