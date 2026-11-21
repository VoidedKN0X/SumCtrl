package com.vknxworks.sumctrl.data.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class WorkMathTest {
    private fun eq(expected: String, actual: BigDecimal?) = assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test fun tip() {
        val t = WorkMath.tip(BigDecimal("100"), BigDecimal("10"), 3)!!
        eq("10", t.tip)
        eq("110", t.total)
        eq("36.67", t.perPersonRounded)
        eq("110.01", t.paidRounded)
        assertNull(WorkMath.tip(BigDecimal("10"), BigDecimal("10"), 0))
        // Exact: 0.1 + 0.2 style sums don't drift
        eq("0.33", WorkMath.tip(BigDecimal("0.30"), BigDecimal("10"), 1)!!.total)
    }

    @Test fun dateDifference() {
        // Monday 3 Nov 2025 to Monday 17 Nov 2025: 14 days, 2 weeks, 10 workdays
        val d = WorkMath.dateDiff(LocalDate.of(2025, 11, 3), LocalDate.of(2025, 11, 17))
        assertEquals(14L, d.days)
        assertEquals(2L, d.weeks)
        assertEquals(0L, d.weekDays)
        assertEquals(10L, d.workdays)
        // Friday to Monday: 3 days, 1 workday (the Friday)
        assertEquals(1L, WorkMath.dateDiff(LocalDate.of(2025, 11, 7), LocalDate.of(2025, 11, 10)).workdays)
        // Backwards is negative, same workdays
        val back = WorkMath.dateDiff(LocalDate.of(2025, 11, 17), LocalDate.of(2025, 11, 3))
        assertEquals(-14L, back.days)
        assertEquals(10L, back.workdays)
        // Leap year: 29 Feb 2024 to 1 Mar 2025
        val leap = WorkMath.dateDiff(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 3, 1))
        assertEquals(366L, leap.days)
        assertEquals(1, leap.period.years)
        assertEquals(0, leap.period.months)
        assertEquals(1, leap.period.days)
    }

    @Test fun loanEndsAtZeroInCents() {
        val l = WorkMath.loan(BigDecimal("10000"), BigDecimal("5"), 12)!!
        eq("856.07", l.payment)
        assertEquals(12, l.rows.size)
        eq("0", l.rows.last().balance)
        // Principal adds up to exactly the loan, and the total is principal plus interest.
        eq("10000", l.rows.fold(BigDecimal.ZERO) { a, r -> a.add(r.principal) })
        eq(l.totalPaid.subtract(BigDecimal("10000")).toPlainString(), l.totalInterest)
        eq("41.67", l.rows.first().interest)
        l.rows.forEach { assertEquals(2, it.payment.scale()) }
    }

    @Test fun loanWithoutInterest() {
        val l = WorkMath.loan(BigDecimal("100"), BigDecimal.ZERO, 3)!!
        eq("33.33", l.payment)
        eq("33.34", l.rows.last().payment)
        eq("100", l.totalPaid)
        eq("0", l.totalInterest)
        assertNull(WorkMath.loan(BigDecimal("100"), BigDecimal.ZERO, 0))
        assertNull(WorkMath.loan(BigDecimal("100"), BigDecimal.ZERO, WorkMath.MAX_LOAN_MONTHS + 1))
    }

    @Test fun currency() {
        eq("108.50", WorkMath.convert(BigDecimal("100"), BigDecimal("1.085")))
        eq("0.5", WorkMath.inverseRate(BigDecimal("2")))
        assertNull(WorkMath.inverseRate(BigDecimal.ZERO))
    }
}
