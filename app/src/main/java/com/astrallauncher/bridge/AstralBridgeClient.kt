package com.astrallauncher.bridge

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cliente TCP que conecta ao AstralBridge.dll rodando dentro do processo do Among Us.
 *
 * Protocolo:
 *   → Envia linhas JSON: {"action":"speed_on","value":2.5}
 *   ← Recebe linhas JSON: {"ok":true,"action":"speed_on"}
 *
 * O plugin C# escuta em 127.0.0.1:7777 assim que o jogo inicia.
 * Tentativas automáticas de reconexão por até 2 minutos.
 */
class AstralBridgeClient {

    companion object {
        private const val TAG      = "AstralBridge"
        private const val HOST     = "127.0.0.1"
        private const val PORT     = 7777
        private const val MAX_RETRY = 60
        private const val RETRY_MS  = 2000L
    }

    private var socket: Socket?         = null
    private var writer: PrintWriter?    = null
    private var reader: BufferedReader? = null
    private val _connected = AtomicBoolean(false)
    val isConnected: Boolean get() = _connected.get()

    var onConnected:    (() -> Unit)?       = null
    var onDisconnected: (() -> Unit)?       = null
    var onResponse:     ((String) -> Unit)? = null

    private val scope   = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null

    // ─── Conexão com retry ────────────────────────────────────
    fun connect() {
        scope.launch {
            for (attempt in 1..MAX_RETRY) {
                if (_connected.get()) return@launch
                try {
                    val s = Socket()
                    s.connect(InetSocketAddress(HOST, PORT), 2000)
                    s.tcpNoDelay = true
                    socket = s
                    writer = PrintWriter(s.getOutputStream(), true)
                    reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    _connected.set(true)
                    Log.i(TAG, "Conectado ao AstralBridge (tentativa $attempt)")
                    withContext(Dispatchers.Main) { onConnected?.invoke() }
                    startReading()
                    return@launch
                } catch (e: Exception) {
                    Log.d(TAG, "Tentativa $attempt: ${e.message}")
                    if (attempt < MAX_RETRY) delay(RETRY_MS)
                }
            }
            Log.w(TAG, "Falha ao conectar após $MAX_RETRY tentativas. AU com plugin rodando?")
        }
    }

    private fun startReading() {
        readJob = scope.launch {
            try {
                val r = reader ?: return@launch
                while (_connected.get()) {
                    val line = r.readLine() ?: break
                    Log.d(TAG, "← $line")
                    withContext(Dispatchers.Main) { onResponse?.invoke(line) }
                }
            } catch (e: Exception) {
                if (_connected.get()) Log.w(TAG, "Leitura: ${e.message}")
            } finally {
                val wasConn = _connected.getAndSet(false)
                if (wasConn) withContext(Dispatchers.Main) { onDisconnected?.invoke() }
            }
        }
    }

    // ─── Envio ────────────────────────────────────────────────
    fun send(action: String, value: Double? = null, x: Double? = null, y: Double? = null): Boolean {
        if (!_connected.get()) return false
        return try {
            val json = JSONObject().apply {
                put("action", action)
                value?.let { put("value", it) }
                x?.let { put("x", it) }
                y?.let { put("y", it) }
            }
            writer?.println(json.toString())
            Log.d(TAG, "→ $json")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Envio: ${e.message}")
            _connected.set(false)
            false
        }
    }

    // ─── API de alto nível ────────────────────────────────────
    fun speedOn(mult: Double = 2.5)   = send("speed_on",  value = mult)
    fun speedOff()                    = send("speed_off")
    fun noClipOn()                    = send("noclip_on")
    fun noClipOff()                   = send("noclip_off")
    fun killCooldownOn()              = send("kill_cooldown_on")
    fun killCooldownOff()             = send("kill_cooldown_off")
    fun godModeOn()                   = send("godmode_on")
    fun godModeOff()                  = send("godmode_off")
    fun revealOn()                    = send("reveal_on")
    fun revealOff()                   = send("reveal_off")
    fun visionOn(mult: Double = 10.0) = send("vision_on",  value = mult)
    fun visionOff()                   = send("vision_off")
    fun completeTasks()               = send("tasks_complete")
    fun teleport(x: Double, y: Double) = send("teleport", x = x, y = y)

    // ─── Executor de scripts ──────────────────────────────────
    /**
     * Interpreta script linha por linha e executa comandos.
     *
     * Sintaxe:
     *   speed(2.5)          noclip_on()     kill_cooldown_on()
     *   speed_off()         noclip_off()    kill_cooldown_off()
     *   reveal()            godmode()       vision(10)
     *   vision_off()        tasks()         tp(x, y)
     *   -- comentário
     */
    fun executeScript(script: String): String {
        val out   = StringBuilder()
        val noConn = "✗ Não conectado — inicie o AU com AstralBridge.dll instalado"

        fun ok(msg: String)  { out.append("✓ $msg\n") }
        fun err(msg: String) { out.append("✗ $msg\n") }
        fun conn(): Boolean  = if (_connected.get()) true else { err(noConn); false }

        fun double(line: String, prefix: String) =
            line.removePrefix(prefix).removeSuffix(")").trim().toDoubleOrNull()

        for (rawLine in script.trim().lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("--")) continue

            when {
                line.startsWith("speed(")          -> if (conn()) { val v = double(line,"speed(") ?: 2.5;  speedOn(v);       ok("Speed x$v ativado") }
                line == "speed_off()"               -> if (conn()) { speedOff();       ok("Speed desativado") }
                line == "noclip_on()"               -> if (conn()) { noClipOn();       ok("NoClip ativado") }
                line == "noclip_off()"              -> if (conn()) { noClipOff();      ok("NoClip desativado") }
                line == "kill_cooldown_on()"        -> if (conn()) { killCooldownOn(); ok("Kill Cooldown zero") }
                line == "kill_cooldown_off()"       -> if (conn()) { killCooldownOff();ok("Kill Cooldown restaurado") }
                line == "reveal()"                  -> if (conn()) { revealOn();       ok("Impostores revelados (vermelho)") }
                line == "godmode()"                 -> if (conn()) { godModeOn();      ok("God Mode ativado") }
                line.startsWith("vision(")          -> if (conn()) { val v = double(line,"vision(") ?: 10.0; visionOn(v); ok("Visão x$v") }
                line == "vision_off()"              -> if (conn()) { visionOff();      ok("Visão restaurada") }
                line == "tasks()"                   -> if (conn()) { completeTasks();  ok("Completando tasks...") }
                line.startsWith("tp(")              -> {
                    val args = line.removePrefix("tp(").removeSuffix(")").split(",")
                    val px = args.getOrNull(0)?.trim()?.toDoubleOrNull()
                    val py = args.getOrNull(1)?.trim()?.toDoubleOrNull()
                    if (px != null && py != null) {
                        if (conn()) { teleport(px, py); ok("Teleportado para ($px, $py)") }
                    } else err("Sintaxe: tp(x, y) — ex: tp(0.0, 5.0)")
                }
                else -> err("Comando desconhecido: \"$line\"")
            }
        }

        return out.toString().trimEnd().ifEmpty { "Nenhum comando executado." }
    }

    fun disconnect() {
        if (!_connected.getAndSet(false)) return
        readJob?.cancel()
        try { socket?.close() } catch (_: Exception) {}
        socket = null; writer = null; reader = null
    }

    fun destroy() { disconnect(); scope.cancel() }
}
