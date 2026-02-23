using HarmonyLib;
using UnityEngine;

namespace AstralBridge.Hacks;

/// <summary>
/// Reveal Impostors — destaca impostores em vermelho.
/// 
/// API confirmada pela análise IL2CPP (0x64marsh.com/blog):
///   - GameData.AllPlayers → List<PlayerInfo>
///   - PlayerInfo.Role.IsImpostor → bool (getter)
///   - PlayerInfo.PlayerName → string
///   - PlayerControl.cosmetics.nameText → TextMeshPro
///
/// Aqui usamos o approach BepInEx IL2CPP (mais limpo que o nativo):
///   - PlayerControl.AllPlayerControls para iterar jogadores
///   - pc.Data.Role.IsImpostor para checar papel
///   - pc.cosmetics.nameText.color para colorir vermelho
/// </summary>
[HarmonyPatch(typeof(HudManager), nameof(HudManager.Update))]
public static class RevealImpostorsPatch
{
    public static void Postfix()
    {
        if (!Plugin.State.RevealEnabled) return;
        if (PlayerControl.AllPlayerControls == null) return;

        foreach (var pc in PlayerControl.AllPlayerControls)
        {
            if (pc == null || pc.Data == null || pc.Data.Role == null) continue;
            if (pc.cosmetics?.nameText == null) continue;

            if (pc.Data.Role.IsImpostor)
            {
                // Impostor: vermelho brilhante
                pc.cosmetics.nameText.color = Color.red;
            }
            else if (pc.cosmetics.nameText.color == Color.red)
            {
                // Restaura cor branca se foi colorido mas não é mais impostor
                pc.cosmetics.nameText.color = Color.white;
            }
        }
    }
}

/// <summary>
/// Visão ampliada — hookeia GetFloat nas opções de jogo.
/// 
/// API confirmada:
///   - IGameOptions.GetFloat(FloatOptionNames) retorna modificadores float
///   - FloatOptionNames.CrewLightMod / ImpostorLightMod controlam visão
///   - Approach usado pelo MalumMenu e TownOfUs
/// </summary>
[HarmonyPatch(typeof(IGameOptions), nameof(IGameOptions.GetFloat))]
public static class VisionHackPatch
{
    public static void Postfix(FloatOptionNames optionName, ref float __result)
    {
        if (!Plugin.State.VisionEnabled) return;

        if (optionName is FloatOptionNames.CrewLightMod or FloatOptionNames.ImpostorLightMod)
            __result = Plugin.State.VisionMultiplier;
    }
}
