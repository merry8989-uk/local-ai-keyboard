package com.merry8989.localaikeyboard.ime

import kotlin.math.pow

/**
 * A tiny, safe arithmetic evaluator — no eval(), just a small recursive-descent
 * parser. Supports + - * / % ^ and parentheses, with unary + and -.
 */
object Calc {

    fun eval(input: String): Double? {
        val s = input.replace(" ", "")
            .replace("\u00D7", "*")
            .replace("\u00F7", "/")
        if (s.isBlank()) return null
        return try {
            val p = Parser(s)
            val v = p.parseExpression()
            if (p.pos != s.length) null
            else if (v.isNaN() || v.isInfinite()) null
            else v
        } catch (t: Throwable) {
            null
        }
    }

    /** Compact display: integers without a trailing ".0", else up to 6 dp. */
    fun format(v: Double): String =
        if (v == v.toLong().toDouble()) {
            v.toLong().toString()
        } else {
            "%.6f".format(v).trimEnd('0').trimEnd('.')
        }

    private class Parser(private val s: String) {
        var pos = 0

        fun parseExpression(): Double {
            var v = parseTerm()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos]; pos++
                val r = parseTerm()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        fun parseTerm(): Double {
            var v = parseFactor()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/' || s[pos] == '%')) {
                val op = s[pos]; pos++
                val r = parseFactor()
                v = when (op) {
                    '*' -> v * r
                    '/' -> v / r
                    else -> v % r
                }
            }
            return v
        }

        fun parseFactor(): Double {
            if (pos < s.length && s[pos] == '+') { pos++; return parseFactor() }
            if (pos < s.length && s[pos] == '-') { pos++; return -parseFactor() }
            return parsePower()
        }

        fun parsePower(): Double {
            val base = parsePrimary()
            if (pos < s.length && s[pos] == '^') {
                pos++
                return base.pow(parseFactor())
            }
            return base
        }

        fun parsePrimary(): Double {
            if (pos < s.length && s[pos] == '(') {
                pos++
                val v = parseExpression()
                if (pos < s.length && s[pos] == ')') pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("bad expression")
            return s.substring(start, pos).toDouble()
        }
    }
}
