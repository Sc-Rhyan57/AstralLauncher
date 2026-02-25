package com.astrallauncher.bridge

import android.util.Log
import com.astrallauncher.util.Constants
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class AstralBridgeClient {

    companion object {
        private const val TAG      = "AstralBridge"
        private const val MAX_RETRY = 60
        private const val RETRY_MS  = 2000L
    }

    private var socket: Socket?       = null
    private var writer: PrintWriter?  = null
    private var reader: BufferedReader? = null
    private val _connected = AtomicBoolean(false)
    val isConnected: Boolean get() = _connected.get()

    var onConnected: (() -> Unit)?    = null
    var onDisconnected: (() -> Unit)? = null
    var onResponse: ((String) -> Unit)? = null

    private val scope    = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null

    fun connect() {
        scope.launch {
            for (attempt in 1..MAX_RETRY) {
                if (_connected.get()) return@launch
                try {
                    val s = Socket()
                    s.connect(InetSocketAddress(Constants.BRIDGE_HOST, Constants.BRIDGE_PORT), 2000)
                    s.tcpNoDelay = true
                    socket = s
                    writer = PrintWriter(s.getOutputStream(), true)
                    reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    _connected.set(true)
                    Log.i(TAG, "Conectado (tentativa $attempt)")
                    withContext(Dispatchers.Main) { onConnected?.invoke() }
                    startReading()
                    return@launch
                } catch (e: Exception) {
                    if (attempt < MAX_RETRY) delay(RETRY_MS)
                }
            }
        }
    }

    private fun startReading() {
        readJob = scope.launch {
            try {
                val r = reader ?: return@launch
                while (_connected.get()) {
                    val line = r.readLine() ?: break
                    withContext(Dispatchers.Main) { onResponse?.invoke(line) }
                }
            } catch (e: Exception) {
                if (_connected.get()) Log.w(TAG, "Leitura: ${e.message}")
            } finally {
                if (_connected.getAndSet(false))
                    withContext(Dispatchers.Main) { onDisconnected?.invoke() }
            }
        }
    }

    fun send(action: String, value: Double? = null, x: Double? = null, y: Double? = null): Boolean {
        if (!_connected.get()) return false
        return runCatching {
            val json = JSONObject().apply {
                put("action", action)
                value?.let { put("value", it) }
                x?.let { put("x", it) }
                y?.let { put("y", it) }
            }
            writer?.println(json.toString())
            true
        }.getOrDefault(false)
    }

    fun speedOn(mult: Double = 2.5)        = send("speed_on", value = mult)
    fun speedOff()                          = send("speed_off")
    fun noClipOn()                          = send("noclip_on")
    fun noClipOff()                         = send("noclip_off")
    fun killCooldownOn()                    = send("kill_cooldown_on")
    fun killCooldownOff()                   = send("kill_cooldown_off")
    fun godModeOn()                         = send("godmode_on")
    fun godModeOff()                        = send("godmode_off")
    fun revealOn()                          = send("reveal_on")
    fun revealOff()                         = send("reveal_off")
    fun visionOn(mult: Double = 10.0)       = send("vision_on", value = mult)
    fun visionOff()                         = send("vision_off")
    fun completeTasks()                     = send("tasks_complete")
    fun teleport(x: Double, y: Double)      = send("teleport", x = x, y = y)

    fun executeScript(script: String): String {
        val out = StringBuilder()
        fun ok(msg: String) { out.append("✓ $msg\n") }
        fun err(msg: String) { out.append("✗ $msg\n") }
        fun needConn(): Boolean {
            if (_connected.get()) return true
            err("Não conectado — abra o AU patcheado primeiro"); return false
        }
        fun num(line: String, prefix: String) =
            line.removePrefix(prefix).removeSuffix(")").trim().toDoubleOrNull()

        for (raw in script.trim().lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("--")) continue
            when {
                line.startsWith("speed(")         -> if (needConn()) { val v = num(line, "speed(") ?: 2.5; speedOn(v); ok("Speed x$v") }
                line == "speed_off()"             -> if (needConn()) { speedOff(); ok("Speed desativado") }
                line == "noclip_on()"             -> if (needConn()) { noClipOn(); ok("NoClip ON") }
                line == "noclip_off()"            -> if (needConn()) { noClipOff(); ok("NoClip OFF") }
                line == "kill_cooldown_on()"      -> if (needConn()) { killCooldownOn(); ok("Kill CD zero") }
                line == "kill_cooldown_off()"     -> if (needConn()) { killCooldownOff(); ok("Kill CD restaurado") }
                line == "reveal()"                -> if (needConn()) { revealOn(); ok("Impostores revelados") }
                line == "godmode()"               -> if (needConn()) { godModeOn(); ok("God Mode ON") }
                line.startsWith("vision(")        -> if (needConn()) { val v = num(line, "vision(") ?: 10.0; visionOn(v); ok("Visão x$v") }
                line == "vision_off()"            -> if (needConn()) { visionOff(); ok("Visão restaurada") }
                line == "tasks()"                 -> if (needConn()) { completeTasks(); ok("Tasks completadas") }
                line.startsWith("tp(")            -> {
                    val args = line.removePrefix("tp(").removeSuffix(")").split(",")
                    val px = args.getOrNull(0)?.trim()?.toDoubleOrNull()
                    val py = args.getOrNull(1)?.trim()?.toDoubleOrNull()
                    if (px != null && py != null && needConn()) { teleport(px, py); ok("Teleport → ($px, $py)") }
                    else if (px == null || py == null) err("Sintaxe: tp(x, y)")
                }
                else -> err("Comando desconhecido: \"$line\"")
            }
        }
        return out.toString().trimEnd().ifEmpty { "Nenhum comando executado." }
    }

    fun disconnect() {
        if (!_connected.getAndSet(false)) return
        readJob?.cancel()
        runCatching { socket?.close() }
        socket = null; writer = null; reader = null
    }

    fun destroy() { disconnect(); scope.cancel() }
}
