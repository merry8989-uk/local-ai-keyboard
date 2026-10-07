package com.merry8989.localaikeyboard.ime

/**
 * Long-press alternates: hold a key for a moment and a strip of related
 * characters appears (accents, other forms, related signs) — the behaviour
 * people expect from a normal keyboard.
 *
 * The first entry of each list is the base character itself.
 */
object AlternateKeys {

    private val letters: Map<Char, String> = mapOf(
        'a' to "aáàâäãåā",
        'e' to "eéèêëēę",
        'i' to "iíìîïīį",
        'o' to "oóòôöõøō",
        'u' to "uúùûüū",
        'y' to "yýÿ",
        'n' to "nñń",
        'c' to "cçćč",
        's' to "sśšş",
        'z' to "zźżž",
        'd' to "dď",
        't' to "tťţ",
        'r' to "rř",
        'l' to "lł",
        'g' to "gğ",
        'p' to "pþ",
        'k' to "kķ",
        'b' to "bβ",
        'f' to "fƒ",
        'm' to "mµ",
    )

    /** Symbol keys get related signs too. */
    private val symbols: Map<Char, String> = mapOf(
        '-' to "-–—_",
        '.' to ".…•",
        '\'' to "'’‘`",
        '"' to "\"“”„",
        '?' to "?¿",
        '!' to "!¡",
        '/' to "/\\|",
        '(' to "([{<",
        ')' to ")]}>",
        ':' to ":;",
        ',' to ",،",
        '*' to "*×÷·",
        '+' to "+−±",
        '=' to "=≠≈≤≥",
        '<' to "<≤«",
        '>' to ">≥»",
        '1' to "1¹½⅓¼",
        '2' to "2²⅔",
        '3' to "3³¾",
        '$' to "\$€£¥₹¢",
        '%' to "%‰°",
        '^' to "^°±",
    )

    /** Characters that should offer alternates, base char included. */
    fun variantsFor(label: String): List<String> {
        if (label.isEmpty()) return emptyList()
        val lower = label.lowercase()
        val c = lower[0]
        val set = letters[c] ?: symbols[c] ?: return emptyList()
        val chars = set.map { it.toString() }
        // If the key was shifted/uppercase, upper-case the letter variants.
        return if (label[0].isUpperCase() && c.isLetter()) {
            chars.map { it.uppercase() }
        } else {
            chars
        }
    }
}
