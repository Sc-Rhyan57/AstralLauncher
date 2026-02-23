package com.astrallauncher.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { D, I, W, E }

data class LogEntry(val level: LogLevel, val time: String, val msg: String)

object AppLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun init(ctx: Context) {}

    private fun add(level: LogLevel, tag: String, msg: String) {
        val entry = LogEntry(level, fmt.format(Date()), "[$tag] $msg")
        _logs.value = (_logs.value + entry).takeLast(200)
    }

    fun d(tag: String, msg: String) { Log.d(tag, msg); add(LogLevel.D, tag, msg) }
    fun i(tag: String, msg: String) { Log.i(tag, msg); add(LogLevel.I, tag, msg) }
    fun w(tag: String, msg: String) { Log.w(tag, msg); add(LogLevel.W, tag, msg) }
    fun e(tag: String, msg: String) { Log.e(tag, msg); add(LogLevel.E, tag, msg) }

    fun clearLogs() { _logs.value = emptyList() }
}
