package com.vknxworks.sumctrl.data.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A SumCtrl settings file: how the calculator and numbers look and the work tools' starting values. Never the
 * app lock, its PIN or "Lock again", and never the history, memory or named values. Every field is optional,
 * so files from older or newer versions restore what they can.
 */
@Serializable
data class SettingsBackup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val appVersion: String? = null,
    val createdAt: String? = null,
    val degrees: Boolean? = null,
    val scientific: Boolean? = null,
    val keyHaptics: Boolean? = null,
    val saveHistory: Boolean? = null,
    val decimalSeparator: String? = null,
    val groupThousands: Boolean? = null,
    val currency: String? = null,
    val vatRate: String? = null,
    val fxFrom: String? = null,
    val fxTo: String? = null,
    val fxRate: String? = null,
    /** Restoring can only turn these on, never off. */
    val hideScreenshots: Boolean? = null,
    val hideRecents: Boolean? = null,
) {
    companion object {
        const val FORMAT = "sumctrl-settings"
        const val VERSION = 1
    }
}

class BackupFormatException(val newer: Boolean) : Exception()

object SettingsBackups {
    const val MAX_BYTES = 256 * 1024

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun from(s: AppSettings, appVersion: String, now: String) = SettingsBackup(
        appVersion = appVersion,
        createdAt = now,
        degrees = s.degrees,
        scientific = s.scientific,
        keyHaptics = s.keyHaptics,
        saveHistory = s.saveHistory,
        decimalSeparator = s.decimalSeparator.name,
        groupThousands = s.groupThousands,
        currency = s.currency,
        vatRate = s.vatRate,
        fxFrom = s.fxFrom,
        fxTo = s.fxTo,
        fxRate = s.fxRate,
        hideScreenshots = s.hideScreenshots,
        hideRecents = s.hideRecents,
    )

    fun export(b: SettingsBackup): String = json.encodeToString(SettingsBackup.serializer(), b)

    fun parse(text: String): SettingsBackup {
        val b = runCatching { json.decodeFromString(SettingsBackup.serializer(), text) }.getOrNull()
            ?: throw BackupFormatException(newer = false)
        return check(b)
    }

    fun check(b: SettingsBackup): SettingsBackup {
        if (b.format != SettingsBackup.FORMAT) throw BackupFormatException(newer = false)
        if (b.version > SettingsBackup.VERSION) throw BackupFormatException(newer = true)
        return b
    }

    private fun currency(c: String?): String? = c?.takeIf { it.length == 3 && it.all { ch -> ch in 'A'..'Z' } }

    private fun number(s: String?, max: Int): String? = s?.takeIf { it.length <= max && it.all { ch -> ch.isDigit() || ch == '.' || ch == ',' } }

    /** [b] applied to [cur]: anything missing or out of range stays as it is; the hide switches only turn on. */
    fun apply(b: SettingsBackup, cur: AppSettings): AppSettings = cur.copy(
        degrees = b.degrees ?: cur.degrees,
        scientific = b.scientific ?: cur.scientific,
        keyHaptics = b.keyHaptics ?: cur.keyHaptics,
        saveHistory = b.saveHistory ?: cur.saveHistory,
        decimalSeparator = b.decimalSeparator?.let { n -> DecimalSeparator.entries.firstOrNull { it.name == n } } ?: cur.decimalSeparator,
        groupThousands = b.groupThousands ?: cur.groupThousands,
        currency = currency(b.currency) ?: cur.currency,
        vatRate = number(b.vatRate, 8)?.takeIf { it.isNotEmpty() } ?: cur.vatRate,
        fxFrom = currency(b.fxFrom) ?: cur.fxFrom,
        fxTo = currency(b.fxTo) ?: cur.fxTo,
        fxRate = number(b.fxRate, 24) ?: cur.fxRate,
        hideScreenshots = cur.hideScreenshots || b.hideScreenshots == true,
        hideRecents = cur.hideRecents || b.hideRecents == true,
    )
}
