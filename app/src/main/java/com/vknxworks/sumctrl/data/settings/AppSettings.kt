package com.vknxworks.sumctrl.data.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vknxworks.sumctrl.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "sumctrl")

/** How long SumCtrl can be in the background before the app lock asks again (-1: only after a restart). */
enum class LockTimeout(@StringRes val label: Int, val millis: Long) {
    IMMEDIATELY(R.string.lock_again_immediately, 0L),
    MINUTE_1(R.string.lock_again_1m, 60_000L),
    MINUTES_5(R.string.lock_again_5m, 5 * 60_000L),
    MINUTES_15(R.string.lock_again_15m, 15 * 60_000L),
    HOUR_1(R.string.lock_again_1h, 60 * 60_000L),
    RESTART(R.string.lock_again_restart, -1L),
}

/** The decimal separator shown on the keypad and in results. Typing always accepts both. */
enum class DecimalSeparator(@StringRes val label: Int) {
    PHONE(R.string.sep_phone),
    COMMA(R.string.sep_comma),
    POINT(R.string.sep_point),
}

data class AppSettings(
    /** Trig functions in degrees (true) or radians. */
    val degrees: Boolean = true,
    /** The scientific keys are shown above the keypad. */
    val scientific: Boolean = false,
    /** A short vibration on every key. */
    val keyHaptics: Boolean = true,
    /** Finished sums are kept in History. */
    val saveHistory: Boolean = true,
    val decimalSeparator: DecimalSeparator = DecimalSeparator.PHONE,
    /** Thousands grouping in results: 1 234 567. */
    val groupThousands: Boolean = true,
    /** ISO 4217 currency for the work tools. */
    val currency: String = "EUR",
    /** VAT rate the VAT tool starts with, in percent. */
    val vatRate: String = "21",
    /** App lock: when it asks again after SumCtrl was in the background. */
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATELY,
    /** "Hide in screenshots": FLAG_SECURE on the app's windows (screenshots, recordings and the Recents preview). */
    val hideScreenshots: Boolean = false,
    /** "Hide in recent apps": the Recents view shows a blank card. Screenshots still work. */
    val hideRecents: Boolean = false,
)

class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    private object K {
        val degrees = booleanPreferencesKey("degrees")
        val scientific = booleanPreferencesKey("scientific")
        val keyHaptics = booleanPreferencesKey("key_haptics")
        val saveHistory = booleanPreferencesKey("save_history")
        val decimal = stringPreferencesKey("decimal_separator")
        val group = booleanPreferencesKey("group_thousands")
        val currency = stringPreferencesKey("currency")
        val vatRate = stringPreferencesKey("vat_rate")
        val lockTimeout = stringPreferencesKey("lock_timeout")
        val hideScreenshots = booleanPreferencesKey("hide_screenshots")
        val hideRecents = booleanPreferencesKey("hide_recents")
    }

    /** The saved settings; if the settings file can't be read, the defaults instead of a crash. */
    private val prefs: Flow<Preferences> = context.appDataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: StateFlow<AppSettings> = prefs
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    private fun Preferences.toSettings() = AppSettings(
        degrees = this[K.degrees] ?: true,
        scientific = this[K.scientific] ?: false,
        keyHaptics = this[K.keyHaptics] ?: true,
        saveHistory = this[K.saveHistory] ?: true,
        decimalSeparator = enumOr(this[K.decimal], DecimalSeparator.PHONE),
        groupThousands = this[K.group] ?: true,
        currency = this[K.currency]?.takeIf { it.length == 3 } ?: "EUR",
        vatRate = this[K.vatRate]?.takeIf { it.isNotBlank() } ?: "21",
        lockTimeout = enumOr(this[K.lockTimeout], LockTimeout.IMMEDIATELY),
        hideScreenshots = this[K.hideScreenshots] ?: false,
        hideRecents = this[K.hideRecents] ?: false,
    )

    /** Saves a change. If the phone's storage fails, the change is dropped instead of closing the app. */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        try {
            context.appDataStore.edit { p ->
                val s = transform(p.toSettings())
                p[K.degrees] = s.degrees
                p[K.scientific] = s.scientific
                p[K.keyHaptics] = s.keyHaptics
                p[K.saveHistory] = s.saveHistory
                p[K.decimal] = s.decimalSeparator.name
                p[K.group] = s.groupThousands
                p[K.currency] = s.currency
                p[K.vatRate] = s.vatRate
                p[K.lockTimeout] = s.lockTimeout.name
                p[K.hideScreenshots] = s.hideScreenshots
                p[K.hideRecents] = s.hideRecents
            }
        } catch (e: IOException) {
            // Nothing to do: the settings stay as they were.
        }
    }
}

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
