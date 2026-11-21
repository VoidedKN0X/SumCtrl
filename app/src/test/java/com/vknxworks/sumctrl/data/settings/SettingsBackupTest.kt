package com.vknxworks.sumctrl.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SettingsBackupTest {
    @Test fun appliesValidValuesOnly() {
        val cur = AppSettings(hideScreenshots = true, lockTimeout = LockTimeout.HOUR_1)
        val b = SettingsBackup(
            degrees = false,
            decimalSeparator = "COMMA",
            currency = "usd",
            vatRate = "6",
            fxFrom = "GBP",
            fxRate = "1.17",
            hideScreenshots = false,
            hideRecents = true,
        )
        val s = SettingsBackups.apply(b, cur)
        assertEquals(false, s.degrees)
        assertEquals(DecimalSeparator.COMMA, s.decimalSeparator)
        // Lower-case isn't a currency code: kept as it was.
        assertEquals("EUR", s.currency)
        assertEquals("6", s.vatRate)
        assertEquals("GBP", s.fxFrom)
        assertEquals("1.17", s.fxRate)
        // Only ever turned on, never off; the app lock's settings are never touched.
        assertTrue(s.hideScreenshots)
        assertTrue(s.hideRecents)
        assertEquals(LockTimeout.HOUR_1, s.lockTimeout)
    }

    @Test fun checksFormatAndVersion() {
        SettingsBackups.check(SettingsBackup())
        try {
            SettingsBackups.check(SettingsBackup(format = "clockctrl-settings"))
            fail()
        } catch (e: BackupFormatException) {
            assertEquals(false, e.newer)
        }
        try {
            SettingsBackups.check(SettingsBackup(version = 9))
            fail()
        } catch (e: BackupFormatException) {
            assertEquals(true, e.newer)
        }
    }
}
