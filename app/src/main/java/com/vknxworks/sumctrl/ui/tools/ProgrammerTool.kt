package com.vknxworks.sumctrl.ui.tools

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.container
import com.vknxworks.sumctrl.data.calc.ProgError
import com.vknxworks.sumctrl.data.calc.ProgState
import com.vknxworks.sumctrl.data.calc.Programmer
import com.vknxworks.sumctrl.data.calc.Radix
import com.vknxworks.sumctrl.ui.components.ScreenTopBar
import com.vknxworks.sumctrl.ui.components.topDivider
import com.vknxworks.sumctrl.ui.theme.CtrlColors
import com.vknxworks.sumctrl.ui.theme.CtrlType
import com.vknxworks.sumctrl.ui.theme.Gutter

/** Keeps the programmer line across rotation: the tokens, the base and the result. */
private val ProgSaver = Saver<ProgState, List<Any?>>(
    save = { listOf(it.tokens.joinToString("\u0001"), it.radix.name, it.evaluated, it.result, it.error?.name) },
    restore = { l ->
        ProgState(
            tokens = (l[0] as String).split("\u0001").filter { it.isNotEmpty() },
            radix = Radix.valueOf(l[1] as String),
            evaluated = l[2] as Boolean,
            result = l[3] as Long?,
            error = (l[4] as String?)?.let { ProgError.valueOf(it) },
        )
    },
)

/**
 * Programmer mode: whole numbers in 64 bits, typed in hexadecimal, decimal, octal or binary, with the value
 * shown in all four at once. Tap a base to type in it.
 */
@Composable
fun ProgrammerTool() {
    val c = LocalContext.current.container
    val view = LocalView.current
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    var state by rememberSaveable(stateSaver = ProgSaver) { mutableStateOf(ProgState()) }

    fun press(change: (ProgState) -> ProgState) {
        if (settings.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        state = change(state)
    }

    val value = state.preview()
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.tool_programmer))
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().padding(start = Gutter, end = Gutter, bottom = 16.dp),
        ) {
            // The line as typed, and the error if "=" failed.
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End,
            ) {
                val line = render(state.tokens)
                Text(
                    if (state.evaluated) "$line =" else line.ifEmpty { "0" },
                    style = CtrlType.Readout.copy(fontSize = 26.sp, lineHeight = 32.sp),
                    color = if (state.evaluated || line.isEmpty()) CtrlColors.Muted else CtrlColors.Text,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                state.error?.let { e ->
                    Text(
                        stringResource(
                            when (e) {
                                ProgError.SYNTAX -> R.string.err_syntax
                                ProgError.DIV_ZERO -> R.string.err_div_zero
                                ProgError.SHIFT -> R.string.prog_err_shift
                            },
                        ),
                        style = CtrlType.Data,
                        color = CtrlColors.Error,
                    )
                }
            }
            // The value in every base; the one you type in is highlighted.
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Radix.entries.forEach { r ->
                    val on = r == state.radix
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .topDivider()
                            .clickable(onClickLabel = stringResource(R.string.prog_type_in, r.name)) { press { it.withRadix(r) } }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            r.name,
                            style = CtrlType.LabelStrong,
                            color = if (on) CtrlColors.AccentText else CtrlColors.Muted,
                            modifier = Modifier.width(48.dp),
                        )
                        Text(
                            value?.let { Programmer.format(it, r) } ?: "—",
                            style = CtrlType.Data.copy(fontSize = if (r == Radix.BIN) 13.sp else 17.sp),
                            color = if (on) CtrlColors.Text else CtrlColors.Muted,
                            textAlign = TextAlign.End,
                            maxLines = 3,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            ProgKeys(state.radix, ::press)
        }
    }
}

/** The line: numbers as typed, operators spaced. */
private fun render(tokens: List<String>): String = buildString {
    tokens.forEachIndexed { i, t ->
        val prev = tokens.getOrNull(i - 1)
        when {
            Programmer.isNumber(t) -> append(Programmer.digitsOf(t))
            t == Programmer.MINUS && (prev == null || prev in Programmer.BINARY || prev == Programmer.OPEN || prev == Programmer.NOT) -> append("−")
            t in Programmer.BINARY -> append(" $t ")
            t == Programmer.NOT -> append("NOT ")
            else -> append(t)
        }
    }
}

private class PKey(val label: String, val weight: Float = 1f, val enabled: Boolean = true, val accent: Boolean = false, val description: String? = null, val onClick: () -> Unit)

@Composable
private fun ProgKeys(radix: Radix, press: ((ProgState) -> ProgState) -> Unit) {
    fun digit(d: Char) = PKey(d.toString(), enabled = Programmer.digitAllowed(d, radix)) { press { it.digit(d) } }
    fun op(o: String) = PKey(o) { press { it.operator(o) } }
    val backspace = stringResource(R.string.key_backspace)
    val rows = listOf(
        "ABCDEF".map { digit(it) },
        listOf(op(Programmer.AND), op(Programmer.OR), op(Programmer.XOR), PKey(Programmer.NOT) { press { it.not() } }, op(Programmer.SHL), op(Programmer.SHR)),
        listOf(digit('7'), digit('8'), digit('9'), PKey("( )") { press { it.bracket() } }, op(Programmer.MOD), op(Programmer.DIVIDE)),
        listOf(digit('4'), digit('5'), digit('6'), PKey("±") { press { it.negate() } }, PKey("AC") { press { it.clear() } }, op(Programmer.TIMES)),
        listOf(digit('1'), digit('2'), digit('3'), PKey("⌫", description = backspace) { press { it.backspace() } }, op(Programmer.MINUS)),
        listOf(digit('0'), PKey("=", weight = 3f, accent = true) { press { it.finish() } }, op(Programmer.PLUS)),
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .weight(k.weight)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(if (k.accent) CtrlColors.Accent else CtrlColors.Surface)
                            .border(1.dp, if (k.accent) CtrlColors.Accent else CtrlColors.ControlOutline, shape)
                            .clickable(enabled = k.enabled, onClick = k.onClick)
                            .semantics { contentDescription = k.description ?: k.label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            k.label,
                            style = CtrlType.Key.copy(fontSize = 17.sp, lineHeight = 20.sp),
                            color = when {
                                k.accent -> CtrlColors.OnAccent
                                !k.enabled -> CtrlColors.Inactive
                                else -> CtrlColors.Text
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
