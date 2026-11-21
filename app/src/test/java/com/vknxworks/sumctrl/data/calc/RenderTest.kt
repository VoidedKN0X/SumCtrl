package com.vknxworks.sumctrl.data.calc

import com.vknxworks.sumctrl.data.NumFmt
import org.junit.Assert.assertEquals
import org.junit.Test

class RenderTest {
    private val fmt = NumFmt(',', ' ', "EUR")

    @Test fun mapsKeysToCharacters() {
        val tokens = listOf("1", "2", "3", "4", Engine.PLUS, "5", Engine.POINT, "5")
        val line = Render.line(tokens, fmt)
        assertEquals("1 234 + 5,5", line.text)
        // "1" at 0, "2" after the group space at 2
        assertEquals(0, line.offsetOf(0))
        assertEquals(2, line.offsetOf(1))
        assertEquals(line.text.length, line.offsetOf(tokens.size))
        assertEquals(1, line.positionAt(2))
        assertEquals(tokens.size, line.positionAt(line.text.length))
    }

    @Test fun namedValuesShowTheirName() {
        assertEquals("2 × rate", Render.tokens(listOf("2", Engine.TIMES, "\$rate"), fmt))
        assertEquals("M", Render.tokens(listOf(Engine.MEMORY), fmt))
    }
}
