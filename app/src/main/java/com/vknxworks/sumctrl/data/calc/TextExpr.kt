package com.vknxworks.sumctrl.data.calc

import com.vknxworks.sumctrl.data.NumFmt
import java.math.BigDecimal

/**
 * Turns a piece of text selected in another app ("12,50 + 3,20", "3 x 4.5", "€ 199 − 10%") into calculator
 * keys, for "Calculate" in the text-selection menu. Pure Kotlin, no Android.
 *
 * Numbers: a comma or a point is the decimal separator; when both appear, the last one is ("1.234,5" and
 * "1,234.5" are both 1234.5), and the same separator more than once is thousands grouping ("1.000.000").
 * Spaces and currency signs are skipped. "*", "x" and "·" multiply, "/" and ":" divide, "-" and "–" subtract.
 */
object TextExpr {
    /** Longest selection that is looked at; anything longer isn't a sum someone selected. */
    const val MAX_LENGTH = 500

    private val SKIP = setOf(' ', ' ', ' ', ' ', '\t', '\n', '\r', '€', '$', '£', '¥', '=')

    /** The keys for [text], or null when it isn't a sum SumCtrl can read. */
    fun parse(text: String): List<String>? {
        if (text.length > MAX_LENGTH) return null
        val out = ArrayList<String>()
        var i = 0
        val s = text.trim().removeSuffix("=").trim()
        if (s.isEmpty()) return null
        while (i < s.length) {
            val c = s[i]
            when {
                c in SKIP -> i++
                c.isDigit() || ((c == ',' || c == '.') && s.getOrNull(i + 1)?.isDigit() == true) -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == ',' || s[i] == '.' ||
                            // A space between digit groups ("1 234,5") belongs to the number.
                            ((s[i] == ' ' || s[i] == ' ' || s[i] == ' ') && s.getOrNull(i + 1)?.isDigit() == true && groupedSpace(s, start, i)))
                    ) i++
                    val run = s.substring(start, i).filter { it.isDigit() || it == ',' || it == '.' }
                    val v = number(run) ?: return null
                    if (Engine.endsOperand(out.lastOrNull())) out += Engine.TIMES
                    out += v.toPlainString().map { it.toString() }
                }
                c == '+' -> { out += Engine.PLUS; i++ }
                c == '-' || c == '−' || c == '–' -> { out += Engine.MINUS; i++ }
                c == '*' || c == '×' || c == 'x' || c == 'X' || c == '·' -> { out += Engine.TIMES; i++ }
                c == '/' || c == '÷' || c == ':' -> { out += Engine.DIVIDE; i++ }
                c == '^' -> { out += Engine.POWER; i++ }
                c == '(' || c == '[' -> { out += Engine.OPEN; i++ }
                c == ')' || c == ']' -> { out += Engine.CLOSE; i++ }
                c == '%' -> { out += Engine.PERCENT; i++ }
                c == '√' -> { out += "√("; i++ }
                c == 'π' -> { out += Engine.PI; i++ }
                else -> return null
            }
        }
        if (out.isEmpty() || out.none { Engine.isDigit(it) || it == Engine.PI }) return null
        return out
    }

    /** A space inside a number only counts when what came before it in the run looks like a digit group. */
    private fun groupedSpace(s: String, start: Int, at: Int): Boolean {
        val before = s.substring(start, at)
        if (before.any { it == ',' || it == '.' }) return false
        val lastGroup = before.substringAfterLast(' ').substringAfterLast(' ').substringAfterLast(' ')
        val next = s.substring(at + 1).takeWhile { it.isDigit() }
        return lastGroup.length in 1..3 && next.length == 3
    }

    private fun number(run: String): BigDecimal? {
        val commas = run.count { it == ',' }
        val points = run.count { it == '.' }
        // "1.000.000" or "1,000,000": one separator used more than once is grouping.
        if ((commas > 1 && points == 0) || (points > 1 && commas == 0)) {
            return run.replace(",", "").replace(".", "").toBigDecimalOrNull()
        }
        return NumFmt.parse(run)
    }

    /** The decimal separator the selection itself uses (for writing the answer back), or null when it shows none. */
    fun decimalOf(text: String): Char? {
        // Look at each number on its own: "12,50 + 3,20" uses a comma, "1.000.000" has no decimal separator.
        Regex("[0-9][0-9.,]*").findAll(text).forEach { m ->
            val run = m.value.trimEnd(',', '.')
            val last = maxOf(run.lastIndexOf(','), run.lastIndexOf('.'))
            if (last < 0) return@forEach
            val c = run[last]
            val other = if (c == ',') '.' else ','
            if (run.count { it == c } == 1 || run.contains(other)) return c
        }
        return null
    }

    /** The answer as plain text for putting back in place of the selection: no grouping, [decimal] as separator. */
    fun answer(v: BigDecimal, decimal: Char): String =
        Engine.rounded(v).toPlainString().replace('.', decimal)
}
