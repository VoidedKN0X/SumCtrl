package com.vknxworks.sumctrl.data.tools

import java.math.BigDecimal
import java.math.MathContext

private val MC = MathContext.DECIMAL128
private val HUNDRED = BigDecimal(100)

private fun pct(p: BigDecimal) = p.divide(HUNDRED, MC)

/** The maths behind the work tools: exact decimals, rounded only when shown. */
object WorkMath {

    data class Vat(val excl: BigDecimal, val vat: BigDecimal, val incl: BigDecimal)

    /** [amount] with or without VAT at [rate] percent. */
    fun vat(amount: BigDecimal, rate: BigDecimal, amountIncludesVat: Boolean): Vat {
        val factor = BigDecimal.ONE.add(pct(rate))
        return if (amountIncludesVat) {
            val excl = amount.divide(factor, MC)
            Vat(excl, amount.subtract(excl), amount)
        } else {
            val incl = amount.multiply(factor, MC)
            Vat(amount, incl.subtract(amount), incl)
        }
    }

    data class Discount(val final: BigDecimal, val saved: BigDecimal, val effectivePercent: BigDecimal)

    /** [price] with [first] percent off, then [second] percent off what's left (both can be zero). */
    fun discount(price: BigDecimal, first: BigDecimal, second: BigDecimal): Discount {
        val afterFirst = price.multiply(BigDecimal.ONE.subtract(pct(first)), MC)
        val final = afterFirst.multiply(BigDecimal.ONE.subtract(pct(second)), MC)
        val saved = price.subtract(final)
        val eff = if (price.signum() == 0) BigDecimal.ZERO else saved.divide(price, MC).multiply(HUNDRED)
        return Discount(final, saved, eff)
    }

    data class Margin(val cost: BigDecimal, val price: BigDecimal, val profit: BigDecimal, val marginPercent: BigDecimal?, val markupPercent: BigDecimal?)

    private fun margin(cost: BigDecimal, price: BigDecimal): Margin {
        val profit = price.subtract(cost)
        val margin = if (price.signum() == 0) null else profit.divide(price, MC).multiply(HUNDRED)
        val markup = if (cost.signum() == 0) null else profit.divide(cost, MC).multiply(HUNDRED)
        return Margin(cost, price, profit, margin, markup)
    }

    /** From what it cost and what it sells for. */
    fun marginFromPrice(cost: BigDecimal, price: BigDecimal) = margin(cost, price)

    /** The selling price that gives [marginPercent] of the price as profit; null for 100 % or more. */
    fun priceForMargin(cost: BigDecimal, marginPercent: BigDecimal): Margin? {
        val keep = BigDecimal.ONE.subtract(pct(marginPercent))
        if (keep.signum() <= 0) return null
        return margin(cost, cost.divide(keep, MC))
    }

    /** The selling price with [markupPercent] added on top of the cost. */
    fun priceForMarkup(cost: BigDecimal, markupPercent: BigDecimal) =
        margin(cost, cost.multiply(BigDecimal.ONE.add(pct(markupPercent)), MC))

    data class Instalments(val financed: BigDecimal, val monthly: BigDecimal, val total: BigDecimal, val interest: BigDecimal)

    /**
     * Paying [price] minus [down] over [months] at a nominal yearly rate of [yearlyRate] percent, with the
     * usual fixed monthly payment (annuity). 0 % is simply the amount split evenly.
     */
    fun instalments(price: BigDecimal, down: BigDecimal, months: Int, yearlyRate: BigDecimal): Instalments? {
        if (months <= 0) return null
        val financed = price.subtract(down)
        if (financed.signum() < 0) return null
        val n = BigDecimal(months)
        val monthly = if (yearlyRate.signum() == 0) {
            financed.divide(n, MC)
        } else {
            val r = yearlyRate.divide(BigDecimal(1200), MC)
            // P·r / (1 − (1 + r)^−n)
            val growth = BigDecimal.ONE.add(r).pow(-months, MC)
            financed.multiply(r, MC).divide(BigDecimal.ONE.subtract(growth), MC)
        }
        val total = monthly.multiply(n, MC).add(down)
        return Instalments(financed, monthly, total, total.subtract(price))
    }

    /** [p] percent of [x]. */
    fun percentOf(p: BigDecimal, x: BigDecimal): BigDecimal = x.multiply(pct(p), MC)

    /** What percent [part] is of [whole]; null when [whole] is zero. */
    fun whatPercent(part: BigDecimal, whole: BigDecimal): BigDecimal? =
        if (whole.signum() == 0) null else part.divide(whole, MC).multiply(HUNDRED)

    /** Change from [from] to [to] in percent; null when [from] is zero. */
    fun change(from: BigDecimal, to: BigDecimal): BigDecimal? =
        if (from.signum() == 0) null else to.subtract(from).divide(from.abs(), MC).multiply(HUNDRED)
}
