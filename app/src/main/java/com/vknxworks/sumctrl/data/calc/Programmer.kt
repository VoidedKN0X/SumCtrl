package com.vknxworks.sumctrl.data.calc

/** The number base the programmer keypad types in. */
enum class Radix(val radix: Int) { HEX(16), DEC(10), OCT(8), BIN(2) }

enum class ProgError { SYNTAX, DIV_ZERO, SHIFT }

class ProgException(val error: ProgError) : Exception(error.name)

/**
 * Programmer mode: whole numbers in 64 bits (two's complement, like a `long`), typed in hexadecimal,
 * decimal, octal or binary. Arithmetic wraps around at 64 bits, as a programmer's calculator does; division
 * rounds towards zero. Everything is exact integer maths.
 *
 * A line is a list of tokens: a number is one token, "#" followed by its digits in the current base (always
 * unsigned: FFFFFFFFFFFFFFFF in hex is −1), and every operator or bracket is its own token.
 */
object Programmer {
    const val PLUS = "+"
    const val MINUS = "−"
    const val TIMES = "×"
    const val DIVIDE = "÷"
    const val MOD = "mod"
    const val AND = "AND"
    const val OR = "OR"
    const val XOR = "XOR"
    const val NOT = "NOT"
    const val SHL = "<<"
    const val SHR = ">>"
    const val OPEN = "("
    const val CLOSE = ")"

    val BINARY = setOf(PLUS, MINUS, TIMES, DIVIDE, MOD, AND, OR, XOR, SHL, SHR)

    fun isNumber(t: String?) = t != null && t.length > 1 && t[0] == '#'

    fun digitsOf(t: String) = t.substring(1)

    /** The value of a number token typed in [radix]. */
    fun valueOf(t: String, radix: Radix): Long = java.lang.Long.parseUnsignedLong(digitsOf(t), radix.radix)

    /** A number token for [v] in [radix] (unsigned digits). */
    fun tokenOf(v: Long, radix: Radix): String = "#" + java.lang.Long.toUnsignedString(v, radix.radix).uppercase()

    /** Whether [digit] (0–9, A–F) can be typed in [radix]. */
    fun digitAllowed(digit: Char, radix: Radix): Boolean = Character.digit(digit, radix.radix) >= 0

    /** [v] as it's shown for [radix]: decimal signed, the others as the 64-bit pattern, grouped by 4 for hex and binary. */
    fun format(v: Long, radix: Radix): String = when (radix) {
        Radix.DEC -> v.toString()
        Radix.HEX -> groupFromRight(java.lang.Long.toHexString(v).uppercase(), 4)
        Radix.OCT -> groupFromRight(java.lang.Long.toOctalString(v), 3)
        Radix.BIN -> groupFromRight(java.lang.Long.toBinaryString(v), 4)
    }

    private fun groupFromRight(s: String, size: Int): String {
        val sb = StringBuilder()
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % size == 0) sb.append(' ')
            sb.append(c)
        }
        return sb.toString()
    }

    /** Works out [tokens] typed in [radix]. Missing closing brackets at the end are added. */
    fun evaluate(tokens: List<String>, radix: Radix): Long {
        if (tokens.isEmpty()) throw ProgException(ProgError.SYNTAX)
        val p = Parser(tokens, radix)
        val v = p.or()
        if (p.i < tokens.size) throw ProgException(ProgError.SYNTAX)
        return v
    }

    // Precedence, loosest first, as in C: | then ^ then & then shifts then + − then × ÷ mod then unary.
    private class Parser(val t: List<String>, val radix: Radix) {
        var i = 0
        fun peek() = t.getOrNull(i)

        fun or(): Long {
            var v = xor()
            while (peek() == OR) { i++; v = v or xor() }
            return v
        }

        fun xor(): Long {
            var v = and()
            while (peek() == XOR) { i++; v = v xor and() }
            return v
        }

        fun and(): Long {
            var v = shift()
            while (peek() == AND) { i++; v = v and shift() }
            return v
        }

        fun shift(): Long {
            var v = additive()
            while (peek() == SHL || peek() == SHR) {
                val op = t[i++]
                val n = additive()
                if (n < 0 || n > 63) throw ProgException(ProgError.SHIFT)
                v = if (op == SHL) v shl n.toInt() else v shr n.toInt()
            }
            return v
        }

        fun additive(): Long {
            var v = term()
            while (peek() == PLUS || peek() == MINUS) {
                val op = t[i++]
                val r = term()
                v = if (op == PLUS) v + r else v - r
            }
            return v
        }

        fun term(): Long {
            var v = unary()
            while (peek() == TIMES || peek() == DIVIDE || peek() == MOD) {
                val op = t[i++]
                val r = unary()
                v = when (op) {
                    TIMES -> v * r
                    else -> {
                        if (r == 0L) throw ProgException(ProgError.DIV_ZERO)
                        if (op == DIVIDE) v / r else v % r
                    }
                }
            }
            return v
        }

        fun unary(): Long = when (peek()) {
            MINUS -> { i++; -unary() }
            NOT -> { i++; unary().inv() }
            else -> primary()
        }

        fun primary(): Long {
            val tok = peek() ?: throw ProgException(ProgError.SYNTAX)
            return when {
                isNumber(tok) -> {
                    i++
                    try {
                        valueOf(tok, radix)
                    } catch (e: NumberFormatException) {
                        throw ProgException(ProgError.SYNTAX)
                    }
                }
                tok == OPEN -> {
                    i++
                    val v = or()
                    when (peek()) {
                        CLOSE -> i++
                        null -> Unit
                        else -> throw ProgException(ProgError.SYNTAX)
                    }
                    v
                }
                else -> throw ProgException(ProgError.SYNTAX)
            }
        }
    }
}

/**
 * The programmer keypad's line: tokens in the current [radix], whether it shows a finished result, and the
 * last error. Every change returns a new state.
 */
data class ProgState(
    val tokens: List<String> = emptyList(),
    val radix: Radix = Radix.DEC,
    val evaluated: Boolean = false,
    val result: Long? = null,
    val error: ProgError? = null,
) {
    private val last get() = tokens.lastOrNull()

    private val openBrackets get() = tokens.count { it == Programmer.OPEN } - tokens.count { it == Programmer.CLOSE }

    private fun endsOperand() = Programmer.isNumber(last) || last == Programmer.CLOSE

    private fun fresh() = ProgState(radix = radix)

    private fun cleared() = if (error != null) copy(error = null) else this

    /** Continues from a result: its value as a number token (a negative one as "−" and its size). */
    private fun continued(): ProgState {
        val v = result ?: return ProgState(tokens = tokens, radix = radix)
        val t = if (v < 0 && radix == Radix.DEC) listOf(Programmer.MINUS, Programmer.tokenOf(-v, radix)) else listOf(Programmer.tokenOf(v, radix))
        return ProgState(tokens = t, radix = radix)
    }

    /** A digit 0–9 or A–F; ignored when the base doesn't have it or the number would no longer fit in 64 bits. */
    fun digit(d: Char): ProgState {
        if (!Programmer.digitAllowed(d, radix)) return this
        val base = if (evaluated) fresh() else cleared()
        val l = base.last
        if (Programmer.isNumber(l)) {
            val digits = Programmer.digitsOf(l!!)
            val next = if (digits == "0") d.uppercase() else digits + d.uppercaseChar()
            if (!fits(next)) return base
            return base.copy(tokens = base.tokens.dropLast(1) + ("#$next"))
        }
        val t = if (l == Programmer.CLOSE) base.tokens + Programmer.TIMES else base.tokens
        return base.copy(tokens = t + "#${d.uppercaseChar()}")
    }

    private fun fits(digits: String): Boolean = try {
        java.lang.Long.parseUnsignedLong(digits, radix.radix)
        true
    } catch (e: NumberFormatException) {
        false
    }

    /** A two-sided operator. After a result, the line continues from it. */
    fun operator(op: String): ProgState {
        val base = if (evaluated) continued() else cleared()
        val l = base.last
        return when {
            l == null || l == Programmer.OPEN -> if (op == Programmer.MINUS) base.copy(tokens = base.tokens + op) else base
            l in Programmer.BINARY || l == Programmer.NOT -> {
                if (op == Programmer.MINUS && l != Programmer.MINUS) return base.copy(tokens = base.tokens + op)
                val t = base.tokens.dropLast(1)
                if (t.isEmpty() || t.last() == Programmer.OPEN || t.last() in Programmer.BINARY) base else base.copy(tokens = t + op)
            }
            else -> base.copy(tokens = base.tokens + op)
        }
    }

    /** NOT, which flips every bit of what follows. After a result, it applies to the result. */
    fun not(): ProgState {
        if (evaluated) {
            val c = continued()
            return c.copy(tokens = listOf(Programmer.NOT, Programmer.OPEN) + c.tokens + Programmer.CLOSE)
        }
        val base = cleared()
        val t = if (base.endsOperand()) base.tokens + Programmer.TIMES else base.tokens
        return base.copy(tokens = t + Programmer.NOT)
    }

    /** The single bracket key: closes a bracket when one is open and the line ends in a number, opens one otherwise. */
    fun bracket(): ProgState {
        val base = if (evaluated) continued() else cleared()
        if (base.openBrackets > 0 && base.endsOperand()) return base.copy(tokens = base.tokens + Programmer.CLOSE)
        val t = if (base.endsOperand()) base.tokens + Programmer.TIMES else base.tokens
        return base.copy(tokens = t + Programmer.OPEN)
    }

    /** ±: puts a minus in front of the number being typed, or takes it away. */
    fun negate(): ProgState {
        val base = if (evaluated) continued() else cleared()
        val t = base.tokens
        if (!Programmer.isNumber(t.lastOrNull())) {
            return if (t.lastOrNull() != Programmer.MINUS) base.operator(Programmer.MINUS) else base
        }
        val before = t.getOrNull(t.size - 2)
        val beforeThat = t.getOrNull(t.size - 3)
        val unary = before == Programmer.MINUS &&
            (beforeThat == null || beforeThat in Programmer.BINARY || beforeThat == Programmer.OPEN || beforeThat == Programmer.NOT)
        return if (unary) {
            base.copy(tokens = t.subList(0, t.size - 2) + t.last())
        } else {
            base.copy(tokens = t.dropLast(1) + Programmer.MINUS + t.last())
        }
    }

    fun backspace(): ProgState {
        val base = if (evaluated) continued() else cleared()
        val l = base.last ?: return base
        if (Programmer.isNumber(l) && l.length > 2) return base.copy(tokens = base.tokens.dropLast(1) + l.dropLast(1))
        return base.copy(tokens = base.tokens.dropLast(1))
    }

    fun clear() = ProgState(radix = radix)

    /** Switches the base: every number typed so far is rewritten in the new base, keeping its value. */
    fun withRadix(r: Radix): ProgState {
        if (r == radix) return this
        val t = tokens.map { if (Programmer.isNumber(it)) Programmer.tokenOf(Programmer.valueOf(it, radix), r) else it }
        return copy(tokens = t, radix = r)
    }

    /** The value of the line as typed so far, or null when it isn't complete. */
    fun preview(): Long? {
        if (evaluated) return result
        if (tokens.isEmpty() || last in Programmer.BINARY || last == Programmer.NOT) return null
        return try {
            Programmer.evaluate(tokens, radix)
        } catch (e: ProgException) {
            null
        }
    }

    fun finish(): ProgState {
        if (tokens.isEmpty() || evaluated) return this
        return try {
            val v = Programmer.evaluate(tokens, radix)
            ProgState(tokens = tokens, radix = radix, evaluated = true, result = v)
        } catch (e: ProgException) {
            copy(error = e.error)
        }
    }
}
