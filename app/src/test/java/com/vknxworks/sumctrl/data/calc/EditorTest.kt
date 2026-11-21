package com.vknxworks.sumctrl.data.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class EditorTest {
    private fun type(vararg keys: (CalcState) -> CalcState): CalcState = keys.fold(CalcState()) { s, k -> s.edit(k) }
    private fun d(x: String): (CalcState) -> CalcState = { it.digit(x) }
    private fun op(x: String): (CalcState) -> CalcState = { it.operator(x) }

    @Test fun cursorInsertsInTheMiddle() {
        // 12+3, cursor after "1", type 5 → 152+3
        var s = type(d("1"), d("2"), op(Engine.PLUS), d("3")).moveCursor(1)
        s = s.edit { it.digit("5") }
        assertEquals(listOf("1", "5", "2", Engine.PLUS, "3"), s.tokens)
        assertEquals(2, s.cursor)
        assertEquals(0, BigDecimal("155").compareTo(s.equals(true).second))
    }

    @Test fun backspaceAtCursorRemovesTheKeyBeforeIt() {
        var s = type(d("1"), d("2"), op(Engine.PLUS), d("3")).moveCursor(2)
        s = s.edit { it.backspace() }
        assertEquals(listOf("1", Engine.PLUS, "3"), s.tokens)
        assertEquals(1, s.cursor)
    }

    @Test fun cursorAtEndIsNull() {
        val s = type(d("1"), d("2")).moveCursor(2)
        assertNull(s.cursor)
        val t = type(d("1"), d("2")).moveCursor(99)
        assertNull(t.cursor)
    }

    @Test fun operatorAtCursorInFrontOfANumber() {
        // 23, cursor at 0, then 4 and × → 4×23
        var s = type(d("2"), d("3")).moveCursor(0)
        s = s.edit { it.digit("4") }.edit { it.operator(Engine.TIMES) }
        assertEquals(listOf("4", Engine.TIMES, "2", "3"), s.tokens)
        assertEquals(0, BigDecimal("92").compareTo(s.equals(true).second))
    }

    @Test fun movingTheCursorOnAResultMakesItEditable() {
        val (r, _) = type(d("2"), op(Engine.PLUS), d("3")).equals(true)
        val s = r.moveCursor(0)
        assertEquals(false, s.evaluated)
        assertEquals(listOf("5"), s.tokens)
        assertEquals(0, s.cursor)
    }

    @Test fun clearDropsTheCursor() {
        val s = type(d("1"), d("2")).moveCursor(1).clear()
        assertNull(s.cursor)
        assertEquals(emptyList<String>(), s.tokens)
    }

    @Test fun memoryAndNamedValues() {
        val values = mapOf(Engine.MEMORY to BigDecimal("10"), "rate" to BigDecimal("0.21"))
        val s = type(d("2"), { it.constant(Engine.MEMORY) }, op(Engine.PLUS), { it.constant(Engine.variableToken("rate")) })
        // 2M + rate = 20.21 (a value right after a number multiplies)
        assertEquals(listOf("2", Engine.TIMES, Engine.MEMORY, Engine.PLUS, "\$rate"), s.tokens)
        assertEquals(0, BigDecimal("20.21").compareTo(s.equals(true, values).second))
    }

    @Test fun unknownValueIsAnError() {
        val s = type({ it.constant(Engine.variableToken("gone")) })
        val (r, v) = s.equals(true, emptyMap())
        assertNull(v)
        assertEquals(CalcError.UNKNOWN_VALUE, r.error)
    }

    @Test fun currentValue() {
        assertEquals(0, BigDecimal("7").compareTo(type(d("3"), op(Engine.PLUS), d("4")).currentValue(true)))
        assertNull(type(d("3"), op(Engine.PLUS)).currentValue(true))
        assertNull(CalcState().currentValue(true))
    }

    @Test fun namedValueKeepsAllDigits() {
        val exact = BigDecimal("1.23456789012345678901")
        val s = type({ it.constant(Engine.variableToken("x")) }, op(Engine.TIMES), d("1"))
        assertEquals(0, exact.compareTo(s.equals(true, mapOf("x" to exact)).second))
    }
}
