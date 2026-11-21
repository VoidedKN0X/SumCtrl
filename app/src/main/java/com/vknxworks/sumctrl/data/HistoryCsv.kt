package com.vknxworks.sumctrl.data

import com.vknxworks.sumctrl.data.calc.Render
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The history as a CSV file (RFC 4180: comma-separated, fields with a comma, quote or line break in quotes,
 * CRLF line ends), for a spreadsheet. Numbers are written with a point and no grouping so every spreadsheet
 * reads them the same way; the sum is the text as SumCtrl shows it. Oldest first.
 */
object HistoryCsv {
    private val TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** [header] are the four column names, in the phone's language. */
    fun write(entries: List<HistoryEntry>, zone: ZoneId, header: List<String>): String {
        val plain = NumFmt('.', null, "EUR")
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { field(it) }).append("\r\n")
        entries.sortedBy { it.time }.forEach { e ->
            val time = TIME.format(Instant.ofEpochMilli(e.time).atZone(zone))
            // The sum keeps its real minus sign (−), so a spreadsheet never reads it as a formula.
            val sum = Render.tokens(e.tokens, plain)
            val result = e.result.toBigDecimalOrNull()?.let { exact(it) } ?: e.result
            sb.append(listOf(time, sum, result, e.note.orEmpty()).joinToString(",") { field(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /** The result as it was worked out, without exponent notation (rounded to 34 digits, like every result). */
    private fun exact(v: BigDecimal): String = if (v.signum() == 0) "0" else v.stripTrailingZeros().toPlainString()

    fun field(s: String): String {
        // A leading =, +, - or @ would make a spreadsheet run the cell as a formula; text gets a quote in front.
        val safe = if (s.isNotEmpty() && s[0] in "=+-@" && s.toBigDecimalOrNull() == null) "'$s" else s
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
