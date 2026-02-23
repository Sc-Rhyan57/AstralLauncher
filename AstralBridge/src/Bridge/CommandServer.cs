using System;
using System.Collections.Concurrent;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Threading;
using AstralBridge.Core;
using UnityEngine;

namespace AstralBridge.Bridge;

/// <summary>
/// Servidor TCP em 127.0.0.1:7777.
/// O Astral Android (AstralBridgeClient.kt) se conecta aqui e envia
/// comandos JSON linha a linha: {"action":"speed_on","value":2.5}
/// 
/// Os comandos são enfileirados e drenados no thread principal do Unity
/// para evitar race conditions com a engine.
/// </summary>
public sealed class CommandServer
{
    // ─── Fila consumida pelo thread principal do Unity ─────────
    private static readonly ConcurrentQueue<AstralCommand> _queue = new();

    private readonly int          _port;
    private TcpListener?          _listener;
    private Thread?               _acceptThread;
    private volatile bool         _running;

    public CommandServer(int port) => _port = port;

    // ─── Ciclo de vida ─────────────────────────────────────────
    public void Start()
    {
        _running  = true;
        _listener = new TcpListener(IPAddress.Loopback, _port);
        _listener.Start();

        _acceptThread = new Thread(AcceptLoop)
            { Name = "AstralBridge-Accept", IsBackground = true };
        _acceptThread.Start();

        Plugin.Log.LogInfo($"[Bridge] TCP em 127.0.0.1:{_port} — aguardando Astral...");
    }

    public void Stop()
    {
        _running = false;
        _listener?.Stop();
        _acceptThread?.Interrupt();
    }

    // ─── Loop de aceite de conexões ────────────────────────────
    private void AcceptLoop()
    {
        while (_running)
        {
            try
            {
                var client = _listener!.AcceptTcpClient();
                Plugin.Log.LogInfo("[Bridge] Astral conectado.");
                var t = new Thread(() => ClientLoop(client)) { IsBackground = true };
                t.Start();
            }
            catch (Exception ex) when (_running)
            {
                Plugin.Log.LogWarning($"[Bridge] Erro accept: {ex.Message}");
            }
        }
    }

    // ─── Loop de comunicação com um client ────────────────────
    private void ClientLoop(TcpClient client)
    {
        try
        {
            using var stream = client.GetStream();
            using var reader = new StreamReader(stream, Encoding.UTF8, leaveOpen: true);
            using var writer = new StreamWriter(stream, Encoding.UTF8, leaveOpen: true) { AutoFlush = true };

            // Handshake inicial
            writer.WriteLine("""{"status":"connected","plugin":"AstralBridge","version":"1.0.0"}""");

            while (_running && client.Connected)
            {
                var line = reader.ReadLine();
                if (line == null) break;
                if (string.IsNullOrWhiteSpace(line)) continue;

                try
                {
                    var cmd = JsonSerializer.Deserialize<AstralCommand>(line,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (cmd?.Action != null)
                    {
                        _queue.Enqueue(cmd);
                        writer.WriteLine($"{{\"ok\":true,\"action\":\"{cmd.Action}\"}}");
                    }
                }
                catch (Exception ex)
                {
                    writer.WriteLine($"{{\"ok\":false,\"error\":\"{ex.Message.Replace("\"", "'")}\"}}");
                }
            }
        }
        catch (Exception ex)
        {
            Plugin.Log.LogWarning($"[Bridge] Client error: {ex.Message}");
        }
        finally
        {
            try { client.Close(); } catch { }
            Plugin.Log.LogInfo("[Bridge] Astral desconectado.");
        }
    }

    // ─── Drenagem da fila (chamado no thread principal Unity) ──
    /// <summary>
    /// Chame isso dentro de um HarmonyPatch.Prefix/Postfix para
    /// processar comandos pendentes com segurança no main thread.
    /// </summary>
    public static void Drain(HackState state)
    {
        while (_queue.TryDequeue(out var cmd))
            Apply(state, cmd);
    }

    // ─── Aplicação de cada comando ─────────────────────────────
    private static void Apply(HackState state, AstralCommand cmd)
    {
        switch (cmd.Action.ToLowerInvariant())
        {
            // Speed
            case "speed_on":
                state.SpeedEnabled    = true;
                if (cmd.Value is float sv) state.SpeedMultiplier = sv;
                Plugin.Log.LogInfo($"[Speed] ON x{state.SpeedMultiplier}");
                break;
            case "speed_off":
                state.SpeedEnabled = false;
                state.ResetOriginalSpeed();
                Plugin.Log.LogInfo("[Speed] OFF");
                break;
            case "speed_set":
                if (cmd.Value is float mult) { state.SpeedMultiplier = mult; }
                break;

            // NoClip
            case "noclip_on":  state.NoClipEnabled = true;  Plugin.Log.LogInfo("[NoClip] ON");  break;
            case "noclip_off": state.NoClipEnabled = false; Plugin.Log.LogInfo("[NoClip] OFF"); break;

            // Kill cooldown zero
            case "kill_cooldown_on":  state.KillCooldownEnabled = true;  break;
            case "kill_cooldown_off": state.KillCooldownEnabled = false; break;

            // God mode
            case "godmode_on":  state.GodModeEnabled = true;  break;
            case "godmode_off": state.GodModeEnabled = false; break;

            // Reveal impostores
            case "reveal_on":  state.RevealEnabled = true;  break;
            case "reveal_off": state.RevealEnabled = false; break;

            // Visão
            case "vision_on":
                state.VisionEnabled = true;
                if (cmd.Value is float vv) state.VisionMultiplier = vv;
                break;
            case "vision_off": state.VisionEnabled = false; break;

            // Tasks (one-shot)
            case "tasks_complete": state.AutoTaskOnce = true; break;

            // Teleport
            case "teleport":
                if (cmd.X is float px && cmd.Y is float py)
                    state.SetTeleport(new Vector2(px, py));
                break;

            default:
                Plugin.Log.LogWarning($"[Bridge] Comando desconhecido: {cmd.Action}");
                break;
        }
    }
}

// ─── DTO deserializado do JSON ─────────────────────────────────
public sealed class AstralCommand
{
    public string Action { get; set; } = "";
    public float? Value  { get; set; }
    public float? X      { get; set; }
    public float? Y      { get; set; }
}
