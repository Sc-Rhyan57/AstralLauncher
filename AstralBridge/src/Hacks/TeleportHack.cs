using HarmonyLib;
using UnityEngine;
using AstralBridge.Bridge;

namespace AstralBridge.Hacks;

/// <summary>
/// Teleport hack — hookeia PlayerControl.FixedUpdate.
/// 
/// API confirmada por:
///   - Reactor docs: NetTransform.RpcSnapTo(Vector2) sincroniza posição
///   - MalumMenu/TownOfUs: padrão teleport no BepInEx IL2CPP
///   - AmongUsMenu C++ (BitCrackers): equivalente nativo via NetTransform
///
/// NetTransform.RpcSnapTo envia RPC para todos os clientes e
/// também move localmente — é a forma correta de teleportar
/// sem dessincronização.
/// </summary>
[HarmonyPatch(typeof(PlayerControl), nameof(PlayerControl.FixedUpdate))]
public static class TeleportPatch
{
    public static void Postfix(PlayerControl __instance)
    {
        if (!__instance.AmOwner) return;

        var target = Plugin.State.TakeAndClearTeleport();
        if (target == null) return;

        // NetTransform sincroniza a posição com todos os outros clientes
        __instance.NetTransform.RpcSnapTo(target.Value);

        Plugin.Log.LogInfo($"[Teleport] → {target.Value}");
    }
}
