package com.astrallauncher.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    data class LogEntry(val time: String, val level: Level, val tag: String, val msg: String)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun init(ctx: Context) { i("AppLogger", "Logger initialized") }

    fun d(tag: String, msg: String) = log(Level.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(Level.INFO,  tag, msg)
    fun w(tag: String, msg: String) = log(Level.WARN,  tag, msg)
    fun e(tag: String, msg: String) = log(Level.ERROR, tag, msg)

    private fun log(level: Level, tag: String, msg: String) {
        val entry = LogEntry(fmt.format(Date()), level, tag, msg)
        val current = _logs.value.takeLast(199)
        _logs.value = current + entry
    }

    fun exportLogs(): String = _logs.value.joinToString("\n") { "[${it.time}][${it.level.name}][${it.tag}] ${it.msg}" }

    fun clearLogs() { _logs.value = emptyList() }
}
