using HarmonyLib;

namespace AstralBridge.Hacks;

/// <summary>
/// Auto-complete tasks (one-shot) — chama NextStep() até a task estar completa.
/// 
/// API confirmada por:
///   - MalumMenu: NormalPlayerTask.NextStep() avança um step da task
///   - Reactor docs: NormalPlayerTask.IsComplete e NormalPlayerTask.NextStep
///   - Abordagem: hookeia FixedUpdate de NormalPlayerTask para avançar
///     cada step no próximo frame quando ativado
///
/// AutoTaskOnce = true dispara UMA rodada de completar todas as tasks.
/// Após completar, reseta para false automaticamente.
/// </summary>
[HarmonyPatch(typeof(NormalPlayerTask), nameof(NormalPlayerTask.FixedUpdate))]
public static class AutoCompleteTasksPatch
{
    public static void Postfix(NormalPlayerTask __instance)
    {
        if (!Plugin.State.AutoTaskOnce) return;
        if (__instance.IsComplete) return;

        // Garante que é o jogador local
        if (__instance.myPlayer == null || !__instance.myPlayer.AmOwner) return;

        __instance.NextStep();
        Plugin.Log.LogInfo($"[Tasks] Step completado: {__instance.TaskType}");

        // Depois que todas tasks forem completas o HUD vai refletir
        // O flag é resetado pelo código após o loop de tasks
    }
}

/// <summary>
/// Reset do AutoTaskOnce após um frame — para que não continue completando
/// tasks no próximo round involuntariamente.
/// </summary>
[HarmonyPatch(typeof(HudManager), nameof(HudManager.Update))]
public static class TaskAutoResetPatch
{
    private static int _framesActive = 0;
    private const  int FRAMES_TO_KEEP = 5; // mantém ativo por 5 frames para garantir cobertura

    public static void Postfix()
    {
        if (!Plugin.State.AutoTaskOnce) { _framesActive = 0; return; }

        _framesActive++;
        if (_framesActive > FRAMES_TO_KEEP)
        {
            Plugin.State.AutoTaskOnce = false;
            _framesActive = 0;
            Plugin.Log.LogInfo("[Tasks] Auto-complete finalizado.");
        }
    }
}
