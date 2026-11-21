package com.vknxworks.sumctrl.data.calc

import com.vknxworks.sumctrl.data.NumFmt

/**
 * A line of keys as text, with where each key starts in it. [starts] has one entry per key plus one for the
 * end: key i is drawn from `starts[i]` up to `starts[i + 1]`, so a tap at a character can be turned back into
 * a cursor position in the keys ([positionAt]) and a cursor position into a character ([offsetOf]).
 */
class RenderedLine(val text: String, val starts: IntArray) {

    /** The character offset where the cursor before key [position] is drawn. */
    fun offsetOf(position: Int): Int = starts[position.coerceIn(0, starts.size - 1)]

    /** The cursor position (0..number of keys) closest to the character offset [offset]. */
    fun positionAt(offset: Int): Int {
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for (i in starts.indices) {
            val d = kotlin.math.abs(starts[i] - offset)
            // On a tie the later position wins, so a tap on the space after an operator lands after it.
            if (d <= bestDistance) {
                best = i
                bestDistance = d
            }
        }
        return best
    }
}

object Render {

    /** The typed keys as text: numbers grouped, operators spaced, inverse functions with ⁻¹, values by name. */
    fun tokens(tokens: List<String>, fmt: NumFmt): String = line(tokens, fmt).text

    /** [tokens] as text, with where each key starts (see [RenderedLine]). */
    fun line(tokens: List<String>, fmt: NumFmt): RenderedLine {
        val sb = StringBuilder()
        val starts = IntArray(tokens.size + 1)
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if (Engine.isNumberPart(t)) {
                val runStart = i
                val run = StringBuilder()
                while (i < tokens.size && Engine.isNumberPart(tokens[i])) run.append(tokens[i++])
                val grouped = fmt.grouped(run.toString())
                // Walk the grouped text: every character but a grouping space is one key of the run.
                var key = runStart
                for (ch in grouped) {
                    if (fmt.group != null && ch == fmt.group) {
                        sb.append(ch)
                        continue
                    }
                    if (key < i) starts[key++] = sb.length
                    sb.append(ch)
                }
                continue
            }
            starts[i] = sb.length
            val prev = tokens.getOrNull(i - 1)
            val unary = t == Engine.MINUS && (prev == null || prev in Engine.BINARY || prev == Engine.OPEN || Engine.isFunction(prev))
            sb.append(
                when {
                    t == Engine.POWER -> "^"
                    t in Engine.BINARY && !unary -> " $t "
                    t == "asin(" -> "sin⁻¹("
                    t == "acos(" -> "cos⁻¹("
                    t == "atan(" -> "tan⁻¹("
                    Engine.isVariable(t) && t != Engine.MEMORY -> Engine.variableKey(t)
                    else -> t
                },
            )
            i++
        }
        starts[tokens.size] = sb.length
        return RenderedLine(sb.toString(), starts)
    }
}
