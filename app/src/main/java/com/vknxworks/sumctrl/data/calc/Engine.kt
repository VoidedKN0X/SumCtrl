package com.vknxworks.sumctrl.data.calc

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.log10

/**
 * The calculator's expression engine. An expression is a list of key tokens exactly as typed on the keypad
 * ("1", "2", ".", "+", "sin(", "π", ")", "%"...), so backspace removes a whole key and the display never has
 * to be parsed back.
 *
 * Arithmetic (+ − × ÷, whole powers, %, !) is exact decimal maths with 34 significant digits, so 0.1 + 0.2 is
 * 0.3 and money sums never pick up binary noise. Functions (sin, ln, √, fractional powers...) go through
 * double precision and are rounded for display.
 */
object Engine {
    const val PLUS = "+"
    const val MINUS = "−"
    const val TIMES = "×"
    const val DIVIDE = "÷"
    const val POWER = "^"
    const val OPEN = "("
    const val CLOSE = ")"
    const val PERCENT = "%"
    const val FACTORIAL = "!"
    const val POINT = "."
    const val PI = "π"
    const val E = "e"
    const val ANS = "Ans"

    /** Functions; each token includes its opening bracket. */
    val FUNCTIONS = listOf("sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "ln(", "log(", "√(")

    val BINARY = setOf(PLUS, MINUS, TIMES, DIVIDE, POWER)

    private val MC = MathContext.DECIMAL128

    /** Largest decimal exponent a result may have before it counts as too big. */
    private const val MAX_EXP = 9_999

    fun isDigit(t: String) = t.length == 1 && t[0] in '0'..'9'
    fun isNumberPart(t: String) = isDigit(t) || t == POINT
    fun isFunction(t: String) = t in FUNCTIONS
    fun isConstant(t: String) = t == PI || t == E || t == ANS
    fun isPostfix(t: String) = t == PERCENT || t == FACTORIAL

    /** Whether something can follow [t] as an operand end (a number, constant, ")" or postfix). */
    fun endsOperand(t: String?) = t != null && (isNumberPart(t) || isConstant(t) || t == CLOSE || isPostfix(t))

    /**
     * Evaluates [tokens]. Missing closing brackets at the end are added. [degrees] sets the angle unit for
     * trig functions, [ans] is the value of "Ans".
     */
    fun evaluate(tokens: List<String>, degrees: Boolean, ans: BigDecimal = BigDecimal.ZERO): BigDecimal {
        if (tokens.isEmpty()) throw CalcException(CalcError.SYNTAX)
        val parser = Parser(lex(tokens), degrees, ans)
        val v = parser.parseExpression()
        if (!parser.atEnd()) throw CalcException(CalcError.SYNTAX)
        return check(v.v)
    }

    // ---------- Lexing ----------

    private sealed interface Lex {
        data class Num(val v: BigDecimal) : Lex
        data class Op(val op: String) : Lex
        data class Fn(val name: String) : Lex
        data object Open : Lex
        data object Close : Lex
        data class Post(val op: String) : Lex
    }

    private fun lex(tokens: List<String>): List<Lex> {
        val out = ArrayList<Lex>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            when {
                isNumberPart(t) -> {
                    val sb = StringBuilder()
                    while (i < tokens.size && isNumberPart(tokens[i])) sb.append(tokens[i++])
                    val s = sb.toString()
                    if (s.count { it == '.' } > 1 || s == ".") throw CalcException(CalcError.SYNTAX)
                    out += Lex.Num(BigDecimal(if (s.startsWith(".")) "0$s" else if (s.endsWith(".")) "${s}0" else s))
                    continue
                }
                t == PI -> out += Lex.Num(BigDecimal("3.141592653589793238462643383279503"))
                t == E -> out += Lex.Num(BigDecimal("2.718281828459045235360287471352662"))
                t == ANS -> out += Lex.Num(NANS)
                t in BINARY -> out += Lex.Op(t)
                t == "-" -> out += Lex.Op(MINUS)
                t == OPEN -> out += Lex.Open
                t == CLOSE -> out += Lex.Close
                isPostfix(t) -> out += Lex.Post(t)
                isFunction(t) -> out += Lex.Fn(t.dropLast(1))
                else -> throw CalcException(CalcError.SYNTAX)
            }
            i++
        }
        return out
    }

    /** Placeholder for "Ans", replaced by the real value in the parser. Never a real input value. */
    private val NANS = BigDecimal("-0.000000000000000000000000000000000000000000001234567")

    // ---------- Parsing and evaluation ----------

    /** A value, and whether it came straight from a "%" (so "a + b%" means a plus b percent of a). */
    private data class V(val v: BigDecimal, val percent: Boolean = false)

    private class Parser(val lx: List<Lex>, val degrees: Boolean, val ans: BigDecimal) {
        var p = 0
        fun atEnd() = p >= lx.size
        fun peek(): Lex? = lx.getOrNull(p)

        // expression := term (('+' | '−') term)*
        fun parseExpression(): V {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Lex.Op && (t.op == PLUS || t.op == MINUS)) {
                    p++
                    val right = parseTerm()
                    // "200 + 21%": 21 percent of 200 on top, like a shop calculator.
                    val r = if (right.percent) left.v.multiply(right.v, MC) else right.v
                    left = V(check(if (t.op == PLUS) left.v.add(r, MC) else left.v.subtract(r, MC)))
                } else {
                    return left
                }
            }
        }

        // term := unary (('×' | '÷' | implicit) unary)*
        fun parseTerm(): V {
            var left = parseUnary()
            while (true) {
                val t = peek()
                when {
                    t is Lex.Op && (t.op == TIMES || t.op == DIVIDE) -> {
                        p++
                        val right = parseUnary().v
                        left = if (t.op == TIMES) {
                            V(check(left.v.multiply(right, MC)))
                        } else {
                            if (right.signum() == 0) throw CalcException(CalcError.DIV_ZERO)
                            V(check(left.v.divide(right, MC)))
                        }
                    }
                    // "2π", "3(4+1)", "2sin(30)", "(1+2)(3+4)": multiplication without a sign.
                    t is Lex.Num || t is Lex.Open || t is Lex.Fn -> {
                        val right = parseUnary().v
                        left = V(check(left.v.multiply(right, MC)))
                    }
                    else -> return left
                }
            }
        }

        // unary := '−' unary | power. So −2^2 is −4, like on paper.
        fun parseUnary(): V {
            val t = peek()
            if (t is Lex.Op && t.op == MINUS) {
                p++
                val v = parseUnary()
                return V(v.v.negate(), v.percent)
            }
            if (t is Lex.Op && t.op == PLUS) {
                p++
                return parseUnary()
            }
            return parsePower()
        }

        // power := postfix ('^' unary)?   right-associative: 2^3^2 = 2^9
        fun parsePower(): V {
            val base = parsePostfix()
            val t = peek()
            if (t is Lex.Op && t.op == POWER) {
                p++
                val exp = parseUnary().v
                return V(power(base.v, exp))
            }
            return base
        }

        // postfix := primary ('!' | '%')*
        fun parsePostfix(): V {
            var v = parsePrimary()
            while (true) {
                val t = peek()
                if (t is Lex.Post) {
                    p++
                    v = if (t.op == PERCENT) {
                        V(v.v.divide(BigDecimal(100), MC), percent = true)
                    } else {
                        V(factorial(v.v))
                    }
                } else {
                    return v
                }
            }
        }

        fun parsePrimary(): V {
            return when (val t = peek()) {
                is Lex.Num -> {
                    p++
                    V(if (t.v === NANS) ans else t.v)
                }
                is Lex.Open -> {
                    p++
                    val v = parseExpression()
                    closeBracket()
                    V(v.v)
                }
                is Lex.Fn -> {
                    p++
                    val arg = parseExpression()
                    closeBracket()
                    V(function(t.name, arg.v))
                }
                else -> throw CalcException(CalcError.SYNTAX)
            }
        }

        /** A ")" — or nothing at the very end, so "sin(30" and "(2+3" still work. */
        fun closeBracket() {
            when (peek()) {
                is Lex.Close -> p++
                null -> Unit
                else -> throw CalcException(CalcError.SYNTAX)
            }
        }

        fun function(name: String, x: BigDecimal): BigDecimal {
            val d = x.toDouble()
            val rad = if (degrees) Math.toRadians(d) else d
            val r: Double = when (name) {
                "sin" -> if (degrees && isMultiple(x, 180)) 0.0 else kotlin.math.sin(rad)
                "cos" -> if (degrees && isMultiple(x.subtract(BigDecimal(90)), 180)) 0.0 else kotlin.math.cos(rad)
                "tan" -> {
                    if (degrees && isMultiple(x.subtract(BigDecimal(90)), 180)) throw CalcException(CalcError.DOMAIN)
                    if (degrees && isMultiple(x, 180)) 0.0 else kotlin.math.tan(rad)
                }
                "asin", "acos" -> {
                    if (d < -1.0 || d > 1.0) throw CalcException(CalcError.DOMAIN)
                    val a = if (name == "asin") kotlin.math.asin(d) else kotlin.math.acos(d)
                    if (degrees) Math.toDegrees(a) else a
                }
                "atan" -> kotlin.math.atan(d).let { if (degrees) Math.toDegrees(it) else it }
                "ln" -> {
                    if (x.signum() <= 0) throw CalcException(CalcError.DOMAIN)
                    ln(x)
                }
                "log" -> {
                    if (x.signum() <= 0) throw CalcException(CalcError.DOMAIN)
                    // Exact for powers of ten: log(1000) is 3, not 2.9999999999999996.
                    val s = x.stripTrailingZeros()
                    if (s.unscaledValue() == BigInteger.ONE) return BigDecimal(-s.scale())
                    ln(x) / kotlin.math.ln(10.0)
                }
                "√" -> {
                    if (x.signum() < 0) throw CalcException(CalcError.DOMAIN)
                    return x.sqrt(MC)
                }
                else -> throw CalcException(CalcError.SYNTAX)
            }
            return fromDouble(r)
        }
    }

    /** Natural log that also works for numbers outside the double range. */
    private fun ln(x: BigDecimal): Double {
        val s = x.stripTrailingZeros()
        val digits = s.precision() - s.scale() - 1
        val mantissa = s.movePointLeft(digits).toDouble()
        return kotlin.math.ln(mantissa) + digits * kotlin.math.ln(10.0)
    }

    private fun isMultiple(x: BigDecimal, of: Int): Boolean =
        x.remainder(BigDecimal(of)).signum() == 0

    private fun fromDouble(d: Double): BigDecimal {
        if (d.isNaN()) throw CalcException(CalcError.DOMAIN)
        if (d.isInfinite()) throw CalcException(CalcError.TOO_BIG)
        return BigDecimal(d.toString())
    }

    private fun power(base: BigDecimal, exp: BigDecimal): BigDecimal {
        if (base.signum() == 0) {
            if (exp.signum() < 0) throw CalcException(CalcError.DIV_ZERO)
            return if (exp.signum() == 0) BigDecimal.ONE else BigDecimal.ZERO
        }
        val whole = exp.stripTrailingZeros().scale() <= 0
        // Rough size of the result, to refuse anything absurd before computing it.
        val size = exp.toDouble() * log10(abs(base.toDouble()).takeIf { it != 0.0 && !it.isInfinite() } ?: digitsOf(base))
        if (size.isNaN() || abs(size) > MAX_EXP) throw CalcException(CalcError.TOO_BIG)
        if (whole && exp.abs() <= BigDecimal(999_999)) {
            val n = exp.intValueExact()
            return check(base.pow(n, MC))
        }
        if (base.signum() < 0) {
            // Odd roots of negatives, like (−8)^(1/3), are real; anything else isn't.
            val inv = BigDecimal.ONE.divide(exp, MC)
            val invWhole = inv.round(MathContext(12)).stripTrailingZeros()
            if (invWhole.scale() <= 0 && invWhole.toBigInteger().testBit(0)) {
                return fromDouble(-Math.pow(-base.toDouble(), exp.toDouble()))
            }
            throw CalcException(CalcError.DOMAIN)
        }
        return fromDouble(Math.pow(base.toDouble(), exp.toDouble()))
    }

    private fun digitsOf(x: BigDecimal): Double {
        val s = x.abs().stripTrailingZeros()
        return (s.precision() - s.scale()).toDouble()
    }

    private fun factorial(x: BigDecimal): BigDecimal {
        val s = x.stripTrailingZeros()
        if (s.signum() < 0 || s.scale() > 0) throw CalcException(CalcError.DOMAIN)
        if (s > BigDecimal(3_000)) throw CalcException(CalcError.TOO_BIG)
        var r = BigInteger.ONE
        for (i in 2..s.toInt()) r = r.multiply(BigInteger.valueOf(i.toLong()))
        return BigDecimal(r).round(MC)
    }

    private fun check(v: BigDecimal): BigDecimal {
        if (v.signum() == 0) return BigDecimal.ZERO
        val s = v.stripTrailingZeros()
        val exp = s.precision() - s.scale() - 1
        if (exp > MAX_EXP) throw CalcException(CalcError.TOO_BIG)
        // Far too small to show: it's zero.
        if (exp < -MAX_EXP) return BigDecimal.ZERO
        return v
    }

    /** Rounds a result the way it's shown and reused: 12 significant digits, no trailing zeros. */
    fun rounded(v: BigDecimal): BigDecimal {
        if (v.signum() == 0) return BigDecimal.ZERO
        return v.round(MathContext(12, RoundingMode.HALF_EVEN)).stripTrailingZeros()
    }

    /**
     * The keys that type [v] back in, for continuing from a result. Very long or tiny numbers use "Ans"
     * instead, so nothing is lost.
     */
    fun tokensOf(v: BigDecimal): List<String> {
        val r = rounded(v)
        val plain = r.abs().toPlainString()
        if (plain.length > 16) return listOf(ANS)
        val digits = plain.map { it.toString() }
        return if (r.signum() < 0) listOf(MINUS) + digits else digits
    }
}

enum class CalcError { SYNTAX, DIV_ZERO, DOMAIN, TOO_BIG }

class CalcException(val error: CalcError) : Exception(error.name)
