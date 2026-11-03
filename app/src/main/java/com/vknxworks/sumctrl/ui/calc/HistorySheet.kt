package com.vknxworks.sumctrl.ui.calc

import android.text.format.DateUtils
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.container
import com.vknxworks.sumctrl.data.HistoryEntry
import com.vknxworks.sumctrl.data.NumFmt
import com.vknxworks.sumctrl.ui.components.ConfirmDialog
import com.vknxworks.sumctrl.ui.components.CtrlBottomSheet
import com.vknxworks.sumctrl.ui.components.CtrlSquareButton
import com.vknxworks.sumctrl.ui.components.MessageView
import com.vknxworks.sumctrl.ui.components.topDivider
import com.vknxworks.sumctrl.ui.theme.CtrlColors
import com.vknxworks.sumctrl.ui.theme.CtrlType
import com.vknxworks.sumctrl.ui.theme.Gutter
import java.math.BigDecimal

/** Earlier sums, newest first. Tapping one puts its result back into the calculator. */
@Composable
fun HistorySheet(fmt: NumFmt, enabled: Boolean, onDismiss: () -> Unit, onPick: (BigDecimal) -> Unit) {
    val c = LocalContext.current.container
    val entries by c.history.entries.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    CtrlBottomSheet(onDismissRequest = onDismiss) {
        Row(
            Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                CtrlSquareButton(Icons.Outlined.DeleteSweep, stringResource(R.string.history_clear), onClick = { confirmClear = true })
            }
        }
        when {
            entries.isEmpty() -> MessageView(
                if (enabled) Icons.Outlined.History else Icons.Outlined.HistoryToggleOff,
                stringResource(if (enabled) R.string.history_empty else R.string.history_off),
                stringResource(if (enabled) R.string.history_empty_body else R.string.history_off_body),
                modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).navigationBarsPadding(),
            )
            else -> LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                items(entries, key = { it.id }) { e ->
                    HistoryRow(e, fmt, onPick = { e.result.toBigDecimalOrNull()?.let(onPick) }, onRemove = { c.history.remove(e.id) })
                }
            }
        }
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

@Composable
private fun HistoryRow(e: HistoryEntry, fmt: NumFmt, onPick: () -> Unit, onRemove: () -> Unit) {
    val result = e.result.toBigDecimalOrNull()?.let { fmt.result(it) } ?: e.result
    val whenText = DateUtils.getRelativeTimeSpanString(e.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    Row(
        Modifier
            .fillMaxWidth()
            .topDivider()
            .combinedClickable(onClick = onPick, onClickLabel = stringResource(R.string.history_use))
            .padding(start = Gutter, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                renderTokens(e.tokens, fmt) + " =",
                style = CtrlType.Data.copy(fontSize = 15.sp, lineHeight = 20.sp),
                color = CtrlColors.Muted,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(result, style = CtrlType.Readout.copy(fontSize = 26.sp, lineHeight = 32.sp), color = CtrlColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(whenText, style = CtrlType.Label, color = CtrlColors.Inactive)
        }
        CtrlSquareButton(Icons.Outlined.Close, stringResource(R.string.history_remove), onClick = onRemove, size = 40.dp, tint = CtrlColors.Muted)
    }
}
