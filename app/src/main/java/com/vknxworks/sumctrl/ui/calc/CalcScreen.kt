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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
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
import com.vknxworks.sumctrl.data.calc.Render
import com.vknxworks.sumctrl.data.calc.RenderedLine
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
    var valuesOpen by rememberSaveable { mutableStateOf(false) }
    val stored by c.values.state.collectAsStateWithLifecycle()
    var inverse by rememberSaveable { mutableStateOf(false) }

    // The key handlers are kept across recompositions (they read the newest settings through this), so the
    // keypad below doesn't have to be rebuilt and redrawn on every key press.
    val currentSettings = rememberUpdatedState(settings)

    val press = remember<((CalcState) -> CalcState) -> Unit>(c, view) {
        { change ->
            if (currentSettings.value.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            // At the cursor when the sum was tapped to move it, otherwise at the end.
            c.calc.value = c.calc.value.edit(change)
        }
    }

    // AC clears the whole line, wherever the cursor is.
    val clearAll = remember<() -> Unit>(c, view) {
        {
            if (currentSettings.value.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            c.calc.value = c.calc.value.clear()
        }
    }

    val onMemory = remember<(MemoryKey) -> Unit>(c, view) {
        { key ->
            val now = currentSettings.value
            if (now.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            when (key) {
                MemoryKey.CLEAR -> c.values.clearMemory()
                MemoryKey.RECALL -> {
                    if (c.values.memory != null) c.calc.value = c.calc.value.edit { it.constant(Engine.MEMORY) }
                }
                MemoryKey.ADD, MemoryKey.SUBTRACT -> {
                    val v = c.calc.value.currentValue(now.degrees, c.values.lookup())
                    if (v != null) c.values.addToMemory(if (key == MemoryKey.ADD) v else v.negate())
                }
            }
            Unit
        }
    }

    val onEqualsPressed = remember<() -> Unit>(c, view) {
        {
            val now = currentSettings.value
            if (now.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val (next, result) = c.calc.value.equals(now.degrees, c.values.lookup())
            if (result != null && now.saveHistory) c.history.add(c.calc.value.tokens, result.toPlainString())
            c.calc.value = next
        }
    }

    fun setDegrees(on: Boolean) = c.appScope.launch { c.settings.update { it.copy(degrees = on) } }
    fun setScientific(on: Boolean) = c.appScope.launch { c.settings.update { it.copy(scientific = on) } }

    val values = remember(stored) { c.values.lookup(stored) }
    val preview = remember(state, settings.degrees, values) { state.preview(settings.degrees, values) }

    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        // Read here: the Column below is not a BoxWithConstraints scope.
        val boxWidth = maxWidth
        val boxHeight = maxHeight
        val landscape = boxWidth > boxHeight
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
                memory = stored.memory != null,
                onBackspace = { press { it.backspace() } },
                onClear = clearAll,
                onMoveCursor = { position ->
                    if (currentSettings.value.keyHaptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    c.calc.value = c.calc.value.moveCursor(position)
                },
                onCopy = { text -> copy(context, text) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            // Built once per number format; only the scientific rows change with INV.
            val memoryLabels = listOf(
                stringResource(R.string.memory_clear),
                stringResource(R.string.memory_recall),
                stringResource(R.string.memory_add),
                stringResource(R.string.memory_subtract),
                stringResource(R.string.values_title),
            )
            val keys = remember(fmt, press, onEqualsPressed, clearAll, onMemory, memoryLabels) {
                Keys(
                    memoryLabels = memoryLabels,
                    fmt = fmt,
                    onInverse = { inverse = !inverse },
                    press = press,
                    onEquals = onEqualsPressed,
                    onClearAll = clearAll,
                    onMemory = onMemory,
                    onValues = { valuesOpen = true },
                )
            }
            val scientificKeys = remember(keys, inverse) { keys.scientific(inverse) }

            if (landscape) {
                // Side by side: scientific keys on the left, the keypad on the right.
                val avail = (boxHeight - 60.dp - 90.dp - clearance - 12.dp).coerceAtLeast(150.dp)
                val keyH = ((avail - gap * 4) / 5).coerceIn(32.dp, 64.dp)
                Row(
                    Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, bottom = clearance + 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(gap * 2),
                ) {
                    // Four rows of scientific keys as tall as the five rows of the keypad.
                    KeyGrid(scientificKeys, keyH * 5 / 4 + gap / 4, gap, Modifier.weight(1f), small = true)
                    KeyGrid(keys.main, keyH, gap, Modifier.weight(1.2f))
                }
            } else {
                val keyW = (boxWidth - Gutter * 2 - gap * 3) / 4
                val sciRows = if (settings.scientific) 4 else 0
                val avail = boxHeight - 60.dp - 150.dp - clearance - 12.dp
                val units = 5f + sciRows * 0.62f
                val gaps = gap * (4 + sciRows)
                val keyH = min(keyW * 0.82f, (avail - gaps) / units).coerceAtLeast(40.dp)
                Column(Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, bottom = clearance + 12.dp)) {
                    AnimatedVisibility(
                        visible = settings.scientific,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        KeyGrid(scientificKeys, keyH * 0.62f, gap, Modifier.fillMaxWidth().padding(bottom = gap), small = true)
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
                c.calc.value = c.calc.value.edit { it.insert(v) }
                historyOpen = false
            },
        )
    }

    if (valuesOpen) {
        ValuesSheet(
            fmt = fmt,
            current = state.currentValue(settings.degrees, values),
            onDismiss = { valuesOpen = false },
            onPick = { name ->
                c.calc.value = c.calc.value.edit { it.constant(Engine.variableToken(name)) }
                valuesOpen = false
            },
        )
    }
}

enum class MemoryKey { CLEAR, RECALL, ADD, SUBTRACT }

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
    memory: Boolean,
    onToggleDegrees: () -> Unit,
    onToggleScientific: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val line: RenderedLine? = remember(state.tokens, state.evaluated, fmt) { if (state.evaluated) null else Render.line(state.tokens, fmt) }
    val main = line?.text ?: fmt.result(state.ans)
    val above = if (state.evaluated && state.previous.isNotEmpty()) renderTokens(state.previous, fmt) + " =" else null
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val cursorColor = CtrlColors.Accent
    val cursorLabel = stringResource(R.string.cursor_move)
    val memoryInUse = stringResource(R.string.memory_in_use)
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
            // Tap the sum to put the cursor there; the next key goes in at the cursor. Tapping a result makes it
            // editable again.
            Text(
                main.ifEmpty { "0" },
                style = CtrlType.Readout.copy(fontSize = size, lineHeight = size * 1.15f),
                color = if (main.isEmpty()) CtrlColors.Inactive else CtrlColors.Text,
                textAlign = TextAlign.End,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layout = it },
                modifier = Modifier
                    .semantics { contentDescription = main.ifEmpty { "0" } }
                    .pointerInput(line, state.evaluated, state.tokens.size) {
                        detectTapGestures(onTap = { pos ->
                            val l = layout
                            when {
                                main.isEmpty() -> Unit
                                line == null -> onMoveCursor(state.tokens.size)
                                l != null -> onMoveCursor(line.positionAt(l.getOffsetForPosition(pos)))
                                else -> Unit
                            }
                        })
                    }
                    .drawWithContent {
                        drawContent()
                        val l = layout
                        val at = state.cursor
                        if (line != null && at != null && l != null) {
                            val offset = line.offsetOf(at).coerceIn(0, l.layoutInput.text.length)
                            val r = l.getCursorRect(offset)
                            drawRect(cursorColor, Offset(r.left - 1.5.dp.toPx(), r.top), Size(3.dp.toPx(), r.height))
                        }
                    },
            )
            if (state.cursor != null) {
                Text(
                    cursorLabel,
                    style = CtrlType.Label,
                    color = CtrlColors.Muted,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
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
            if (memory) {
                Text(
                    "M",
                    style = CtrlType.LabelStrong,
                    color = CtrlColors.AccentText,
                    modifier = Modifier.semantics { contentDescription = memoryInUse },
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
    CalcError.UNKNOWN_VALUE -> R.string.err_unknown_value
}

/** A number for the clipboard: the rounded result with the chosen decimal separator, no grouping. */
private fun plain(v: BigDecimal, fmt: NumFmt): String =
    Engine.rounded(v).toPlainString().replace('.', fmt.decimal)

/** The typed keys as text: numbers grouped, operators spaced, inverse functions with ⁻¹, values by name. */
fun renderTokens(tokens: List<String>, fmt: NumFmt): String = Render.tokens(tokens, fmt)

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
    private val memoryLabels: List<String>,
    fmt: NumFmt,
    private val onInverse: () -> Unit,
    private val press: ((CalcState) -> CalcState) -> Unit,
    onEquals: () -> Unit,
    onClearAll: () -> Unit,
    private val onMemory: (MemoryKey) -> Unit,
    private val onValues: () -> Unit,
) {
    private fun num(d: String) = Key(d, KeyKind.NUMBER) { press { it.digit(d) } }
    private fun op(o: String) = Key(o, KeyKind.OPERATOR) { press { it.operator(o) } }

    val main: List<List<Key>> = listOf(
        listOf(
            Key("AC", KeyKind.CLEAR, onClick = onClearAll),
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
        // Memory and named values.
        listOf(
            Key("MC", KeyKind.FUNCTION, description = memoryLabels[0]) { onMemory(MemoryKey.CLEAR) },
            Key("MR", KeyKind.FUNCTION, description = memoryLabels[1]) { onMemory(MemoryKey.RECALL) },
            Key("M+", KeyKind.FUNCTION, description = memoryLabels[2]) { onMemory(MemoryKey.ADD) },
            Key("M−", KeyKind.FUNCTION, description = memoryLabels[3]) { onMemory(MemoryKey.SUBTRACT) },
            Key("x=", KeyKind.FUNCTION, description = memoryLabels[4], onClick = onValues),
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
