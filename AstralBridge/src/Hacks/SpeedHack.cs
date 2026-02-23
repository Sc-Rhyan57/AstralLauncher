using HarmonyLib;
using AstralBridge.Bridge;

namespace AstralBridge.Hacks;

/// <summary>
/// Speed hack — hookeia PlayerControl.FixedUpdate (main thread Unity).
/// 
/// API confirmada por:
///   - blog tsuchinaga (PlayerControl.GameOptions.PlayerSpeedMod)
///   - MalumMenu source (mesma API)
///   - ModMenuCrew (GameOptionsManager.Instance.currentNormalGameOptions.PlayerSpeedMod)
/// 
/// Usamos PlayerControl.GameOptions que é o field estático equivalente
/// no IL2CPP interop do BepInEx 6.
/// </summary>
[HarmonyPatch(typeof(PlayerControl), nameof(PlayerControl.FixedUpdate))]
public static class SpeedHackPatch
{
    public static void Prefix(PlayerControl __instance)
    {
        // Drena comandos pendentes do TCP no main thread
        CommandServer.Drain(Plugin.State);

        // Aplica apenas ao jogador local
        if (!__instance.AmOwner) return;

        var state = Plugin.State;

        if (state.SpeedEnabled)
        {
            // PlayerControl.GameOptions é o IGameOptions estático
            // PlayerSpeedMod é o multiplicador de velocidade (default 1.0)
            var opts = PlayerControl.GameOptions;
            if (opts == null) return;

            float original = state.GetOrSaveOriginalSpeed(opts.PlayerSpeedMod);
            opts.PlayerSpeedMod = original * state.SpeedMultiplier;
        }
        else if (!state.SpeedEnabled)
        {
            // Restaura velocidade original quando desativado
            var opts = PlayerControl.GameOptions;
            if (opts != null && Plugin.State.SpeedMultiplier != 1f)
            {
                // Só restaura se tínhamos salvo o original
                // (evita resetar para 0 se nunca ativamos)
                float orig = Plugin.State.GetOrSaveOriginalSpeed(opts.PlayerSpeedMod);
                if (orig > 0) opts.PlayerSpeedMod = orig;
            }
            state.ResetOriginalSpeed();
        }
    }
}
