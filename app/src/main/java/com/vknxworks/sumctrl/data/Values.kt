package com.vknxworks.sumctrl.data

import android.content.Context
import com.vknxworks.sumctrl.data.calc.Engine
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.math.BigDecimal

/** A number saved under a name, to use in sums by name. [value] is the exact plain decimal string. */
@Serializable
data class NamedValue(val name: String, val value: String)

/** The memory (null when empty) and the named values, as stored. */
@Serializable
data class StoredValues(val memory: String? = null, val values: List<NamedValue> = emptyList())

/**
 * The calculator's memory (M+, M−, MR, MC) and named values, kept in `values.json` in SumCtrl's private
 * storage and encrypted at rest with the same data key as the history ([DataKeyStore], [SealedFile]). Without
 * a data key the ordinary file is used, as for the history.
 *
 * A sealed file that can't be opened is never overwritten or deleted by a save: it is moved aside as
 * `values.json.unreadable`, empty values start, and it is merged back (values here win on a name clash) when
 * it opens at a later start. Only [clear] deletes it.
 */
class ValuesRepository(context: Context, private val scope: CoroutineScope, private val keys: DataKeyStore) {
    private val dir: File = context.filesDir
    private val file = File(dir, FILE)
    private val tmp = File(dir, TMP)
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var loaded = false
    private var blocked = false

    private val _state = MutableStateFlow(StoredValues())
    val state: StateFlow<StoredValues> = _state.asStateFlow()

    private val _setAside = MutableStateFlow(false)

    /** Earlier values that are stored encrypted and couldn't be opened are being kept aside. */
    val setAside: StateFlow<Boolean> = _setAside.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) { lock.withLock { loadLocked() } }
    }

    /** The memory and every named value, keyed the way [Engine.evaluate] looks them up. */
    fun lookup(s: StoredValues = _state.value): Map<String, BigDecimal> {
        val m = HashMap<String, BigDecimal>()
        s.values.forEach { v -> v.value.toBigDecimalOrNull()?.let { m[v.name] = it } }
        s.memory?.toBigDecimalOrNull()?.let { m[Engine.MEMORY] = it }
        return m
    }

    val memory: BigDecimal? get() = _state.value.memory?.toBigDecimalOrNull()

    /** M+ (or M− with a negative [v]): adds to the memory. */
    fun addToMemory(v: BigDecimal) {
        val now = memory ?: BigDecimal.ZERO
        save(_state.value.copy(memory = now.add(v, java.math.MathContext.DECIMAL128).toPlainString()))
    }

    /** MC. */
    fun clearMemory() = save(_state.value.copy(memory = null))

    /** Saves [v] under [name], replacing a value of the same name (names are compared without case). */
    fun put(name: String, v: BigDecimal) {
        val rest = _state.value.values.filterNot { it.name.equals(name, ignoreCase = true) }
        save(_state.value.copy(values = (rest + NamedValue(name, v.toPlainString())).sortedBy { it.name.lowercase() }))
    }

    fun remove(name: String) = save(_state.value.copy(values = _state.value.values.filterNot { it.name == name }))

    /** Deletes the memory, every named value and any earlier values kept aside. */
    fun clear() = save(StoredValues(), dropAside = true)

    private fun save(s: StoredValues, dropAside: Boolean = false) {
        _state.value = s
        scope.launch(Dispatchers.IO) {
            lock.withLock {
                if (dropAside) {
                    runCatching { asideFiles().forEach { it.delete() } }
                    _setAside.value = runCatching { asideFiles().isNotEmpty() }.getOrDefault(false)
                    blocked = false
                }
                loadLocked()
                if (blocked) return@withLock
                write(_state.value)
            }
        }
    }

    /** Reads the file once per run; what was saved in the meantime wins. Call with [lock] held. */
    private fun loadLocked() {
        if (loaded) return
        loaded = true
        val recovered = runCatching { recoverAside() }.getOrDefault(emptyList())
        val fromFile = runCatching { readFile() }.getOrNull()
        val old = listOfNotNull(fromFile) + recovered.map { it.second }
        if (old.isNotEmpty()) {
            var merged = _state.value
            old.forEach { o ->
                val names = merged.values.map { it.name.lowercase() }.toSet()
                merged = StoredValues(
                    memory = merged.memory ?: o.memory,
                    values = (merged.values + o.values.filter { it.name.lowercase() !in names }).sortedBy { it.name.lowercase() },
                )
            }
            _state.value = merged
        }
        if (recovered.isNotEmpty() && !blocked && write(_state.value)) {
            recovered.forEach { runCatching { it.first.delete() } }
        }
        _setAside.value = runCatching { asideFiles().isNotEmpty() }.getOrDefault(false)
    }

    private fun readFile(): StoredValues? {
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
        val s = decode(bytes) ?: return null
        try {
            keys.get(create = true)?.let { SealedFile.sealInPlace(it, file, tmp) }
        } catch (e: Throwable) {
            // The ordinary file stays in use.
        }
        return s
    }

    private fun open(sealed: ByteArray): ByteArray? {
        val key = keys.get(create = false) ?: return null
        return try {
            SealedFile.open(key, sealed)
        } catch (e: Exception) {
            null
        }
    }

    private fun decode(plain: ByteArray): StoredValues? =
        runCatching { json.decodeFromString(StoredValues.serializer(), String(plain, Charsets.UTF_8)) }.getOrNull()

    private fun moveAside(): Boolean = try {
        var aside = File(dir, ASIDE)
        if (aside.exists()) aside = File(dir, "$ASIDE.${System.currentTimeMillis()}")
        file.renameTo(aside)
    } catch (e: Exception) {
        false
    }

    private fun asideFiles(): List<File> = dir.listFiles().orEmpty().filter { it.isFile && it.name.startsWith(ASIDE) }

    private fun recoverAside(): List<Pair<File, StoredValues>> = asideFiles().mapNotNull { f ->
        try {
            val bytes = f.readBytes()
            if (!SealedFile.isSealed(bytes)) return@mapNotNull null
            val s = open(bytes)?.let(::decode) ?: return@mapNotNull null
            f to s
        } catch (e: Exception) {
            null
        }
    }

    private fun write(s: StoredValues): Boolean = runCatching {
        if (s.memory == null && s.values.isEmpty()) {
            file.delete()
        } else {
            val plain = json.encodeToString(StoredValues.serializer(), s).toByteArray(Charsets.UTF_8)
            val sealed = keys.get(create = true)?.let { SealedFile.sealChecked(it, plain) }
            SealedFile.writeAtomic(file, tmp, sealed ?: plain)
        }
        true
    }.getOrDefault(false)

    companion object {
        private const val FILE = "values.json"
        private const val TMP = "values.json.tmp"
        private const val ASIDE = "values.json.unreadable"
        const val NAME_MAX = 16

        private val RESERVED = setOf("e", "m", "ans", "pi", "π", "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "mod")

        /** A name a value can have: a letter, then letters, digits or "_", at most [NAME_MAX], not a key's name. */
        fun validName(name: String): Boolean =
            name.length in 1..NAME_MAX &&
                name[0].isLetter() &&
                name.all { it.isLetterOrDigit() || it == '_' } &&
                name.lowercase() !in RESERVED
    }
}
