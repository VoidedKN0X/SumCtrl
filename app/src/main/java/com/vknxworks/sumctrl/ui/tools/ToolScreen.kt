package com.vknxworks.sumctrl.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.container
import com.vknxworks.sumctrl.data.NumFmt
import com.vknxworks.sumctrl.data.tools.WorkMath
import com.vknxworks.sumctrl.ui.components.CtrlCard
import com.vknxworks.sumctrl.ui.components.FieldNote
import com.vknxworks.sumctrl.ui.components.NumberInput
import com.vknxworks.sumctrl.ui.components.ScreenTopBar
import com.vknxworks.sumctrl.ui.components.SectionLabel
import com.vknxworks.sumctrl.ui.components.rememberNumFmt
import com.vknxworks.sumctrl.ui.components.topDivider
import com.vknxworks.sumctrl.ui.theme.CtrlColors
import com.vknxworks.sumctrl.ui.theme.CtrlShapes
import com.vknxworks.sumctrl.ui.theme.CtrlType
import com.vknxworks.sumctrl.ui.theme.Gutter
import java.math.BigDecimal

private fun num(s: String): BigDecimal? = NumFmt.parse(s)

@Composable
fun ToolScreen(id: String) {
    val tool = Tool.of(id)
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(tool.title))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = Gutter, end = Gutter, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val fmt = rememberNumFmt()
            when (tool) {
                Tool.VAT -> VatTool(fmt)
                Tool.DISCOUNT -> DiscountTool(fmt)
                Tool.MARGIN -> MarginTool(fmt)
                Tool.INSTALMENTS -> InstalmentsTool(fmt)
                Tool.PERCENT -> PercentTool(fmt)
            }
        }
    }
}

// ---------- VAT ----------

@Composable
private fun ColumnScope.VatTool(fmt: NumFmt) {
    val settings by LocalContext.current.container.settings.settings.collectAsStateWithLifecycle()
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf(settings.vatRate) }
    var includes by rememberSaveable { mutableStateOf(false) }

    Chips(
        listOf(false to stringResource(R.string.vat_add), true to stringResource(R.string.vat_remove)),
        includes,
    ) { includes = it }
    NumberInput(stringResource(if (includes) R.string.vat_amount_incl else R.string.vat_amount_excl), amount, { amount = it }, suffix = fmt.currency)
    SectionLabel(stringResource(R.string.vat_rate))
    Chips(listOf("21", "12", "6", "0").map { it to "$it %" }, rate) { rate = it }
    NumberInput(stringResource(R.string.vat_rate_custom), rate, { rate = it }, suffix = "%")

    val a = num(amount)
    val r = num(rate)
    if (a != null && r != null) {
        val v = WorkMath.vat(a, r, includes)
        Results {
            ResultRow(stringResource(R.string.vat_excl), fmt.money(v.excl), emphasis = includes)
            ResultRow(stringResource(R.string.vat_vat, fmt.percent(r)), fmt.money(v.vat))
            ResultRow(stringResource(R.string.vat_incl), fmt.money(v.incl), emphasis = !includes)
        }
    }
    FieldNote(stringResource(R.string.vat_note))
}

// ---------- Discount ----------

@Composable
private fun ColumnScope.DiscountTool(fmt: NumFmt) {
    var price by rememberSaveable { mutableStateOf("") }
    var first by rememberSaveable { mutableStateOf("") }
    var second by rememberSaveable { mutableStateOf("") }

    NumberInput(stringResource(R.string.discount_price), price, { price = it }, suffix = fmt.currency)
    NumberInput(stringResource(R.string.discount_percent), first, { first = it }, suffix = "%")
    Chips(listOf("5", "10", "15", "20", "25", "30", "50").map { it to "$it %" }, first) { first = it }
    NumberInput(stringResource(R.string.discount_extra), second, { second = it }, suffix = "%", placeholder = "0")

    val p = num(price)
    val f = num(first) ?: BigDecimal.ZERO
    val s = num(second) ?: BigDecimal.ZERO
    if (p != null) {
        val d = WorkMath.discount(p, f, s)
        Results {
            ResultRow(stringResource(R.string.discount_final), fmt.money(d.final), emphasis = true)
            ResultRow(stringResource(R.string.discount_saved), fmt.money(d.saved))
            if (s.signum() != 0) ResultRow(stringResource(R.string.discount_effective), fmt.percent(d.effectivePercent))
        }
    }
    FieldNote(stringResource(R.string.discount_note))
}

// ---------- Margin ----------

private enum class MarginMode { PRICE, MARGIN, MARKUP }

@Composable
private fun ColumnScope.MarginTool(fmt: NumFmt) {
    var mode by rememberSaveable { mutableStateOf(MarginMode.PRICE) }
    var cost by rememberSaveable { mutableStateOf("") }
    var second by rememberSaveable { mutableStateOf("") }

    Chips(
        listOf(
            MarginMode.PRICE to stringResource(R.string.margin_mode_price),
            MarginMode.MARGIN to stringResource(R.string.margin_mode_margin),
            MarginMode.MARKUP to stringResource(R.string.margin_mode_markup),
        ),
        mode,
    ) { mode = it }
    NumberInput(stringResource(R.string.margin_cost), cost, { cost = it }, suffix = fmt.currency)
    when (mode) {
        MarginMode.PRICE -> NumberInput(stringResource(R.string.margin_price), second, { second = it }, suffix = fmt.currency)
        MarginMode.MARGIN -> NumberInput(stringResource(R.string.margin_margin), second, { second = it }, suffix = "%")
        MarginMode.MARKUP -> NumberInput(stringResource(R.string.margin_markup), second, { second = it }, suffix = "%")
    }

    val c = num(cost)
    val x = num(second)
    if (c != null && x != null) {
        val m = when (mode) {
            MarginMode.PRICE -> WorkMath.marginFromPrice(c, x)
            MarginMode.MARGIN -> WorkMath.priceForMargin(c, x)
            MarginMode.MARKUP -> WorkMath.priceForMarkup(c, x)
        }
        if (m == null) {
            FieldNote(stringResource(R.string.margin_impossible))
        } else {
            Results {
                ResultRow(stringResource(R.string.margin_price), fmt.money(m.price), emphasis = mode != MarginMode.PRICE)
                ResultRow(stringResource(R.string.margin_profit), fmt.money(m.profit), emphasis = mode == MarginMode.PRICE)
                ResultRow(stringResource(R.string.margin_margin), m.marginPercent?.let { fmt.percent(it) } ?: "—")
                ResultRow(stringResource(R.string.margin_markup), m.markupPercent?.let { fmt.percent(it) } ?: "—")
            }
        }
    }
    FieldNote(stringResource(R.string.margin_note))
}

// ---------- Instalments ----------

@Composable
private fun ColumnScope.InstalmentsTool(fmt: NumFmt) {
    var price by rememberSaveable { mutableStateOf("") }
    var down by rememberSaveable { mutableStateOf("") }
    var months by rememberSaveable { mutableStateOf("12") }
    var rate by rememberSaveable { mutableStateOf("0") }

    NumberInput(stringResource(R.string.inst_price), price, { price = it }, suffix = fmt.currency)
    NumberInput(stringResource(R.string.inst_down), down, { down = it }, suffix = fmt.currency, placeholder = "0")
    NumberInput(stringResource(R.string.inst_months), months, { months = it }, decimals = false)
    Chips(listOf("3", "6", "10", "12", "24", "36", "48").map { it to it }, months) { months = it }
    NumberInput(stringResource(R.string.inst_rate), rate, { rate = it }, suffix = "%")

    val p = num(price)
    val d = num(down) ?: BigDecimal.ZERO
    val n = months.toIntOrNull()
    val r = num(rate) ?: BigDecimal.ZERO
    if (p != null && n != null) {
        val i = WorkMath.instalments(p, d, n, r)
        if (i == null) {
            FieldNote(stringResource(R.string.inst_impossible))
        } else {
            Results {
                ResultRow(stringResource(R.string.inst_monthly, n), fmt.money(i.monthly), emphasis = true)
                ResultRow(stringResource(R.string.inst_financed), fmt.money(i.financed))
                ResultRow(stringResource(R.string.inst_total), fmt.money(i.total))
                ResultRow(stringResource(R.string.inst_interest), fmt.money(i.interest))
            }
        }
    }
    FieldNote(stringResource(R.string.inst_note))
}

// ---------- Percentages ----------

@Composable
private fun ColumnScope.PercentTool(fmt: NumFmt) {
    var a1 by rememberSaveable { mutableStateOf("") }
    var b1 by rememberSaveable { mutableStateOf("") }
    var a2 by rememberSaveable { mutableStateOf("") }
    var b2 by rememberSaveable { mutableStateOf("") }
    var a3 by rememberSaveable { mutableStateOf("") }
    var b3 by rememberSaveable { mutableStateOf("") }

    PercentCard(
        stringResource(R.string.pct_of_title),
        stringResource(R.string.pct_of_a), a1, { a1 = it }, "%",
        stringResource(R.string.pct_of_b), b1, { b1 = it },
    ) {
        val x = num(a1)
        val y = num(b1)
        if (x != null && y != null) fmt.number(WorkMath.percentOf(x, y)) else null
    }
    PercentCard(
        stringResource(R.string.pct_what_title),
        stringResource(R.string.pct_what_a), a2, { a2 = it }, null,
        stringResource(R.string.pct_what_b), b2, { b2 = it },
    ) {
        val x = num(a2)
        val y = num(b2)
        if (x != null && y != null) WorkMath.whatPercent(x, y)?.let { fmt.percent(it) } ?: "—" else null
    }
    PercentCard(
        stringResource(R.string.pct_change_title),
        stringResource(R.string.pct_change_a), a3, { a3 = it }, null,
        stringResource(R.string.pct_change_b), b3, { b3 = it },
    ) {
        val x = num(a3)
        val y = num(b3)
        if (x != null && y != null) {
            WorkMath.change(x, y)?.let { (if (it.signum() > 0) "+" else "") + fmt.percent(it) } ?: "—"
        } else {
            null
        }
    }
}

@Composable
private fun PercentCard(
    title: String,
    aLabel: String,
    a: String,
    onA: (String) -> Unit,
    aSuffix: String?,
    bLabel: String,
    b: String,
    onB: (String) -> Unit,
    result: () -> String?,
) {
    CtrlCard(Modifier.fillMaxWidth()) {
        SectionLabel(title)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(aLabel, a, onA, Modifier.weight(1f), suffix = aSuffix)
            NumberInput(bLabel, b, onB, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            result() ?: "—",
            style = CtrlType.Readout.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize),
            color = CtrlColors.Text,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------- Shared pieces ----------

@Composable
private fun <T> Chips(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(CtrlShapes.Chip)
                    .background(if (on) CtrlColors.Accent else CtrlColors.Surface)
                    .border(1.dp, if (on) CtrlColors.Accent else CtrlColors.ControlOutline, CtrlShapes.Chip)
                    .clickable { onPick(value) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = CtrlType.LabelStrong, color = if (on) CtrlColors.OnAccent else CtrlColors.Text)
            }
        }
    }
}

@Composable
private fun Results(content: @Composable ColumnScope.() -> Unit) {
    CtrlCard(Modifier.fillMaxWidth().padding(top = 4.dp), padding = androidx.compose.foundation.layout.PaddingValues(0.dp), content = content)
}

@Composable
private fun ResultRow(label: String, value: String, emphasis: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().topDivider().padding(horizontal = 16.dp, vertical = if (emphasis) 16.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (emphasis) CtrlColors.Text else CtrlColors.Muted, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (emphasis) CtrlType.Readout.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize) else CtrlType.Data.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
            color = if (emphasis) CtrlColors.AccentText else CtrlColors.Text,
            textAlign = TextAlign.End,
        )
    }
}
