package com.vknxworks.sumctrl.ui.convert

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.data.NumFmt
import com.vknxworks.sumctrl.data.convert.Category
import com.vknxworks.sumctrl.data.convert.ConvUnit
import com.vknxworks.sumctrl.data.convert.Units
import com.vknxworks.sumctrl.ui.components.ChoiceSheet
import com.vknxworks.sumctrl.ui.components.CtrlCard
import com.vknxworks.sumctrl.ui.components.CtrlSquareButton
import com.vknxworks.sumctrl.ui.components.CtrlTextField
import com.vknxworks.sumctrl.ui.components.Group
import com.vknxworks.sumctrl.ui.components.GroupRow
import com.vknxworks.sumctrl.ui.components.PickerField
import com.vknxworks.sumctrl.ui.components.SectionLabel
import com.vknxworks.sumctrl.ui.components.TabTitle
import com.vknxworks.sumctrl.ui.components.bottomClearance
import com.vknxworks.sumctrl.ui.components.rememberNumFmt
import com.vknxworks.sumctrl.ui.theme.CtrlColors
import com.vknxworks.sumctrl.ui.theme.CtrlShapes
import com.vknxworks.sumctrl.ui.theme.CtrlType
import com.vknxworks.sumctrl.ui.theme.Gutter

/** Unit converter: pick a category, type a value, see it in the other unit and in every unit of the category. */
@Composable
fun ConvertScreen() {
    val context = LocalContext.current
    val fmt = rememberNumFmt()
    var categoryId by rememberSaveable { mutableStateOf(Units.all.first().id) }
    val category = Units.category(categoryId)
    var fromId by rememberSaveable { mutableStateOf(category.defaultFrom) }
    var toId by rememberSaveable { mutableStateOf(category.defaultTo) }
    var input by rememberSaveable { mutableStateOf("1") }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    val from = category.unit(fromId) ?: category.units.first()
    val to = category.unit(toId) ?: category.units.last()
    val value = NumFmt.parse(input)
    val result = value?.let { category.convert(it, from, to) }

    fun pickCategory(cat: Category) {
        if (cat.id == categoryId) return
        categoryId = cat.id
        fromId = cat.defaultFrom
        toId = cat.defaultTo
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .bottomClearance(),
    ) {
        TabTitle(stringResource(R.string.tab_convert))

        // Categories
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Units.all.forEach { cat ->
                val selected = cat.id == categoryId
                Box(
                    Modifier
                        .heightIn(min = 40.dp)
                        .clip(CtrlShapes.Chip)
                        .background(if (selected) CtrlColors.Accent else CtrlColors.Surface)
                        .border(1.dp, if (selected) CtrlColors.Accent else CtrlColors.ControlOutline, CtrlShapes.Chip)
                        .clickable { pickCategory(cat) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(cat.name), style = CtrlType.LabelStrong, color = if (selected) CtrlColors.OnAccent else CtrlColors.Text)
                }
            }
        }

        CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 10.dp)) {
            SectionLabel(stringResource(R.string.convert_from))
            Spacer(Modifier.height(8.dp))
            CtrlTextField(
                value = input,
                onValueChange = { t -> if (t.length <= 24 && t.all { it.isDigit() || it == ',' || it == '.' || it == '-' }) input = t },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = { Text(from.symbol, style = CtrlType.Data, color = CtrlColors.Muted, modifier = Modifier.padding(end = 12.dp)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            PickerField(stringResource(R.string.convert_unit), unitLabel(from), onClick = { picking = "from" })

            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(CtrlColors.Divider))
                Spacer(Modifier.width(10.dp))
                CtrlSquareButton(
                    Icons.Outlined.SwapVert,
                    stringResource(R.string.convert_swap),
                    onClick = {
                        val old = fromId
                        fromId = to.id
                        toId = old
                        // Keep the number on screen: the result becomes the new input.
                        if (result != null) input = fmt.number(result).replace(" ", "").replace('−', '-')
                    },
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f).height(1.dp).background(CtrlColors.Divider))
            }

            SectionLabel(stringResource(R.string.convert_to))
            Spacer(Modifier.height(6.dp))
            val shown = when {
                value == null -> "—"
                result == null -> stringResource(R.string.convert_no_answer)
                else -> fmt.number(result)
            }
            Text(
                "$shown ${if (result != null) to.symbol else ""}".trim(),
                style = CtrlType.Readout.copy(fontSize = if (shown.length > 14) 28.sp else 36.sp),
                color = CtrlColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (result != null) {
                                context.getSystemService(ClipboardManager::class.java)
                                    ?.setPrimaryClip(ClipData.newPlainText("SumCtrl", fmt.number(result).replace(" ", "").replace('−', '-')))
                            }
                        },
                        onLongClickLabel = stringResource(R.string.copy_result),
                    )
                    .padding(vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            PickerField(stringResource(R.string.convert_unit), unitLabel(to), onClick = { picking = "to" })
        }

        if (value != null) {
            Group(stringResource(R.string.convert_all, input.ifBlank { "0" }, from.symbol), footer = stringResource(R.string.convert_all_footer)) {
                category.units.filter { it !== from }.forEach { u ->
                    val v = category.convert(value, from, u)
                    GroupRow(
                        title = stringResource(u.name),
                        value = (v?.let { fmt.number(it) } ?: "—") + " " + u.symbol,
                        onClick = { toId = u.id },
                    )
                }
            }
        }
    }

    picking?.let { which ->
        ChoiceSheet(
            title = stringResource(if (which == "from") R.string.convert_from else R.string.convert_to),
            options = category.units.map { it.id to unitText(it) },
            selected = if (which == "from") from.id else to.id,
            onDismiss = { picking = null },
        ) { id ->
            if (which == "from") fromId = id else toId = id
            picking = null
        }
    }
}

@Composable
private fun unitText(u: ConvUnit) = "${stringResource(u.name)} (${u.symbol})"

@Composable
private fun unitLabel(u: ConvUnit) = unitText(u)
