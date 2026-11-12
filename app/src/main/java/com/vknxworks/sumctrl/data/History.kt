package com.vknxworks.sumctrl.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** One finished sum: the keys typed and the exact result (as a plain decimal string). */
@Serializable
data class HistoryEntry(
    val id: Long,
    val time: Long,
    val tokens: List<String>,
    val result: String,
)

/**
 * The calculator's history, kept in a JSON file in SumCtrl's private storage. Nothing leaves the phone;
 * Android's backups are switched off for the app.
 */
class HistoryRepository(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "history.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    /** Newest first. */
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            lock.withLock {
                val loaded = runCatching { json.decodeFromString(ListSerializer(HistoryEntry.serializer()), file.readText()) }.getOrNull()
                // Anything added while the file was still loading stays on top.
                if (loaded != null) _entries.value = (_entries.value + loaded).distinctBy { it.id }.take(MAX)
            }
        }
    }

    fun add(tokens: List<String>, result: String) {
        val now = System.currentTimeMillis()
        // The same sum twice in a row is kept once.
        val current = _entries.value
        if (current.firstOrNull()?.let { it.tokens == tokens && it.result == result } == true) return
        save((listOf(HistoryEntry(now, now, tokens, result)) + current).take(MAX))
    }

    fun remove(id: Long) = save(_entries.value.filterNot { it.id == id })

    fun clear() = save(emptyList())

    private fun save(list: List<HistoryEntry>) {
        _entries.value = list
        scope.launch(Dispatchers.IO) {
            lock.withLock {
                // Always the newest list, whatever order the saves run in.
                val latest = _entries.value
                runCatching {
                    if (latest.isEmpty()) {
                        file.delete()
                    } else {
                        val tmp = File(file.parentFile, "history.json.tmp")
                        tmp.writeText(json.encodeToString(ListSerializer(HistoryEntry.serializer()), latest))
                        tmp.renameTo(file)
                    }
                }
            }
        }
    }

    companion object {
        const val MAX = 500
    }
}
