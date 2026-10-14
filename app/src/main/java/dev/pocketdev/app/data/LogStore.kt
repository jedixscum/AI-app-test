package dev.pocketdev.app.data

import android.content.Context
import dev.pocketdev.app.core.Redactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { Info, Warn, Error }

data class LogEntry(val time: Long, val level: LogLevel, val source: String, val message: String)

/** App log that survives restarts. Every message is redacted before it is stored. */
class LogStore(context: Context) {
    private val file = File(context.filesDir, "pocketdev.log")
    private val maxEntries = 500
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    init {
        _entries.value = load()
        persist(_entries.value)
    }

    @Synchronized
    fun log(level: LogLevel, source: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, source, Redactor.redact(message).take(4000))
        val next = (_entries.value + entry).takeLast(maxEntries)
        _entries.value = next
        runCatching { file.appendText(serialize(entry) + "\n") }
    }

    fun info(source: String, message: String) = log(LogLevel.Info, source, message)
    fun warn(source: String, message: String) = log(LogLevel.Warn, source, message)
    fun error(source: String, message: String) = log(LogLevel.Error, source, message)

    @Synchronized
    fun clear() {
        _entries.value = emptyList()
        runCatching { file.writeText("") }
    }

    fun exportText(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return _entries.value.joinToString("\n") {
            "${fmt.format(Date(it.time))} ${it.level.name.uppercase()} [${it.source}] ${it.message}"
        }
    }

    private fun serialize(e: LogEntry) =
        listOf(e.time, e.level.name, e.source, e.message.replace("\n", "\\n")).joinToString("\t")

    private fun load(): List<LogEntry> = runCatching {
        if (!file.exists()) emptyList() else file.readLines().mapNotNull { line ->
            val parts = line.split("\t", limit = 4)
            if (parts.size < 4) null else LogEntry(
                parts[0].toLongOrNull() ?: return@mapNotNull null,
                runCatching { LogLevel.valueOf(parts[1]) }.getOrDefault(LogLevel.Info),
                parts[2],
                parts[3].replace("\\n", "\n"),
            )
        }.takeLast(maxEntries)
    }.getOrDefault(emptyList())

    private fun persist(list: List<LogEntry>) {
        runCatching { file.writeText(list.joinToString("") { serialize(it) + "\n" }) }
    }
}
