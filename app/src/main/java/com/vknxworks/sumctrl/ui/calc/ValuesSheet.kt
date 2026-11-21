package com.vknxworks.sumctrl.ui.calc

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.container
import com.vknxworks.sumctrl.data.NumFmt
import com.vknxworks.sumctrl.data.ValuesRepository
import com.vknxworks.sumctrl.ui.components.CtrlAlertDialog
import com.vknxworks.sumctrl.ui.components.CtrlBottomSheet
import com.vknxworks.sumctrl.ui.components.CtrlSquareButton
import com.vknxworks.sumctrl.ui.components.CtrlTextField
import com.vknxworks.sumctrl.ui.components.MessageView
import com.vknxworks.sumctrl.ui.components.topDivider
import com.vknxworks.sumctrl.ui.theme.CtrlColors
import com.vknxworks.sumctrl.ui.theme.CtrlType
import com.vknxworks.sumctrl.ui.theme.Gutter
import java.math.BigDecimal

/**
 * Named values and the memory. Tap a value to put its name into the sum (it is worked out with every digit
 * it was saved with); save the current result under a name of your own.
 */
@Composable
fun ValuesSheet(fmt: NumFmt, current: BigDecimal?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val c = LocalContext.current.container
    val stored by c.values.state.collectAsStateWithLifecycle()
    val setAside by c.values.setAside.collectAsStateWithLifecycle()
    var naming by remember { mutableStateOf(false) }

    CtrlBottomSheet(onDismissRequest = onDismiss) {
        Row(
            Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.values_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (current != null) {
                CtrlSquareButton(Icons.Outlined.Save, stringResource(R.string.values_save), onClick = { naming = true })
            }
        }
        val memory = stored.memory?.toBigDecimalOrNull()
        Row(
            Modifier.fillMaxWidth().topDivider().padding(horizontal = Gutter, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.values_memory), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                memory?.let { fmt.result(it) } ?: stringResource(R.string.values_memory_empty),
                style = CtrlType.Readout.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = if (memory != null) CtrlColors.Text else CtrlColors.Inactive,
                maxLines = 1,
            )
        }
        if (stored.values.isEmpty()) {
            MessageView(
                Icons.Outlined.DataObject,
                stringResource(R.string.values_empty),
                stringResource(if (setAside) R.string.values_set_aside_body else R.string.values_empty_body),
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).navigationBarsPadding(),
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(stored.values, key = { it.name }) { v ->
                    val value = v.value.toBigDecimalOrNull()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .topDivider()
                            .combinedClickable(onClick = { onPick(v.name) }, onClickLabel = stringResource(R.string.values_use))
                            .padding(start = Gutter, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(v.name, style = CtrlType.LabelStrong, color = CtrlColors.AccentText)
                            Text(
                                value?.let { fmt.result(it) } ?: v.value,
                                style = CtrlType.Readout.copy(fontSize = 22.sp, lineHeight = 28.sp),
                                color = CtrlColors.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CtrlSquareButton(Icons.Outlined.Close, stringResource(R.string.values_remove), onClick = { c.values.remove(v.name) }, size = 40.dp, tint = CtrlColors.Muted)
                    }
                }
            }
        }
    }

    if (naming && current != null) {
        ValueNameDialog(
            value = fmt.result(current),
            exists = { n -> stored.values.any { it.name.equals(n, ignoreCase = true) } },
            onDismiss = { naming = false },
            onSave = { name ->
                c.values.put(name, current)
                naming = false
            },
        )
    }
}

@Composable
private fun ValueNameDialog(value: String, exists: (String) -> Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val name = text.trim()
    val valid = ValuesRepository.validName(name)
    CtrlAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.values_save_title, value), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CtrlTextField(
                    value = text,
                    onValueChange = { text = it.take(ValuesRepository.NAME_MAX) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.values_name)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(
                        when {
                            name.isNotEmpty() && !valid -> R.string.values_name_invalid
                            valid && exists(name) -> R.string.values_name_replaces
                            else -> R.string.values_name_hint
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (name.isNotEmpty() && !valid) CtrlColors.Error else CtrlColors.Muted,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = valid) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CtrlColors.Text) } },
    )
}
