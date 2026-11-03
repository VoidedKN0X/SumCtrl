package com.vknxworks.sumctrl.ui.calc

import android.content.ClipData
import android.content.ClipboardManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.container
import com.vknxworks.sumctrl.data.NumFmt
import com.vknxworks.sumctrl.data.calc.CalcError
import com.vknxworks.sumctrl.data.calc.CalcState
import com.vknxworks.sumctrl.data.calc.Engine
import com.vknxworks.sumctrl.ui.components.CtrlSquareButton
import com.vknxworks.sumctrl.ui.components.CtrlWordmark
import com.vknxworks.sumctrl.ui.components.LocalBottomClearance
import com.vknxworks.sumctrl.ui.components.ProfileButton
import com.vknxworks.sumctrl.ui.components.rememberNumFmt
import com.vknxworks.sumctrl.ui.theme.CtrlColors
import com.vknxworks.sumctrl.ui.theme.CtrlShapes
import com.vknxworks.sumctrl.ui.theme.CtrlType
import com.vknxworks.sumctrl.ui.theme.Gutter
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * The calculator: the sum and its live result at the top, the scientific keys (folded away by default) and
 * the keypad at the bottom. Finished sums go to History.
 */
@Composable
fun CalcScreen() {
    val context = LocalContext.current
    val c = context.container
    val view = LocalView.current
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val state by c.calc.collectAsStateWithLifecycle()
    val fmt = rememberNumFmt()
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var inverse by rememberSaveable { mutableStateOf(false) }

    fun press(change: (CalcState) -> CalcState) {
        if (settings.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        c.calc.value = change(c.calc.value)
    }

    fun onEqualsPressed() {
        if (settings.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val (next, result) = c.calc.value.equals(settings.degrees)
        if (result != null && settings.saveHistory) c.history.add(c.calc.value.tokens, result.toPlainString())
        c.calc.value = next
    }

    fun setDegrees(on: Boolean) = c.appScope.launch { c.settings.update { it.copy(degrees = on) } }
    fun setScientific(on: Boolean) = c.appScope.launch { c.settings.update { it.copy(scientific = on) } }

    val preview = remember(state, settings.degrees) { state.preview(settings.degrees) }

    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val landscape = maxWidth > maxHeight
        val clearance = LocalBottomClearance.current
        val gap = 8.dp

        Column(Modifier.fillMaxSize()) {
            // Wordmark, History and the profile picture.
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = Gutter, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CtrlWordmark("Sum", Modifier.weight(1f))
                CtrlSquareButton(Icons.Outlined.History, stringResource(R.string.history), onClick = { historyOpen = true })
                Spacer(Modifier.width(10.dp))
                ProfileButton()
            }

            Display(
                state = state,
                preview = preview,
                fmt = fmt,
                degrees = settings.degrees,
                scientific = settings.scientific,
                showSciToggle = !landscape,
                onToggleDegrees = { setDegrees(!settings.degrees) },
                onToggleScientific = { setScientific(!settings.scientific) },
                onBackspace = { press { it.backspace() } },
                onClear = { press { it.clear() } },
                onCopy = { text -> copy(context, text) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            val keys = Keys(
                fmt = fmt,
                inverse = inverse,
                onInverse = { inverse = !inverse },
                press = ::press,
                onEquals = ::onEqualsPressed,
            )

            if (landscape) {
                // Side by side: scientific keys on the left, the keypad on the right.
                val avail = (maxHeight - 60.dp - 90.dp - clearance - 12.dp).coerceAtLeast(150.dp)
                val keyH = ((avail - gap * 4) / 5).coerceIn(32.dp, 64.dp)
                Row(
                    Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, bottom = clearance + 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(gap * 2),
                ) {
                    KeyGrid(keys.scientific(inverse), keyH * 5 / 3 + gap * 2 / 3, gap, Modifier.weight(1f), small = true)
                    KeyGrid(keys.main, keyH, gap, Modifier.weight(1.2f))
                }
            } else {
                val keyW = (maxWidth - Gutter * 2 - gap * 3) / 4
                val sciRows = if (settings.scientific) 3 else 0
                val avail = maxHeight - 60.dp - 150.dp - clearance - 12.dp
                val units = 5f + sciRows * 0.62f
                val gaps = gap * (4 + sciRows)
                val keyH = min(keyW * 0.82f, (avail - gaps) / units).coerceAtLeast(40.dp)
                Column(Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, bottom = clearance + 12.dp)) {
                    AnimatedVisibility(
                        visible = settings.scientific,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        KeyGrid(keys.scientific(inverse), keyH * 0.62f, gap, Modifier.fillMaxWidth().padding(bottom = gap), small = true)
                    }
                    KeyGrid(keys.main, keyH, gap, Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (historyOpen) {
        HistorySheet(
            fmt = fmt,
            enabled = settings.saveHistory,
            onDismiss = { historyOpen = false },
            onPick = { v ->
                c.calc.value = c.calc.value.insert(v)
                historyOpen = false
            },
        )
    }
}

private fun copy(context: android.content.Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return
    // Android shows its own "Copied" confirmation.
    cm.setPrimaryClip(ClipData.newPlainText("SumCtrl", text))
}

// ---------- Display ----------

@Composable
private fun Display(
    state: CalcState,
    preview: BigDecimal?,
    fmt: NumFmt,
    degrees: Boolean,
    scientific: Boolean,
    showSciToggle: Boolean,
    onToggleDegrees: () -> Unit,
    onToggleScientific: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val main = if (state.evaluated) fmt.result(state.ans) else renderTokens(state.tokens, fmt)
    val above = if (state.evaluated && state.previous.isNotEmpty()) renderTokens(state.previous, fmt) + " =" else null
    val copyText = when {
        state.evaluated -> plain(state.ans, fmt)
        preview != null -> plain(preview, fmt)
        else -> null
    }
    val copiedLabel = stringResource(R.string.copy_result)
    Column(
        modifier.padding(horizontal = Gutter),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(CtrlShapes.Card)
                .combinedClickable(
                    onClick = {},
                    onLongClick = copyText?.let { t -> { onCopy(t) } },
                    onLongClickLabel = copiedLabel,
                )
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            if (above != null) {
                Text(
                    above,
                    style = CtrlType.Data.copy(fontSize = 18.sp, lineHeight = 24.sp),
                    color = CtrlColors.Muted,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
            val size = when {
                main.length <= 9 -> 56.sp
                main.length <= 12 -> 46.sp
                main.length <= 16 -> 36.sp
                main.length <= 24 -> 30.sp
                else -> 26.sp
            }
            Text(
                main.ifEmpty { "0" },
                style = CtrlType.Readout.copy(fontSize = size, lineHeight = size * 1.15f),
                color = if (main.isEmpty()) CtrlColors.Inactive else CtrlColors.Text,
                textAlign = TextAlign.End,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { contentDescription = main.ifEmpty { "0" } },
            )
            val error = state.error
            val below = when {
                error != null -> stringResource(errorText(error))
                preview != null -> "= " + fmt.result(preview)
                else -> ""
            }
            Text(
                below,
                style = CtrlType.Readout.copy(fontSize = 24.sp, lineHeight = 30.sp),
                color = if (error != null) CtrlColors.Error else CtrlColors.Muted,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // DEG/RAD, the scientific keys and backspace.
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeChip(stringResource(if (degrees) R.string.mode_deg else R.string.mode_rad), stringResource(R.string.angle_unit_toggle), onToggleDegrees)
            if (showSciToggle) {
                ModeChip(
                    stringResource(R.string.mode_sci),
                    stringResource(if (scientific) R.string.sci_hide else R.string.sci_show),
                    onToggleScientific,
                    icon = if (scientific) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                    active = scientific,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CtrlShapes.Control)
                    .combinedClickable(onClick = onBackspace, onLongClick = onClear, onLongClickLabel = stringResource(R.string.key_clear)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.key_backspace), tint = CtrlColors.AccentText)
            }
        }
    }
}

@Composable
private fun ModeChip(text: String, description: String, onClick: () -> Unit, icon: ImageVector? = null, active: Boolean = false) {
    Row(
        Modifier
            .heightIn(min = 36.dp)
            .clip(CtrlShapes.Chip)
            .background(if (active) CtrlColors.Raised else Color.Transparent)
            .border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Chip)
            .combinedClickable(onClick = onClick, onClickLabel = description)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = CtrlType.LabelStrong, color = CtrlColors.Text)
        if (icon != null) {
            Spacer(Modifier.width(4.dp))
            Icon(icon, null, Modifier.size(16.dp), tint = CtrlColors.Muted)
        }
    }
}

private fun errorText(e: CalcError) = when (e) {
    CalcError.SYNTAX -> R.string.err_syntax
    CalcError.DIV_ZERO -> R.string.err_div_zero
    CalcError.DOMAIN -> R.string.err_domain
    CalcError.TOO_BIG -> R.string.err_too_big
}

/** A number for the clipboard: the rounded result with the chosen decimal separator, no grouping. */
private fun plain(v: BigDecimal, fmt: NumFmt): String =
    Engine.rounded(v).toPlainString().replace('.', fmt.decimal)

/** The typed keys as text: numbers grouped, operators spaced, inverse functions with ⁻¹. */
fun renderTokens(tokens: List<String>, fmt: NumFmt): String {
    val sb = StringBuilder()
    var i = 0
    while (i < tokens.size) {
        val t = tokens[i]
        if (Engine.isNumberPart(t)) {
            val run = StringBuilder()
            while (i < tokens.size && Engine.isNumberPart(tokens[i])) run.append(tokens[i++])
            sb.append(fmt.grouped(run.toString()))
            continue
        }
        val prev = tokens.getOrNull(i - 1)
        val unary = t == Engine.MINUS && (prev == null || prev in Engine.BINARY || prev == Engine.OPEN || Engine.isFunction(prev))
        sb.append(
            when {
                t == Engine.POWER -> "^"
                t in Engine.BINARY && !unary -> " $t "
                t == "asin(" -> "sin⁻¹("
                t == "acos(" -> "cos⁻¹("
                t == "atan(" -> "tan⁻¹("
                else -> t
            },
        )
        i++
    }
    return sb.toString()
}

// ---------- Keys ----------

private enum class KeyKind { NUMBER, OPERATOR, FUNCTION, EQUALS, CLEAR }

private class Key(
    val label: String,
    val kind: KeyKind,
    val description: String? = null,
    val active: Boolean = false,
    val onClick: () -> Unit,
)

/** Every key, wired to the calculator. */
private class Keys(
    fmt: NumFmt,
    private val inverse: Boolean,
    private val onInverse: () -> Unit,
    private val press: ((CalcState) -> CalcState) -> Unit,
    onEquals: () -> Unit,
) {
    private fun num(d: String) = Key(d, KeyKind.NUMBER) { press { it.digit(d) } }
    private fun op(o: String) = Key(o, KeyKind.OPERATOR) { press { it.operator(o) } }

    val main: List<List<Key>> = listOf(
        listOf(
            Key("AC", KeyKind.CLEAR) { press { it.clear() } },
            Key("( )", KeyKind.OPERATOR) { press { it.bracket() } },
            Key("%", KeyKind.OPERATOR) { press { it.postfix(Engine.PERCENT) } },
            op(Engine.DIVIDE),
        ),
        listOf(num("7"), num("8"), num("9"), op(Engine.TIMES)),
        listOf(num("4"), num("5"), num("6"), op(Engine.MINUS)),
        listOf(num("1"), num("2"), num("3"), op(Engine.PLUS)),
        listOf(
            Key("±", KeyKind.NUMBER) { press { it.negate() } },
            num("0"),
            Key(fmt.decimal.toString(), KeyKind.NUMBER) { press { it.point() } },
            Key("=", KeyKind.EQUALS, onClick = onEquals),
        ),
    )

    private fun fn(label: String, token: String) = Key(label, KeyKind.FUNCTION) { press { it.function(token) } }

    /** x², x³ and xʸ only follow a number (or a result). */
    private fun raise(label: String, exponent: String?) = Key(label, KeyKind.FUNCTION) {
        press { s ->
            if (!s.evaluated && !Engine.endsOperand(s.tokens.lastOrNull())) {
                s
            } else {
                val withPower = s.operator(Engine.POWER)
                if (exponent == null) withPower else withPower.digit(exponent)
            }
        }
    }

    fun scientific(inv: Boolean): List<List<Key>> = listOf(
        listOf(
            Key("INV", KeyKind.FUNCTION, active = inv, onClick = onInverse),
            if (inv) fn("sin⁻¹", "asin(") else fn("sin", "sin("),
            if (inv) fn("cos⁻¹", "acos(") else fn("cos", "cos("),
            if (inv) fn("tan⁻¹", "atan(") else fn("tan", "tan("),
            Key("π", KeyKind.FUNCTION) { press { it.constant(Engine.PI) } },
        ),
        listOf(
            if (inv) {
                Key("eˣ", KeyKind.FUNCTION) { press { it.constant(Engine.E).operator(Engine.POWER).open() } }
            } else {
                fn("ln", "ln(")
            },
            if (inv) {
                Key("10ˣ", KeyKind.FUNCTION) { press { s -> powerOfTen(s) } }
            } else {
                fn("log", "log(")
            },
            fn("√", "√("),
            if (inv) raise("x³", "3") else raise("x²", "2"),
            raise("xʸ", null),
        ),
        listOf(
            Key("(", KeyKind.FUNCTION) { press { it.open() } },
            Key(")", KeyKind.FUNCTION) { press { it.close() } },
            Key("!", KeyKind.FUNCTION) { press { it.postfix(Engine.FACTORIAL) } },
            Key("e", KeyKind.FUNCTION) { press { it.constant(Engine.E) } },
            Key("Ans", KeyKind.FUNCTION) { press { it.constant(Engine.ANS) } },
        ),
    )

    /** 10^( — a new "10" even right after another number, so it reads as 5×10^(…). */
    private fun powerOfTen(s: CalcState): CalcState {
        val start = if (!s.evaluated && Engine.endsOperand(s.tokens.lastOrNull())) s.operator(Engine.TIMES) else s
        return start.digit("1").digit("0").operator(Engine.POWER).open()
    }
}

@Composable
private fun KeyGrid(rows: List<List<Key>>, keyHeight: Dp, gap: Dp, modifier: Modifier = Modifier, small: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().height(keyHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { key -> KeyButton(key, small, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
private fun KeyButton(key: Key, small: Boolean, modifier: Modifier) {
    val shape: Shape = if (small) CtrlShapes.Control else RoundedCornerShape(20.dp)
    val (bg, fg) = when {
        key.kind == KeyKind.EQUALS -> CtrlColors.Accent to CtrlColors.OnAccent
        key.active -> CtrlColors.Accent to CtrlColors.OnAccent
        key.kind == KeyKind.OPERATOR -> CtrlColors.Raised to CtrlColors.AccentText
        key.kind == KeyKind.CLEAR -> CtrlColors.Raised to CtrlColors.Error
        key.kind == KeyKind.FUNCTION -> CtrlColors.Background to CtrlColors.Text
        else -> CtrlColors.Surface to CtrlColors.Text
    }
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .then(if (key.kind == KeyKind.FUNCTION && !key.active) Modifier.border(1.dp, CtrlColors.ControlOutline, shape) else Modifier)
            .combinedClickable(onClick = key.onClick)
            .semantics { contentDescription = key.description ?: key.label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            key.label,
            style = if (small) CtrlType.Key.copy(fontSize = 17.sp, lineHeight = 20.sp) else CtrlType.Key,
            color = fg,
            maxLines = 1,
        )
    }
}
