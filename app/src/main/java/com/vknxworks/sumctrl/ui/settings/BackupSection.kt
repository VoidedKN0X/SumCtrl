package com.vknxworks.sumctrl.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.vknxworks.sumctrl.BuildConfig
import com.vknxworks.sumctrl.R
import com.vknxworks.sumctrl.container
import com.vknxworks.sumctrl.data.settings.BackupFormatException
import com.vknxworks.sumctrl.data.settings.SettingsBackup
import com.vknxworks.sumctrl.data.settings.SettingsBackups
import com.vknxworks.sumctrl.ui.components.CtrlAlertDialog
import com.vknxworks.sumctrl.ui.components.Group
import com.vknxworks.sumctrl.ui.components.GroupRow
import com.vknxworks.sumctrl.ui.lock.AppLock
import com.vknxworks.sumctrl.ui.theme.CtrlColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Export and import of SumCtrl's settings as a JSON file in a place you pick. The file never holds the app
 * lock, its PIN, "Lock again", the history, the memory or named values; importing one says what it replaces
 * and asks first, and can only turn "Hide in screenshots" / "Hide in recent apps" on.
 */
@Composable
fun BackupGroup() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<SettingsBackup?>(null) }

    fun toast(id: Int) = Toast.makeText(context, id, Toast.LENGTH_LONG).show()

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = SettingsBackups.export(SettingsBackups.from(c.settings.settings.value, BuildConfig.VERSION_NAME, OffsetDateTime.now().toString()))
        val app = context.applicationContext
        // In the app's scope, so leaving Settings right after picking the file doesn't cut the file short.
        c.appScope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: error("no stream")
                }
            }.isSuccess
            withContext(Dispatchers.Main) {
                Toast.makeText(app, if (ok) R.string.backup_saved else R.string.backup_save_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) { readSmall(context, uri) } ?: throw BackupFormatException(newer = false)
                SettingsBackups.parse(text)
            }
            result.onSuccess { pending = it }.onFailure { e ->
                toast(if (e is BackupFormatException && e.newer) R.string.backup_too_new else R.string.backup_not_valid)
            }
        }
    }

    Group(stringResource(R.string.backup_title), footer = stringResource(R.string.backup_footer)) {
        GroupRow(
            stringResource(R.string.backup_save),
            stringResource(R.string.backup_save_sub),
            icon = Icons.Outlined.FileDownload,
            onClick = { AppLock.expectReturn(); save.launch("sumctrl-settings-${LocalDate.now()}.json") },
        )
        GroupRow(
            stringResource(R.string.backup_restore),
            stringResource(R.string.backup_restore_sub),
            icon = Icons.Outlined.FileUpload,
            onClick = { AppLock.expectReturn(); open.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
        )
    }

    pending?.let { b ->
        CtrlAlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_restore_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.backup_restore_body, b.createdAt?.take(10) ?: "?"), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    val app = context.applicationContext
                    // In the app's scope, so leaving Settings straight away doesn't stop the import halfway.
                    c.appScope.launch {
                        c.settings.update { SettingsBackups.apply(b, it) }
                        withContext(Dispatchers.Main) { Toast.makeText(app, R.string.backup_restored, Toast.LENGTH_LONG).show() }
                    }
                }) { Text(stringResource(R.string.backup_restore_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel), color = CtrlColors.Text) } },
        )
    }
}

private fun readSmall(context: Context, uri: Uri): String? =
    context.contentResolver.openInputStream(uri)?.use { input ->
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > SettingsBackups.MAX_BYTES) return@use null
        }
        out.toString(Charsets.UTF_8.name())
    }
