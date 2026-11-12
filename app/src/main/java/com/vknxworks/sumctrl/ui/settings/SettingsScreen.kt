package com.vknxworks.sumctrl.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.container
import com.vknxworks.sumctrl.data.lock.LockVault
import com.vknxworks.sumctrl.data.settings.AppSettings
import com.vknxworks.sumctrl.data.settings.DecimalSeparator
import com.vknxworks.sumctrl.data.settings.LockTimeout
import com.vknxworks.sumctrl.ui.components.ConfirmDialog
import com.vknxworks.sumctrl.ui.components.Group
import com.vknxworks.sumctrl.ui.components.GroupChoice
import com.vknxworks.sumctrl.ui.components.GroupRow
import com.vknxworks.sumctrl.ui.components.GroupSwitch
import com.vknxworks.sumctrl.ui.components.ScreenTopBar
import com.vknxworks.sumctrl.ui.lock.AppLock
import com.vknxworks.sumctrl.ui.lock.PinRequest
import com.vknxworks.sumctrl.ui.lock.findActivity
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Settings, opened from the You page (tap the profile picture). SumCtrl follows the phone's dark theme
 * setting (Ctrl / Signal), so there are no theme settings.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    fun set(transform: (AppSettings) -> AppSettings) {
        scope.launch { c.settings.update(transform) }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.settings))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            CalculatorGroup(settings) { t -> set(t) }

            NumbersGroup(settings) { t -> set(t) }

            PrivacyAndSecurityGroup(settings)
        }
    }
}

/** History, key vibration and the angle unit. */
@Composable
private fun CalculatorGroup(settings: AppSettings, set: ((AppSettings) -> AppSettings) -> Unit) {
    val c = LocalContext.current.container
    val entries by c.history.entries.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    Group(stringResource(R.string.group_calculator), footer = stringResource(R.string.calculator_footer)) {
        GroupSwitch(
            stringResource(R.string.save_history),
            stringResource(R.string.save_history_sub),
            checked = settings.saveHistory,
            icon = Icons.Outlined.History,
        ) { on -> set { it.copy(saveHistory = on) } }
        GroupRow(
            stringResource(R.string.history_clear),
            pluralStringResource(R.plurals.history_count, entries.size, entries.size),
            icon = Icons.Outlined.DeleteSweep,
            onClick = if (entries.isNotEmpty()) ({ confirmClear = true }) else null,
        )
        GroupSwitch(
            stringResource(R.string.key_haptics),
            stringResource(R.string.key_haptics_sub),
            checked = settings.keyHaptics,
            icon = Icons.Outlined.Vibration,
        ) { on -> set { it.copy(keyHaptics = on) } }
        GroupChoice(
            stringResource(R.string.angle_unit),
            listOf(true to stringResource(R.string.angle_degrees), false to stringResource(R.string.angle_radians)),
            settings.degrees,
            icon = Icons.Outlined.WbTwilight,
        ) { d -> set { it.copy(degrees = d) } }
    }
    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear_title),
            body = stringResource(R.string.history_clear_body),
            confirm = stringResource(R.string.history_clear),
            onDismiss = { confirmClear = false },
            onConfirm = {
                c.history.clear()
                confirmClear = false
            },
        )
    }
}

/** How numbers look, and the money settings for the work tools. */
@Composable
private fun NumbersGroup(settings: AppSettings, set: ((AppSettings) -> AppSettings) -> Unit) {
    Group(stringResource(R.string.group_numbers), footer = stringResource(R.string.numbers_footer)) {
        GroupChoice(
            stringResource(R.string.decimal_separator),
            DecimalSeparator.entries.map { it to stringResource(it.label) },
            settings.decimalSeparator,
            icon = Icons.Outlined.Straighten,
        ) { d -> set { it.copy(decimalSeparator = d) } }
        GroupSwitch(
            stringResource(R.string.group_thousands),
            stringResource(R.string.group_thousands_sub),
            checked = settings.groupThousands,
            icon = Icons.Outlined.FormatListNumbered,
        ) { on -> set { it.copy(groupThousands = on) } }
        GroupChoice(
            stringResource(R.string.unit_currency),
            CURRENCIES.map { it to currencyLabel(it) },
            settings.currency,
            icon = Icons.Outlined.Payments,
        ) { cur -> set { it.copy(currency = cur) } }
        GroupChoice(
            stringResource(R.string.default_vat),
            listOf("21", "12", "6", "0", "20", "19", "10", "5").map { it to "$it %" },
            settings.vatRate,
            icon = Icons.Outlined.Percent,
            fallbackLabel = "${settings.vatRate} %",
        ) { r -> set { it.copy(vatRate = r) } }
    }
}

/** Common currencies; amounts are only labelled with it, never converted. */
private val CURRENCIES = listOf("EUR", "USD", "GBP", "CHF", "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "CAD", "AUD", "JPY")

private fun currencyLabel(code: String): String =
    runCatching { java.util.Currency.getInstance(code) }.getOrNull()
        ?.let { "${it.getDisplayName(Locale.getDefault())} (${it.getSymbol(Locale.getDefault())})" }
        ?: code

/** Privacy and security: the shared Ctrl series app lock, then hiding SumCtrl in screenshots. */
@Composable
private fun PrivacyAndSecurityGroup(settings: AppSettings) {
    val context = LocalContext.current
    val c = context.container
    val bioAvailable = remember { AppLock.biometricAvailable(context) }
    val lockOn = c.vault.enabled
    Group(stringResource(R.string.group_privacy), footer = stringResource(R.string.app_lock_footer)) {
        GroupSwitch(
            stringResource(R.string.app_lock),
            stringResource(R.string.app_lock_subtitle),
            checked = lockOn,
            icon = Icons.Outlined.Lock,
        ) { on -> AppLock.request = if (on) PinRequest.ENABLE else PinRequest.DISABLE }
        if (lockOn) {
            GroupRow(
                stringResource(R.string.lock_change_pin),
                stringResource(R.string.lock_change_pin_subtitle, LockVault.PIN_MIN),
                icon = Icons.Outlined.Password,
                showChevron = true,
                onClick = { AppLock.request = PinRequest.CHANGE },
            )
            GroupSwitch(
                stringResource(R.string.lock_fingerprint),
                stringResource(if (bioAvailable || c.vault.biometricEnabled) R.string.lock_fingerprint_subtitle else R.string.lock_fingerprint_unavailable),
                checked = c.vault.biometricEnabled,
                icon = Icons.Outlined.Fingerprint,
            ) { on ->
                if (!on) {
                    c.vault.disableBiometric()
                } else if (bioAvailable) {
                    val activity = context.findActivity()
                    val cipher = c.vault.biometricEnrolCipher()
                    if (activity == null || cipher == null) {
                        Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                    } else {
                        AppLock.biometricPrompt(
                            activity,
                            context.getString(R.string.lock_fingerprint_enable_title),
                            context.getString(R.string.lock_fingerprint_enable_subtitle),
                            cipher,
                        ) { unlocked ->
                            if (!c.vault.finishBiometricEnrol(unlocked)) {
                                Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            GroupChoice(
                stringResource(R.string.lock_again),
                LockTimeout.entries.map { it to stringResource(it.label) },
                settings.lockTimeout,
                icon = Icons.Outlined.Timer,
            ) { t -> c.appScope.launch { c.settings.update { it.copy(lockTimeout = t) } } }
        }
        GroupSwitch(
            stringResource(R.string.hide_screenshots),
            stringResource(R.string.hide_screenshots_subtitle),
            checked = settings.hideScreenshots,
            icon = Icons.Outlined.VisibilityOff,
        ) { on -> c.appScope.launch { c.settings.update { it.copy(hideScreenshots = on) } } }
        GroupSwitch(
            stringResource(R.string.hide_recents),
            stringResource(R.string.hide_recents_subtitle),
            checked = settings.hideRecents,
            icon = Icons.Outlined.Layers,
        ) { on -> c.appScope.launch { c.settings.update { it.copy(hideRecents = on) } } }
    }
}
