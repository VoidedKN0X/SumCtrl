package com.vknxworks.sumctrl.data.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class TextExprTest {
    private fun eval(s: String): BigDecimal? = TextExpr.parse(s)?.let { Engine.evaluate(it, true) }

    private fun assertValue(expected: String, text: String) =
        assertEquals(text, 0, BigDecimal(expected).compareTo(eval(text)))

    @Test fun commonSelections() {
        assertValue("15.70", "12,50 + 3,20")
        assertValue("13.5", "3 x 4.5")
        assertValue("179.1", "€ 199 − 10%")
        assertValue("2.5", "10 / 4")
        assertValue("2.5", "10 : 4")
        assertValue("1000001", "1.000.000 + 1")
        assertValue("1234.5", "1.234,5")
        assertValue("1234.5", "1,234.5")
        assertValue("1235.5", "1 234,5 + 1")
        assertValue("20", "(2 + 3) * 4 =")
        assertValue("-3", "-3")
    }

    @Test fun notASum() {
        assertNull(TextExpr.parse("hello"))
        assertNull(TextExpr.parse(""))
        assertNull(TextExpr.parse("+ -"))
        assertNull(TextExpr.parse("1".repeat(TextExpr.MAX_LENGTH + 1)))
    }

    @Test fun answerFollowsTheSelection() {
        assertEquals(',', TextExpr.decimalOf("12,50 + 3,20"))
        assertEquals('.', TextExpr.decimalOf("3 x 4.5"))
        assertNull(TextExpr.decimalOf("1.000.000 + 1"))
        assertEquals("15,7", TextExpr.answer(BigDecimal("15.70"), ','))
        assertEquals("-0.5", TextExpr.answer(BigDecimal("-0.5"), '.'))
    }
}
