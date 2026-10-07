package com.merry8989.localaikeyboard.ime

/** What a key does when tapped. */
enum class KeyCode { CHAR, SHIFT, BACKSPACE, ENTER, SPACE, SYMBOLS, LANG, AI }

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
 * Static key arrangements. Deliberately plain data so layouts are easy to
 * tweak or generate (e.g. for future languages) without touching the view.
 */
object KeyboardLayout {

    private fun row(s: String, weight: Float = 1f): List<KeyDef> =
        s.map { KeyDef(it.toString(), output = it.toString(), weight = weight) }

    /** Lowercase QWERTY with the action rows. */
    val qwerty: List<List<KeyDef>> = listOf(
        row("qwertyuiop"),
        row("asdfghjkl"),
        listOf(
            KeyDef("⇧", code = KeyCode.SHIFT, weight = 1.5f, isSpecial = true)
        ) + row("zxcvbnm") + listOf(
            KeyDef("⌫", code = KeyCode.BACKSPACE, weight = 1.5f, isSpecial = true)
        ),
        listOf(
            KeyDef("?123", code = KeyCode.SYMBOLS, weight = 1.5f, isSpecial = true),
            KeyDef("हि", code = KeyCode.LANG, weight = 1.1f, isSpecial = true),
            KeyDef("✦", code = KeyCode.AI, weight = 1.1f, isSpecial = true),
            KeyDef("space", output = " ", code = KeyCode.SPACE, weight = 4f),
            KeyDef(".", output = "."),
            KeyDef("↵", code = KeyCode.ENTER, weight = 1.5f, isSpecial = true),
        ),
    )

    /** Symbols / numbers page. */
    val symbols: List<List<KeyDef>> = listOf(
        row("1234567890"),
        row("@#\$%&*-+()"),
        listOf(
            KeyDef("⇧", code = KeyCode.SHIFT, weight = 1.5f, isSpecial = true)
        ) + row("\"':;/?!,") + listOf(
            KeyDef("⌫", code = KeyCode.BACKSPACE, weight = 1.5f, isSpecial = true)
        ),
        listOf(
            KeyDef("ABC", code = KeyCode.SYMBOLS, weight = 1.5f, isSpecial = true),
            KeyDef("हि", code = KeyCode.LANG, weight = 1.1f, isSpecial = true),
            KeyDef("✦", code = KeyCode.AI, weight = 1.1f, isSpecial = true),
            KeyDef("space", output = " ", code = KeyCode.SPACE, weight = 4f),
            KeyDef(".", output = "."),
            KeyDef("↵", code = KeyCode.ENTER, weight = 1.5f, isSpecial = true),
        ),
    )
}
