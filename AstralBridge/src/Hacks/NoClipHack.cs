using HarmonyLib;
using UnityEngine;

namespace AstralBridge.Hacks;

/// <summary>
/// NoClip — desativa as colisões do jogador local com o mapa.
///
/// API confirmada por:
///   - MalumMenu: toggle de Collider2D.enabled no player local
///   - ModMenuCrew: abordagem equivalente via Physics2D.IgnoreLayerCollision
///   - Approach: hookeia PlayerPhysics.FixedUpdate para garantir que
///     o layer "Ship" não colida com o jogador quando ativo.
///
/// Layer "Ship" (5) é onde as paredes do mapa ficam no Among Us.
/// Usamos Physics2D.IgnoreLayerCollision em vez de desativar colliders
/// individuais — mais robusto e não quebra interações de task.
/// </summary>
[HarmonyPatch(typeof(PlayerPhysics), nameof(PlayerPhysics.FixedUpdate))]
public static class NoClipPatch
{
    private static bool _lastState = false;

    public static void Prefix(PlayerPhysics __instance)
    {
        if (!__instance.myPlayer.AmOwner) return;

        bool current = Plugin.State.NoClipEnabled;

        // Só atualiza quando o estado muda (evita overhead por frame)
        if (current == _lastState) return;
        _lastState = current;

        int playerLayer = LayerMask.NameToLayer("Players");
        int shipLayer   = LayerMask.NameToLayer("Ship");
        int objectsLayer = LayerMask.NameToLayer("Objects");

        if (current)
        {
            // NoClip ON: ignora colisão entre o jogador e as paredes
            Physics2D.IgnoreLayerCollision(playerLayer, shipLayer,   ignore: true);
            Physics2D.IgnoreLayerCollision(playerLayer, objectsLayer, ignore: true);
            Plugin.Log.LogInfo("[NoClip] ON");
        }
        else
        {
            // NoClip OFF: restaura colisões
            Physics2D.IgnoreLayerCollision(playerLayer, shipLayer,   ignore: false);
            Physics2D.IgnoreLayerCollision(playerLayer, objectsLayer, ignore: false);
            Plugin.Log.LogInfo("[NoClip] OFF");
        }
    }
}
