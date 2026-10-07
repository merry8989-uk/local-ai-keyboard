package com.merry8989.localaikeyboard.ime

/**
 * Long-press popups: hold a key and a strip of related characters appears.
 * With [includeSymbols] the popup also carries related symbols, the way the
 * "accents include symbol popups" option works on other keyboards.
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

    /** Symbols added to a letter's popup when symbol popups are enabled. */
    private val letterSymbols: Map<Char, String> = mapOf(
        'a' to "@",
        'b' to "•",
        'c' to "©",
        'd' to "°",
        'e' to "€",
        'f' to "±",
        'g' to "&",
        'h' to "#",
        'i' to "¡",
        'j' to "—",
        'k' to "×",
        'l' to "£",
        'm' to "µ",
        'n' to "№",
        'o' to "°",
        'p' to "¶",
        'q' to "¿",
        'r' to "®",
        's' to "§\$",
        't' to "™",
        'u' to "_",
        'v' to "✓",
        'w' to "~",
        'x' to "»",
        'y' to "¥",
        'z' to "«",
    )

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

    /** Characters that should offer a popup, base char included. */
    fun variantsFor(label: String, includeSymbols: Boolean = true): List<String> {
        if (label.isEmpty()) return emptyList()
        val lower = label.lowercase()
        val c = lower[0]

        val base = letters[c] ?: symbols[c] ?: return emptyList()
        val builder = StringBuilder(base)

        if (includeSymbols) {
            letterSymbols[c]?.let { extra ->
                for (ch in extra) if (builder.indexOf(ch.toString()) < 0) builder.append(ch)
            }
        }

        val chars = builder.map { it.toString() }
        return if (label[0].isUpperCase() && c.isLetter()) {
            chars.map { it.uppercase() }
        } else {
            chars
        }
    }
}
