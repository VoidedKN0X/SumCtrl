package com.vknxworks.sumctrl.data.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgrammerTest {
    private fun keys(radix: Radix, s: String): ProgState {
        var st = ProgState(radix = radix)
        s.split(' ').filter { it.isNotEmpty() }.forEach { k ->
            st = when {
                k in Programmer.BINARY -> st.operator(k)
                k == "NOT" -> st.not()
                k == "(" || k == ")" -> st.bracket()
                else -> k.fold(st) { a, c -> a.digit(c) }
            }
        }
        return st
    }

    @Test fun hexArithmetic() {
        assertEquals(0x1F + 0x01L, keys(Radix.HEX, "1F + 1").finish().result)
        assertEquals(0xFFL and 0x0FL, keys(Radix.HEX, "FF AND F").finish().result)
        assertEquals(0xF0L or 0x0FL, keys(Radix.HEX, "F0 OR F").finish().result)
        assertEquals(0xFFL xor 0x0FL, keys(Radix.HEX, "FF XOR F").finish().result)
    }

    @Test fun precedenceLikeC() {
        // 1 + 2 << 3 = (1 + 2) << 3 = 24; 6 AND 3 OR 8 = (6 & 3) | 8 = 10
        assertEquals(24L, keys(Radix.DEC, "1 + 2 << 3").finish().result)
        assertEquals(10L, keys(Radix.DEC, "6 AND 3 OR 8").finish().result)
        assertEquals(14L, keys(Radix.DEC, "2 + 3 × 4").finish().result)
    }

    @Test fun wrapsAt64Bits() {
        val max = keys(Radix.HEX, "7FFFFFFFFFFFFFFF + 1").finish().result
        assertEquals(Long.MIN_VALUE, max)
        assertEquals(-1L, keys(Radix.HEX, "FFFFFFFFFFFFFFFF").finish().result)
    }

    @Test fun refusesDigitsThatDontFitOrDontExist() {
        // a 17th hex digit is ignored
        val s = keys(Radix.HEX, "FFFFFFFFFFFFFFFFF")
        assertEquals(listOf("#FFFFFFFFFFFFFFFF"), s.tokens)
        // 2 isn't a binary digit
        assertEquals(listOf("#101"), keys(Radix.BIN, "1021").tokens)
    }

    @Test fun divisionAndErrors() {
        assertEquals(3L, keys(Radix.DEC, "7 ÷ 2").finish().result)
        assertEquals(1L, keys(Radix.DEC, "7 mod 2").finish().result)
        assertEquals(ProgError.DIV_ZERO, keys(Radix.DEC, "7 ÷ 0").finish().error)
        assertEquals(ProgError.SHIFT, keys(Radix.DEC, "1 << 64").finish().error)
    }

    @Test fun notAndNegative() {
        assertEquals(-1L, keys(Radix.DEC, "NOT 0").finish().result)
        val neg = ProgState(radix = Radix.DEC).digit('5').negate().finish()
        assertEquals(-5L, neg.result)
        assertEquals("FFFF FFFF FFFF FFFB", Programmer.format(-5L, Radix.HEX))
        assertEquals("-5", Programmer.format(-5L, Radix.DEC))
        assertEquals("1010", Programmer.format(10L, Radix.BIN))
        assertEquals("1 0000", Programmer.format(16L, Radix.BIN))
    }

    @Test fun switchingBaseKeepsValues() {
        val s = keys(Radix.DEC, "255 + 1").withRadix(Radix.HEX)
        assertEquals(listOf("#FF", Programmer.PLUS, "#1"), s.tokens)
        assertEquals(256L, s.finish().result)
    }

    @Test fun backspaceAndPreview() {
        val s = keys(Radix.HEX, "AB").backspace()
        assertEquals(listOf("#A"), s.tokens)
        assertEquals(emptyList<String>(), s.backspace().tokens)
        assertNull(keys(Radix.DEC, "1 +").preview())
        assertEquals(3L, keys(Radix.DEC, "1 + 2").preview())
    }

    @Test fun continuesFromAResult() {
        val r = keys(Radix.DEC, "2 − 5").finish()
        assertEquals(-3L, r.result)
        assertEquals(-1L, r.operator(Programmer.PLUS).digit('2').finish().result)
    }
}
