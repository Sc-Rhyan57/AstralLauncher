package com.astrallauncher.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var logFile: File? = null

    enum class Level { DEBUG, INFO, WARN, ERROR }
    data class LogEntry(val time: String, val level: Level, val tag: String, val msg: String)

    fun init(ctx: Context) {
        logFile = File(ctx.filesDir, "astral_log.txt")
        if ((logFile?.length() ?: 0) > 2 * 1024 * 1024) logFile?.delete()
        logFile?.parentFile?.mkdirs()
    }

    private fun append(level: Level, tag: String, msg: String) {
        val entry = LogEntry(fmt.format(Date()), level, tag, msg)
        _logs.value = (_logs.value + entry).takeLast(500)
        try { logFile?.appendText("[${entry.time}] [${level.name}] [$tag] $msg\n") } catch (_: Exception) {}
    }

    fun d(tag: String, msg: String) = append(Level.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = append(Level.INFO, tag, msg)
    fun w(tag: String, msg: String) = append(Level.WARN, tag, msg)
    fun e(tag: String, msg: String) = append(Level.ERROR, tag, msg)
    fun clearLogs() { _logs.value = emptyList() }
    fun exportLogs() = _logs.value.joinToString("\n") { "[${it.time}] [${it.level.name}] [${it.tag}] ${it.msg}" }
}
