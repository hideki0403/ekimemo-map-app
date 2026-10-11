package dev.yukineko.ekimemo_map.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LogLevel { DEBUG, INFO, WARNING, ERROR }

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val time: Long,
)

class LogBuffer(private val capacity: Int) {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun add(entry: LogEntry) {
        _entries.update { (it + entry).takeLast(capacity) }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
