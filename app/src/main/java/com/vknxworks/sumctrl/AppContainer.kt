package com.vknxworks.sumctrl

import android.content.Context
import com.vknxworks.sumctrl.data.HistoryRepository
import com.vknxworks.sumctrl.data.ValuesRepository
import com.vknxworks.sumctrl.data.calc.CalcState
import com.vknxworks.sumctrl.data.crypto.DataKeyStore
import com.vknxworks.sumctrl.data.crypto.KeystoreWrapper
import com.vknxworks.sumctrl.data.lock.LockVault
import com.vknxworks.sumctrl.data.settings.AppSettings
import com.vknxworks.sumctrl.data.settings.SettingsRepository
import com.vknxworks.sumctrl.ui.lock.AppLock
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/** Manual dependency container; one per process. */
class AppContainer(val context: Context) {
    /**
     * Background work that outlives a screen (saving history and settings). A failure there (for example a
     * full disk) must never close the app, so it's dropped here instead of crashing.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> })

    val settings = SettingsRepository(context, appScope)
    /**
     * The key the history file is encrypted with at rest. Nothing is asked of the phone's secure hardware here:
     * that happens on a background thread, the first time the history is read or saved.
     */
    private val dataKeys = DataKeyStore(context.filesDir, KeystoreWrapper("sumctrl_data_at_rest"))
    val history = HistoryRepository(context, appScope, dataKeys)

    /** The memory and the named values, encrypted at rest with the same key as the history. */
    val values = ValuesRepository(context, appScope, dataKeys)

    /** The calculator's line. Kept here so it survives switching tabs and rotating the phone. */
    val calc = MutableStateFlow(CalcState())

    /** The app lock's PIN and fingerprint key (the shared Ctrl series lock). */
    val vault = LockVault(context, "sumctrl")

    /** Turns the app lock on with [pin]. */
    suspend fun enableLock(pin: String) {
        // Unlocked first, so the lock screen doesn't flash up while the PIN is being saved.
        AppLock.markUnlocked()
        withContext(Dispatchers.Default) { vault.setUp(pin) }
    }

    /** Turns the app lock off (after the PIN was confirmed). */
    fun disableLock() {
        vault.disable()
    }

    /**
     * "Forgot PIN?": the PIN can't be recovered, and skipping it would make the lock pointless, so the history,
     * the memory and named values, the current sum and every setting are deleted and the lock turned off.
     */
    suspend fun forgetEverything() {
        history.clear()
        values.clear()
        calc.value = CalcState()
        settings.update { AppSettings() }
        vault.disable()
        AppLock.markUnlocked()
    }

    fun onLocked() {
        vault.close()
    }

    fun onUnlocked() {
        AppLock.markUnlocked()
    }

    init {
        AppLock.onLocked = { onLocked() }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as SumCtrlApp).container
