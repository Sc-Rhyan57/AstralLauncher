using HarmonyLib;

namespace AstralBridge.Hacks;

/// <summary>
/// Kill Cooldown Zero — hookeia SetKillTimer para forçar timer = 0.
/// 
/// API confirmada por:
///   - MalumMenu: PlayerControl.SetKillTimer(float) patcheado com Harmony
///   - ModMenuCrew RoleCheats: killTimer = 0f no FixedUpdate
///   - Behavior: killTimer é o campo que o jogo verifica antes de permitir kill
/// </summary>
[HarmonyPatch(typeof(PlayerControl), nameof(PlayerControl.SetKillTimer))]
public static class KillCooldownZeroPatch
{
    public static bool Prefix(PlayerControl __instance, ref float time)
    {
        if (!__instance.AmOwner) return true;
        if (!Plugin.State.KillCooldownEnabled) return true;

        // Substitui completamente: timer = 0 imediatamente
        time = 0f;
        __instance.killTimer = 0f;
        return false; // bloqueia o método original
    }
}

/// <summary>
/// Kill Cooldown Zero — também garante killTimer=0 no CmdCheckMurder
/// para cobrir o caso em que o SetKillTimer não é chamado.
/// </summary>
[HarmonyPatch(typeof(PlayerControl), nameof(PlayerControl.CmdCheckMurder))]
public static class KillCooldownOnMurderPatch
{
    public static void Prefix(PlayerControl __instance)
    {
        if (!__instance.AmOwner) return;
        if (Plugin.State.KillCooldownEnabled)
            __instance.killTimer = 0f;
    }
}

/// <summary>
/// God Mode — hookeia PlayerControl.Die para bloquear morte.
/// 
/// API confirmada por:
///   - MalumMenu: patch em PlayerControl.Die retornando false (prefix)
///   - BitCrackers AmongUsMenu: mesmo approach via hook nativo
/// </summary>
[HarmonyPatch(typeof(PlayerControl), nameof(PlayerControl.Die))]
public static class GodModePatch
{
    public static bool Prefix(PlayerControl __instance)
    {
        if (!__instance.AmOwner) return true;
        if (!Plugin.State.GodModeEnabled) return true;

        Plugin.Log.LogInfo("[GodMode] Morte bloqueada.");
        return false; // não deixa o jogador morrer
    }
}
