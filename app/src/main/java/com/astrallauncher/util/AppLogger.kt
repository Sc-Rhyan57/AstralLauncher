package com.astrallauncher.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(val time: String, val level: LogLevel, val tag: String, val msg: String)

object AppLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private fun add(level: LogLevel, tag: String, msg: String) {
        val entry = LogEntry(fmt.format(Date()), level, tag, msg)
        _logs.value = (_logs.value + entry).takeLast(500)
        when (level) {
            LogLevel.ERROR -> android.util.Log.e(tag, msg)
            LogLevel.WARN  -> android.util.Log.w(tag, msg)
            LogLevel.INFO  -> android.util.Log.i(tag, msg)
            LogLevel.DEBUG -> android.util.Log.d(tag, msg)
        }
    }

    fun d(tag: String, msg: String) = add(LogLevel.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = add(LogLevel.INFO,  tag, msg)
    fun w(tag: String, msg: String) = add(LogLevel.WARN,  tag, msg)
    fun e(tag: String, msg: String) = add(LogLevel.ERROR, tag, msg)
    fun clear() { _logs.value = emptyList() }
}
