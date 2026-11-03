package com.vknxworks.sumctrl.data

import com.vknxworks.sumctrl.data.calc.Engine
import com.vknxworks.sumctrl.data.settings.AppSettings
import com.vknxworks.sumctrl.data.settings.DecimalSeparator
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * How numbers look everywhere in SumCtrl: the decimal separator (the phone's, or a fixed comma or point)
 * and optional thousands grouping. Input fields accept both "," and "." whatever is chosen here.
 */
class NumFmt(val decimal: Char, val group: Char?, currencyCode: String) {

    /** The currency symbol for money amounts in the work tools, e.g. "€". */
    val currency: String = runCatching { Currency.getInstance(currencyCode).getSymbol(Locale.getDefault()) }.getOrDefault(currencyCode)

    /** A calculator result: 12 significant digits, scientific notation when very big or very small. */
    fun result(v: BigDecimal): String {
        val r = Engine.rounded(v)
        if (r.signum() == 0) return "0"
        val exp = r.precision() - r.scale() - 1
        if (exp >= 15 || exp < -9) return scientific(r, exp)
        return grouped(r.toPlainString())
    }

    /** A converted value: up to [significant] significant digits, no trailing zeros. */
    fun number(v: BigDecimal, significant: Int = 10): String {
        if (v.signum() == 0) return "0"
        val r = v.round(MathContext(significant, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val exp = r.precision() - r.scale() - 1
        if (exp >= 15 || exp < -9) return scientific(r, exp)
        return grouped(r.toPlainString())
    }

    /** Money: always two decimals, with the currency symbol. */
    fun money(v: BigDecimal): String = "$currency " + grouped(v.setScale(2, RoundingMode.HALF_UP).toPlainString())

    /** A percentage with up to two decimals: "21 %", "12,5 %". */
    fun percent(v: BigDecimal): String =
        grouped(v.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }.toPlainString()) + " %"

    private fun scientific(r: BigDecimal, exp: Int): String {
        val mantissa = r.movePointLeft(exp).round(MathContext(10, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val m = mantissa.let { if (it.scale() < 0) it.setScale(0) else it }.toPlainString().replace('.', decimal)
        return "${m}E$exp"
    }

    /** "-1234567.5" → "-1.234.567,5" (with the chosen separators). */
    fun grouped(plain: String): String {
        val neg = plain.startsWith("-")
        val body = plain.removePrefix("-")
        val intPart = body.substringBefore('.')
        val frac = body.substringAfter('.', "")
        val sb = StringBuilder()
        if (group != null && intPart.length > 3) {
            intPart.forEachIndexed { i, c ->
                if (i > 0 && (intPart.length - i) % 3 == 0) sb.append(group)
                sb.append(c)
            }
        } else {
            sb.append(intPart)
        }
        if (body.contains('.')) sb.append(decimal).append(frac)
        return (if (neg) "−" else "") + sb
    }

    companion object {
        fun from(s: AppSettings): NumFmt {
            val decimal = when (s.decimalSeparator) {
                DecimalSeparator.COMMA -> ','
                DecimalSeparator.POINT -> '.'
                DecimalSeparator.PHONE -> DecimalFormatSymbols.getInstance().decimalSeparator
            }
            // A thin no-break space never clashes with either separator.
            val group = if (s.groupThousands) ' ' else null
            return NumFmt(decimal, group, s.currency)
        }

        /**
         * Reads a typed number. "," and "." both work as the decimal separator; when both appear, the last one
         * is the decimal separator and the others are thousands groups ("1.234,5" and "1,234.5" are both 1234.5).
         */
        fun parse(text: String): BigDecimal? {
            var t = text.trim().replace(" ", "").replace(" ", "").replace('−', '-')
            if (t.isEmpty() || t == "-") return null
            val last = maxOf(t.lastIndexOf(','), t.lastIndexOf('.'))
            if (last >= 0) {
                val head = t.substring(0, last).replace(",", "").replace(".", "")
                t = head + "." + t.substring(last + 1)
            }
            return t.toBigDecimalOrNull()
        }
    }
}
