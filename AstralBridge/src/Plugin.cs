using BepInEx;
using BepInEx.Logging;
using BepInEx.Unity.IL2CPP;
using HarmonyLib;
using AstralBridge.Bridge;
using AstralBridge.Core;

namespace AstralBridge;

[BepInPlugin(PluginInfo.GUID, PluginInfo.NAME, PluginInfo.VERSION)]
public sealed class Plugin : BasePlugin
{
    // ─── Singletons acessados pelos patches ────────────────────
    internal static ManualLogSource Log    { get; private set; } = null!;
    internal static Harmony         _harmony { get; private set; } = null!;
    internal static CommandServer   Server  { get; private set; } = null!;
    internal static HackState       State   { get; } = new();

    public override void Load()
    {
        Log     = base.Log;
        _harmony = new Harmony(PluginInfo.GUID);
        _harmony.PatchAll();

        Server = new CommandServer(7777);
        Server.Start();

        Log.LogInfo($"[{PluginInfo.NAME}] v{PluginInfo.VERSION} carregado — bridge TCP em :7777");
    }

    public override bool Unload()
    {
        Server?.Stop();
        _harmony?.UnpatchSelf();
        Log.LogInfo($"[{PluginInfo.NAME}] descarregado.");
        return base.Unload();
    }
}
