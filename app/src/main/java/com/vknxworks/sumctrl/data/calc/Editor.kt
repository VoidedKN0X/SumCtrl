package com.vknxworks.sumctrl.data.calc

import com.vknxworks.sumctrl.data.calc.Engine.CLOSE
import com.vknxworks.sumctrl.data.calc.Engine.MINUS
import com.vknxworks.sumctrl.data.calc.Engine.OPEN
import com.vknxworks.sumctrl.data.calc.Engine.PLUS
import com.vknxworks.sumctrl.data.calc.Engine.POINT
import java.math.BigDecimal

/**
 * What's on the calculator: the typed keys, whether the line is a finished result (the next digit starts a new
 * sum), the last answer and the last error. Every change returns a new state; nothing here touches the UI.
 */
data class CalcState(
    val tokens: List<String> = emptyList(),
    /** The line holds the result of "=": typing a digit starts over, an operator continues from it. */
    val evaluated: Boolean = false,
    /** The last result, for "Ans" and for continuing. */
    val ans: BigDecimal = BigDecimal.ZERO,
    /** Set when "=" failed; cleared by the next key. */
    val error: CalcError? = null,
    /** The sum the shown result came from, shown small above it after "=". */
    val previous: List<String> = emptyList(),
) {
    private val last: String? get() = tokens.lastOrNull()

    private val openBrackets: Int
        get() = tokens.count { it == OPEN || Engine.isFunction(it) } - tokens.count { it == CLOSE }

    /** A digit 0–9. */
    fun digit(d: String): CalcState {
        val base = if (evaluated) fresh() else cleared()
        // After ")", a constant or "!", a digit means multiply: (2+3)4 → (2+3)×4.
        val t = if (Engine.isConstant(base.last ?: "") || base.last == CLOSE || base.last == Engine.FACTORIAL || base.last == Engine.PERCENT) {
            base.tokens + Engine.TIMES
        } else {
            base.tokens
        }
        // No leading zeros: "0" then "5" is "5", but "0." stays.
        val run = numberRun(t)
        if (run == listOf("0")) return base.copy(tokens = t.dropLast(1) + d)
        return base.copy(tokens = t + d)
    }

    /** The decimal separator. */
    fun point(): CalcState {
        val base = if (evaluated) fresh() else cleared()
        val run = numberRun(base.tokens)
        if (POINT in run) return base
        val t = if (run.isEmpty()) {
            val pre = if (Engine.endsOperand(base.last)) base.tokens + Engine.TIMES else base.tokens
            pre + "0" + POINT
        } else {
            base.tokens + POINT
        }
        return base.copy(tokens = t)
    }

    /** + − × ÷ ^. After a result, the sum continues from it. */
    fun operator(op: String): CalcState {
        val base = if (evaluated) continued() else cleared()
        val l = base.last
        return when {
            l == null -> if (op == MINUS) base.copy(tokens = listOf(MINUS)) else base
            l == OPEN || Engine.isFunction(l) -> if (op == MINUS) base.copy(tokens = base.tokens + MINUS) else base
            l in Engine.BINARY -> {
                // "5 × −" makes a negative number; any other operator replaces the last one.
                if (op == MINUS && l != MINUS && l != PLUS) {
                    base.copy(tokens = base.tokens + MINUS)
                } else {
                    // Replacing also drops a "−" typed for a negative after × or ÷.
                    var t = base.tokens.dropLast(1)
                    if (t.lastOrNull() in Engine.BINARY) t = t.dropLast(1)
                    if (t.isEmpty()) base.copy(tokens = if (op == MINUS) listOf(MINUS) else emptyList()) else base.copy(tokens = t + op)
                }
            }
            l == POINT -> base.copy(tokens = base.tokens + "0" + op)
            else -> base.copy(tokens = base.tokens + op)
        }
    }

    /** π, e or Ans. */
    fun constant(c: String): CalcState {
        val base = if (evaluated) fresh() else cleared()
        val t = if (Engine.endsOperand(base.last)) base.tokens + Engine.TIMES else base.tokens
        return base.copy(tokens = t + c)
    }

    /** sin(, ln(, √( ... After a result the function takes it: "= 9" then √ gives √(9. */
    fun function(f: String): CalcState {
        if (evaluated) {
            return continued().let { s -> s.copy(tokens = listOf(f) + s.tokens + CLOSE) }
        }
        val base = cleared()
        val t = if (Engine.endsOperand(base.last)) base.tokens + Engine.TIMES else base.tokens
        return base.copy(tokens = t + f)
    }

    /** Opens a bracket, or closes one when that makes sense: the single "( )" key. */
    fun bracket(): CalcState {
        val base = if (evaluated) continued() else cleared()
        val close = base.openBrackets > 0 && Engine.endsOperand(base.last)
        return if (close) base.copy(tokens = base.tokens + CLOSE) else base.open()
    }

    fun open(): CalcState {
        val base = if (evaluated) fresh() else cleared()
        val t = if (Engine.endsOperand(base.last)) base.tokens + Engine.TIMES else base.tokens
        return base.copy(tokens = t + OPEN)
    }

    fun close(): CalcState {
        val base = if (evaluated) continued() else cleared()
        if (base.openBrackets <= 0 || !Engine.endsOperand(base.last)) return base
        return base.copy(tokens = base.tokens + CLOSE)
    }

    /** % or ! after a number. */
    fun postfix(p: String): CalcState {
        val base = if (evaluated) continued() else cleared()
        if (!Engine.endsOperand(base.last) || base.last == POINT) return base
        return base.copy(tokens = base.tokens + p)
    }

    /** ±: flips the sign of the number being typed (or of the result). */
    fun negate(): CalcState {
        val base = if (evaluated) continued() else cleared()
        val t = base.tokens
        if (t.isEmpty()) return base.copy(tokens = listOf(MINUS))
        // The operand at the end: a number, a constant, or a bracket group.
        var start = t.size
        when {
            t.last() == CLOSE -> {
                var depth = 0
                for (i in t.indices.reversed()) {
                    if (t[i] == CLOSE) depth++
                    if (t[i] == OPEN || Engine.isFunction(t[i])) depth--
                    if (depth == 0) { start = i; break }
                }
            }
            else -> {
                while (start > 0 && (Engine.isNumberPart(t[start - 1]) || Engine.isPostfix(t[start - 1]))) start--
                if (start == t.size && Engine.isConstant(t.last())) start--
            }
        }
        if (start == t.size) {
            // Nothing to flip yet (the line ends in an operator): start a negative number.
            return if (t.last() in Engine.BINARY && t.last() != MINUS) base.copy(tokens = t + MINUS) else base
        }
        val before = t.getOrNull(start - 1)
        val beforeThat = t.getOrNull(start - 2)
        val unaryMinus = before == MINUS && (beforeThat == null || beforeThat in Engine.BINARY || beforeThat == OPEN || Engine.isFunction(beforeThat))
        val newTokens = when {
            unaryMinus -> t.subList(0, start - 1) + t.subList(start, t.size)
            before == PLUS -> t.subList(0, start - 1) + MINUS + t.subList(start, t.size)
            before == MINUS -> t.subList(0, start - 1) + PLUS + t.subList(start, t.size)
            else -> t.subList(0, start) + MINUS + t.subList(start, t.size)
        }
        return base.copy(tokens = newTokens)
    }

    /** Removes the last key; after a result it edits the result. */
    fun backspace(): CalcState = copy(tokens = tokens.dropLast(1), evaluated = false, error = null, previous = emptyList())

    /** AC: clears the line (the last answer stays for "Ans"). */
    fun clear(): CalcState = CalcState(ans = ans)

    /** "=": the result, or an error. Returns the new state and the result when it worked. */
    fun equals(degrees: Boolean): Pair<CalcState, BigDecimal?> {
        if (tokens.isEmpty() || evaluated) return this to null
        return try {
            val v = Engine.evaluate(tokens, degrees, ans)
            CalcState(tokens = Engine.tokensOf(v), evaluated = true, ans = v, previous = tokens) to v
        } catch (e: CalcException) {
            copy(error = e.error) to null
        } catch (e: ArithmeticException) {
            copy(error = CalcError.TOO_BIG) to null
        }
    }

    /** The live result under the line while typing, or null when the sum isn't complete or has no answer. */
    fun preview(degrees: Boolean): BigDecimal? {
        if (tokens.isEmpty() || evaluated) return null
        // A lone number is its own result; no preview needed.
        if (tokens.all { Engine.isNumberPart(it) || it == MINUS }) return null
        if (tokens.last() in Engine.BINARY) return null
        return try {
            Engine.evaluate(tokens, degrees, ans)
        } catch (e: CalcException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** Puts a number in (from history or the clipboard): replaces a result, or is added to the sum. */
    fun insert(v: BigDecimal): CalcState {
        val base = if (evaluated) fresh() else cleared()
        val number = Engine.tokensOf(v)
        val t = when {
            Engine.endsOperand(base.last) -> base.tokens + Engine.TIMES
            else -> base.tokens
        }
        // A negative number after an operator goes in brackets: 5 − (−3).
        val add = if (number.firstOrNull() == MINUS && t.isNotEmpty()) listOf(OPEN) + number + CLOSE else number
        return base.copy(tokens = t + add, ans = if (number == listOf(Engine.ANS)) v else base.ans)
    }

    /** A new, empty line, keeping the last answer. */
    private fun fresh() = CalcState(ans = ans)

    /** The same line with any error cleared. */
    private fun cleared() = if (error != null || previous.isNotEmpty()) copy(error = null, previous = emptyList()) else this

    /** Continuing from a result: the result's keys, ready for an operator. */
    private fun continued() = CalcState(tokens = tokens, ans = ans)

    private fun numberRun(t: List<String>): List<String> {
        var i = t.size
        while (i > 0 && Engine.isNumberPart(t[i - 1])) i--
        return t.subList(i, t.size)
    }
}
