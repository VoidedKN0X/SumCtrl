package com.vknxworks.sumctrl.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class HistoryCsvTest {
    @Test fun writesOldestFirstWithQuoting() {
        val entries = listOf(
            HistoryEntry(2, 60_000, listOf("1", "2", "3", "4", "+", "1"), "1235", note = "shop, \"Krëfel\""),
            HistoryEntry(1, 0, listOf("−", "2", "×", "3"), "-6"),
        )
        val csv = HistoryCsv.write(entries, ZoneOffset.UTC, listOf("Time", "Sum", "Result", "Note"))
        val lines = csv.split("\r\n")
        assertEquals("Time,Sum,Result,Note", lines[0])
        assertEquals("1970-01-01 00:00:00,−2 × 3,-6,", lines[1])
        assertEquals("1970-01-01 00:01:00,1234 + 1,1235,\"shop, \"\"Krëfel\"\"\"", lines[2])
    }

    @Test fun formulasAreNotRun() {
        assertEquals("'=HYPERLINK(1)", HistoryCsv.field("=HYPERLINK(1)"))
        assertEquals("-6", HistoryCsv.field("-6"))
    }
}
