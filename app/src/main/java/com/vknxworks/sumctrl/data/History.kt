package com.vknxworks.sumctrl.data

import android.content.Context
import com.vknxworks.sumctrl.data.crypto.DataKeyStore
import com.vknxworks.sumctrl.data.crypto.SealedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File


/**
 * The calculator's history, kept in a JSON file in SumCtrl's private storage. Nothing leaves the phone;
 * Android's backups are switched off for the app.
 *
 * The file is encrypted at rest with the app's data key ([DataKeyStore], [SealedFile]). A file from before
 * encryption is still read, and is then replaced by its encrypted copy ([SealedFile.sealInPlace]). When there
 * is no data key to be had, the ordinary file is read and written as before.
 *
 * An encrypted history that can't be opened (the key in the phone's secure storage is gone, or the file is
 * damaged) is never overwritten or deleted by a save: it is moved aside as `history.json.unreadable` and a new
 * history starts. If it opens again at a later start, its sums are put back. Only "Clear history" deletes it.
 */
class HistoryRepository(context: Context, private val scope: CoroutineScope, private val keys: DataKeyStore) {
    private val dir: File = context.filesDir
    private val file = File(dir, FILE)
    private val tmp = File(dir, TMP)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(HistoryEntry.serializer())
    private val lock = Mutex()

    /** The file was read (or tried) in this run. Only touched with [lock] held. */
    private var loaded = false

    /** An unreadable history file couldn't be moved aside: nothing is saved over it. Only touched with [lock] held. */
    private var blocked = false

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    /** Newest first. */
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    private val _setAside = MutableStateFlow(false)

    /** An earlier, encrypted history that can't be opened is being kept aside (the History sheet says so). */
    val setAside: StateFlow<Boolean> = _setAside.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            lock.withLock { loadLocked() }
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

    /** Sets or (with a blank [note]) removes the note on one sum. */
    fun setNote(id: Long, note: String) {
        val n = note.trim().take(NOTE_MAX).ifEmpty { null }
        save(_entries.value.map { if (it.id == id) it.copy(note = n) else it })
    }

    /** Deletes the history, and any earlier history that was kept aside because it couldn't be opened. */
    fun clear() = save(emptyList(), dropAside = true)

    private fun save(list: List<HistoryEntry>, dropAside: Boolean = false) {
        _entries.value = list
        scope.launch(Dispatchers.IO) {
            lock.withLock {
                if (dropAside) {
                    runCatching { asideFiles().forEach { it.delete() } }
                    _setAside.value = runCatching { asideFiles().isNotEmpty() }.getOrDefault(false)
                    // Clearing is the one thing that may remove a history file that can't be opened.
                    blocked = false
                }
                // The file is always read before it is first written, so a save can't replace sums not loaded yet.
                loadLocked()
                if (blocked) return@withLock
                // Always the newest list, whatever order the saves run in.
                write(_entries.value)
            }
        }
    }

    /** Reads the file once per run and adds what it holds under anything typed in the meantime. Call with [lock] held. */
    private fun loadLocked() {
        if (loaded) return
        loaded = true
        // An earlier history that was set aside and opens again (the key is back) is put back.
        val recovered = runCatching { recoverAside() }.getOrDefault(emptyList())
        val fromFile = runCatching { readFile() }.getOrNull().orEmpty()
        val old = fromFile + recovered.flatMap { it.second }
        if (old.isNotEmpty()) {
            // Anything added while the file was still loading stays on top.
            var merged = (_entries.value + old).distinctBy { it.id }
            if (recovered.isNotEmpty()) merged = merged.sortedByDescending { it.time }
            _entries.value = merged.take(MAX)
        }
        if (recovered.isNotEmpty() && !blocked && write(_entries.value)) {
            // Its sums are in the history file now.
            recovered.forEach { runCatching { it.first.delete() } }
        }
        _setAside.value = runCatching { asideFiles().isNotEmpty() }.getOrDefault(false)
    }

    /**
     * The saved history, or null when there is none to use. An ordinary file that reads fine is replaced by its
     * encrypted copy; an encrypted file that can't be opened is moved aside, not deleted.
     */
    private fun readFile(): List<HistoryEntry>? {
        if (!file.exists()) return null
        val bytes = file.readBytes()
        if (SealedFile.isSealed(bytes)) {
            val plain = open(bytes)
            if (plain == null) {
                if (!moveAside()) blocked = true
                return null
            }
            return decode(plain)
        }
        val list = decode(bytes) ?: return null
        try {
            val key = keys.get(create = true)
            if (key != null) SealedFile.sealInPlace(key, file, tmp)
        } catch (e: Throwable) {
            // The ordinary file stays in use.
        }
        return list
    }

    /** Sealed bytes opened with the data key, or null when there is no key or it doesn't open them. */
    private fun open(sealed: ByteArray): ByteArray? {
        val key = keys.get(create = false) ?: return null
        return try {
            SealedFile.open(key, sealed)
        } catch (e: Exception) {
            null
        }
    }

    private fun decode(plain: ByteArray): List<HistoryEntry>? =
        runCatching { json.decodeFromString(serializer, String(plain, Charsets.UTF_8)) }.getOrNull()

    /** Moves the history file out of the way without deleting it (or an earlier one set aside). False when that fails. */
    private fun moveAside(): Boolean = try {
        var aside = File(dir, ASIDE)
        if (aside.exists()) aside = File(dir, "$ASIDE.${System.currentTimeMillis()}")
        file.renameTo(aside)
    } catch (e: Exception) {
        false
    }

    private fun asideFiles(): List<File> = dir.listFiles().orEmpty().filter { it.isFile && it.name.startsWith(ASIDE) }

    /** The histories kept aside that open now, each with its file. */
    private fun recoverAside(): List<Pair<File, List<HistoryEntry>>> = asideFiles().mapNotNull { f ->
        try {
            val bytes = f.readBytes()
            if (!SealedFile.isSealed(bytes)) return@mapNotNull null
            val list = open(bytes)?.let(::decode) ?: return@mapNotNull null
            f to list
        } catch (e: Exception) {
            null
        }
    }

    /** Writes [list] in one step: encrypted when there's a data key, the ordinary file otherwise. True when it worked. */
    private fun write(list: List<HistoryEntry>): Boolean = runCatching {
        if (list.isEmpty()) {
            file.delete()
        } else {
            val plain = json.encodeToString(serializer, list).toByteArray(Charsets.UTF_8)
            val sealed = keys.get(create = true)?.let { SealedFile.sealChecked(it, plain) }
            SealedFile.writeAtomic(file, tmp, sealed ?: plain)
        }
        true
    }.getOrDefault(false)

    companion object {
        const val MAX = 500
        const val NOTE_MAX = 200
        private const val FILE = "history.json"
        private const val TMP = "history.json.tmp"
        private const val ASIDE = "history.json.unreadable"
    }
}
