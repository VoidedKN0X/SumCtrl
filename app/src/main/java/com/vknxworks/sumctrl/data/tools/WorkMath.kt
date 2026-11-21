package com.vknxworks.sumctrl.data.tools

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

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

    // ---------- Tip and split ----------

    /**
     * A bill with [tipPercent] tip on top, shared by [people]. [perPerson] is the exact share; [perPersonRounded]
     * is that share rounded up to the cent, so that everyone paying it covers the bill, and [paidRounded] is
     * what that adds up to. Null for fewer than one person.
     */
    data class Tip(
        val tip: BigDecimal,
        val total: BigDecimal,
        val perPerson: BigDecimal,
        val perPersonRounded: BigDecimal,
        val paidRounded: BigDecimal,
    )

    fun tip(bill: BigDecimal, tipPercent: BigDecimal, people: Int): Tip? {
        if (people < 1) return null
        val tip = bill.multiply(pct(tipPercent), MC)
        val total = bill.add(tip, MC)
        val n = BigDecimal(people)
        val per = total.divide(n, MC)
        val perRounded = per.setScale(2, RoundingMode.CEILING)
        return Tip(tip, total, per, perRounded, perRounded.multiply(n))
    }

    // ---------- Date difference ----------

    /**
     * From [from] to [to]: [days] in between (negative when [to] is earlier), the same as whole [weeks] and
     * [weekDays] left over, as a calendar [period] (years, months, days), and the [workdays] (Monday to Friday)
     * among the days counted: from the earlier date up to, but not including, the later one.
     */
    data class DateDiff(val days: Long, val weeks: Long, val weekDays: Long, val period: Period, val workdays: Long)

    fun dateDiff(from: LocalDate, to: LocalDate): DateDiff {
        val days = ChronoUnit.DAYS.between(from, to)
        val start = if (days >= 0) from else to
        val end = if (days >= 0) to else from
        val span = kotlin.math.abs(days)
        // Whole weeks have five workdays each; then the days left over one by one.
        var workdays = (span / 7) * 5
        var d = start.plusDays((span / 7) * 7)
        while (d.isBefore(end)) {
            if (d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY) workdays++
            d = d.plusDays(1)
        }
        val period = Period.between(start, end)
        return DateDiff(days, span / 7, span % 7, period, workdays)
    }

    // ---------- Loan table ----------

    data class LoanRow(val month: Int, val payment: BigDecimal, val interest: BigDecimal, val principal: BigDecimal, val balance: BigDecimal)

    data class Loan(val payment: BigDecimal, val rows: List<LoanRow>, val totalPaid: BigDecimal, val totalInterest: BigDecimal)

    /** The most months the loan table lists (50 years). */
    const val MAX_LOAN_MONTHS = 600

    /**
     * A loan of [amount] paid back over [months] with fixed monthly payments at a nominal yearly rate of
     * [yearlyRate] percent (the rate divided by 12 each month), worked out in cents as a bank statement does:
     * the payment is rounded to the cent, each month's interest is rounded to the cent, and the last payment
     * takes whatever is left so the balance ends at exactly zero. Null for no months, more than
     * [MAX_LOAN_MONTHS], a negative amount or a negative rate.
     */
    fun loan(amount: BigDecimal, yearlyRate: BigDecimal, months: Int): Loan? {
        if (months < 1 || months > MAX_LOAN_MONTHS || amount.signum() < 0 || yearlyRate.signum() < 0) return null
        val principal = amount.setScale(2, RoundingMode.HALF_UP)
        val r = yearlyRate.divide(BigDecimal(1200), MC)
        val exactPayment = if (r.signum() == 0) {
            principal.divide(BigDecimal(months), MC)
        } else {
            val growth = BigDecimal.ONE.add(r).pow(-months, MC)
            principal.multiply(r, MC).divide(BigDecimal.ONE.subtract(growth), MC)
        }
        val payment = exactPayment.setScale(2, RoundingMode.HALF_UP)
        val rows = ArrayList<LoanRow>(months)
        var balance = principal
        var paid = BigDecimal.ZERO
        var interestTotal = BigDecimal.ZERO
        for (m in 1..months) {
            val interest = balance.multiply(r, MC).setScale(2, RoundingMode.HALF_UP)
            var toPrincipal = payment.subtract(interest)
            if (m == months || toPrincipal > balance) toPrincipal = balance
            val pay = toPrincipal.add(interest)
            balance = balance.subtract(toPrincipal)
            paid = paid.add(pay)
            interestTotal = interestTotal.add(interest)
            rows += LoanRow(m, pay, interest, toPrincipal, balance)
        }
        return Loan(payment, rows, paid, interestTotal)
    }

    // ---------- Currency with your own rate ----------

    /** [amount] at [rate] (one unit of the first currency is [rate] of the second). */
    fun convert(amount: BigDecimal, rate: BigDecimal): BigDecimal = amount.multiply(rate, MC)

    /** The rate the other way round (1 ÷ [rate]); null for a zero rate. */
    fun inverseRate(rate: BigDecimal): BigDecimal? = if (rate.signum() == 0) null else BigDecimal.ONE.divide(rate, MC)
}
