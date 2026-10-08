package com.merry8989.localaikeyboard.ime

/** What a key does when tapped. */
enum class KeyCode {
    CHAR, SHIFT, BACKSPACE, ENTER, SPACE, SYMBOLS, LANG, AI, EMOJI, STICKER, SETTINGS, CLIPBOARD, MENU
}

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
 * Key arrangements, built from options so settings (number row, ". ?") can
 * shape the layout.
 */
object KeyboardLayout {

    private fun row(s: String, weight: Float = 1f): List<KeyDef> =
        s.map { KeyDef(it.toString(), output = it.toString(), weight = weight) }

    /** The optional number row shown above the letters. */
    val numberRow: List<KeyDef> = row("1234567890")

    private fun actionRow(left: String, dotOrQuestion: Boolean): List<KeyDef> {
        val punct = if (dotOrQuestion) {
            KeyDef("?", output = "?", weight = 0.9f)
        } else {
            KeyDef(".", output = ".", weight = 0.9f)
        }
        return listOf(
            KeyDef(left, code = KeyCode.SYMBOLS, weight = 1.25f, isSpecial = true),
            // Settings and Language now live in the ⋮ smart-bar menu, so the
            // action row carries a single ⋮ menu key instead of the old ⚙ + हि.
            KeyDef("⋮", code = KeyCode.MENU, weight = 0.85f, isSpecial = true),
            KeyDef("✦", code = KeyCode.AI, weight = 0.85f, isSpecial = true),
            KeyDef("☺", code = KeyCode.EMOJI, weight = 0.85f, isSpecial = true),
            KeyDef("space", output = " ", code = KeyCode.SPACE, weight = 3.25f),
            punct,
            KeyDef("↵", code = KeyCode.ENTER, weight = 1.25f, isSpecial = true),
        )
    }

    /** Letter layout, optionally with a number row. */
    fun qwerty(showNumberRow: Boolean, dotOrQuestion: Boolean): List<List<KeyDef>> {
        val rows = listOf(
            row("qwertyuiop"),
            row("asdfghjkl"),
            listOf(
                KeyDef("⇧", code = KeyCode.SHIFT, weight = 1.5f, isSpecial = true)
            ) + row("zxcvbnm") + listOf(
                KeyDef("⌫", code = KeyCode.BACKSPACE, weight = 1.5f, isSpecial = true)
            ),
            actionRow("?123", dotOrQuestion),
        )
        return if (showNumberRow) listOf(numberRow) + rows else rows
    }

    /** Symbols / numbers page, with an extra row of signs. */
    fun symbols(dotOrQuestion: Boolean): List<List<KeyDef>> = listOf(
        row("1234567890"),
        row("@#\$%&*-+="),
        listOf(
            KeyDef("⇧", code = KeyCode.SHIFT, weight = 1.5f, isSpecial = true)
        ) + row("()[]{}<>") + listOf(
            KeyDef("⌫", code = KeyCode.BACKSPACE, weight = 1.5f, isSpecial = true)
        ),
        row("\"'`~^|\\/;:"),
        row("₹€£¥¢©®™°"),
        actionRow("ABC", dotOrQuestion),
    )
}
