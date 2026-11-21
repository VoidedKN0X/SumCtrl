package com.vknxworks.sumctrl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.sumctrl.data.NumFmt
import com.vknxworks.sumctrl.data.calc.CalcError
import com.vknxworks.sumctrl.data.calc.CalcException
import com.vknxworks.sumctrl.data.calc.Engine
import com.vknxworks.sumctrl.data.calc.Render
import com.vknxworks.sumctrl.data.calc.TextExpr
import com.vknxworks.sumctrl.ui.lock.AppLock
import com.vknxworks.sumctrl.ui.theme.CtrlColors
import com.vknxworks.sumctrl.ui.theme.CtrlShapes
import com.vknxworks.sumctrl.ui.theme.CtrlType
import com.vknxworks.sumctrl.ui.theme.SumCtrlTheme
import java.math.BigDecimal

/**
 * "Calculate" in the text-selection menu of other apps: works out the selected sum and offers to put the
 * answer in place of the selection (when the app allows it) or to copy it. It reads only the selected text,
 * shows nothing of SumCtrl's history, memory or named values, and saves nothing.
 */
class CalculateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Opening this pop-up isn't coming back to SumCtrl, for the app lock's time away.
        AppLock.startSideTrip()
        super.onCreate(savedInstanceState)
        // Hidden from screenshots until the saved settings say otherwise, as in the main window.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setRecentsScreenshotEnabled(false)

        val c = container
        val text = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
        val readOnly = intent?.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) ?: true
        val tokens = TextExpr.parse(text)
        val outcome: Pair<BigDecimal?, CalcError?> = when (tokens) {
            null -> null to null
            else -> try {
                Engine.evaluate(tokens, degrees = true) to null
            } catch (e: CalcException) {
                null to e.error
            } catch (e: ArithmeticException) {
                null to CalcError.TOO_BIG
            }
        }
        val (value, error) = outcome
        // The answer goes back with the decimal separator the selection used, or the phone's.
        val decimal = TextExpr.decimalOf(text) ?: java.text.DecimalFormatSymbols.getInstance().decimalSeparator

        setContent {
            val loaded by c.settings.loaded.collectAsStateWithLifecycle()
            val settings by c.settings.settings.collectAsStateWithLifecycle()
            LaunchedEffect(loaded, settings.hideScreenshots) {
                if (loaded && !settings.hideScreenshots) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val fmt = remember(settings.decimalSeparator, settings.groupThousands, settings.currency) { NumFmt.from(settings) }
            SumCtrlTheme(isSystemInDarkTheme()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier
                            .padding(24.dp)
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .background(CtrlColors.Surface, CtrlShapes.Dialog)
                            .border(1.dp, CtrlColors.Outline, CtrlShapes.Dialog)
                            // Taps on the card itself don't close it.
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = CtrlColors.Text)
                        if (tokens == null) {
                            Text(stringResource(R.string.calculate_not_a_sum), style = MaterialTheme.typography.bodyMedium, color = CtrlColors.Muted)
                        } else {
                            Text(
                                Render.tokens(tokens, fmt) + " =",
                                style = CtrlType.Data.copy(fontSize = 16.sp, lineHeight = 22.sp),
                                color = CtrlColors.Muted,
                                textAlign = TextAlign.End,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                value?.let { fmt.result(it) } ?: stringResource(errorText(error ?: CalcError.SYNTAX)),
                                style = CtrlType.Readout.copy(fontSize = 34.sp, lineHeight = 40.sp),
                                color = if (value != null) CtrlColors.Text else CtrlColors.Error,
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { finish() }) { Text(stringResource(R.string.calculate_close), color = CtrlColors.Text) }
                            if (value != null) {
                                TextButton(onClick = {
                                    getSystemService(ClipboardManager::class.java)
                                        ?.setPrimaryClip(ClipData.newPlainText("SumCtrl", TextExpr.answer(value, decimal)))
                                    finish()
                                }) { Text(stringResource(R.string.calculate_copy)) }
                                if (!readOnly) {
                                    TextButton(onClick = {
                                        setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, TextExpr.answer(value, decimal)))
                                        finish()
                                    }) { Text(stringResource(R.string.calculate_replace)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun errorText(e: CalcError) = when (e) {
        CalcError.SYNTAX -> R.string.err_syntax
        CalcError.DIV_ZERO -> R.string.err_div_zero
        CalcError.DOMAIN -> R.string.err_domain
        CalcError.TOO_BIG -> R.string.err_too_big
        CalcError.UNKNOWN_VALUE -> R.string.err_unknown_value
    }
}
