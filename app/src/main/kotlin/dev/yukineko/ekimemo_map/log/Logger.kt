package dev.yukineko.ekimemo_map.log

import android.util.Log

class Logger(private val tag: String) {
    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun info(message: String) = log(LogLevel.INFO, message)
    fun warning(message: String, throwable: Throwable? = null) = log(LogLevel.WARNING, message, throwable)
    fun error(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)

    private fun log(level: LogLevel, message: String, throwable: Throwable? = null) {
        val text = if (throwable == null) message else "$message\n${throwable.stackTraceToString()}"
        Log.println(level.priority, tag, text)
        buffer.add(level, tag, text, System.currentTimeMillis())
    }

    private val LogLevel.priority
        get() = when (this) {
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.INFO -> Log.INFO
            LogLevel.WARNING -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
        }

    companion object {
        val buffer = LogBuffer(capacity = 3000)
    }
}
