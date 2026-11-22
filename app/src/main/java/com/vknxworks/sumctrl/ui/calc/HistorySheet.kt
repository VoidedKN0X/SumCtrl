package com.vknxworks.sumctrl.ui.calc

import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.vknxworks.sumctrl.data.HistoryCsv
import com.vknxworks.sumctrl.data.HistoryRepository
import com.vknxworks.sumctrl.ui.components.CtrlAlertDialog
import com.vknxworks.sumctrl.ui.components.CtrlTextField
import com.vknxworks.sumctrl.ui.lock.AppLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    val setAside by c.history.setAside.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HistoryEntry?>(null) }
    val context = LocalContext.current
    val header = listOf(
        stringResource(R.string.csv_time),
        stringResource(R.string.csv_sum),
        stringResource(R.string.csv_result),
        stringResource(R.string.csv_note),
    )

    // CSV export: the whole history, oldest first, to a file you pick. It's an ordinary file there, not encrypted.
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val list = c.history.entries.value
        val app = context.applicationContext
        // In the app's own scope, not the sheet's: closing the sheet right after picking the file must not cut
        // the file short. The toast uses the application context, so it still shows when the sheet is gone.
        c.appScope.launch {
            val ok = runCatching {
                val text = HistoryCsv.write(list, ZoneId.systemDefault(), header)
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                        ?: error("no stream")
                }
            }.isSuccess
            withContext(Dispatchers.Main) {
                Toast.makeText(app, if (ok) R.string.csv_saved else R.string.csv_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    CtrlBottomSheet(onDismissRequest = onDismiss) {
        Row(
            Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                CtrlSquareButton(Icons.Outlined.FileDownload, stringResource(R.string.csv_export), onClick = {
                    AppLock.expectReturn()
                    export.launch("sumctrl-history-${LocalDate.now()}.csv")
                })
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                CtrlSquareButton(Icons.Outlined.DeleteSweep, stringResource(R.string.history_clear), onClick = { confirmClear = true })
            }
        }
        when {
            entries.isEmpty() -> MessageView(
                if (enabled) Icons.Outlined.History else Icons.Outlined.HistoryToggleOff,
                stringResource(if (enabled) R.string.history_empty else R.string.history_off),
                stringResource(
                    when {
                        !enabled -> R.string.history_off_body
                        setAside -> R.string.history_set_aside_body
                        else -> R.string.history_empty_body
                    },
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).navigationBarsPadding(),
            )
            else -> LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                items(entries, key = { it.id }) { e ->
                    HistoryRow(
                        e,
                        fmt,
                        onPick = { e.result.toBigDecimalOrNull()?.let(onPick) },
                        onNote = { editing = e },
                        onRemove = { c.history.remove(e.id) },
                    )
                }
            }
        }
    }

    editing?.let { e ->
        NoteDialog(
            initial = e.note.orEmpty(),
            onDismiss = { editing = null },
            onSave = { note ->
                c.history.setNote(e.id, note)
                editing = null
            },
        )
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
private fun HistoryRow(e: HistoryEntry, fmt: NumFmt, onPick: () -> Unit, onNote: () -> Unit, onRemove: () -> Unit) {
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
            if (!e.note.isNullOrBlank()) {
                Text(e.note, style = MaterialTheme.typography.bodyMedium, color = CtrlColors.AccentText, textAlign = TextAlign.End, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Text(whenText, style = CtrlType.Label, color = CtrlColors.Inactive)
        }
        CtrlSquareButton(
            Icons.Outlined.EditNote,
            stringResource(if (e.note.isNullOrBlank()) R.string.history_note_add else R.string.history_note_edit),
            onClick = onNote,
            size = 40.dp,
            tint = CtrlColors.Muted,
        )
        CtrlSquareButton(Icons.Outlined.Close, stringResource(R.string.history_remove), onClick = onRemove, size = 40.dp, tint = CtrlColors.Muted)
    }
}

/** Adds, changes or (left empty) removes the note on a sum. */
@Composable
private fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    CtrlAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_note_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            CtrlTextField(
                value = text,
                onValueChange = { text = it.take(HistoryRepository.NOTE_MAX) },
                minLines = 2,
                placeholder = { Text(stringResource(R.string.history_note_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CtrlColors.Text) } },
    )
}
