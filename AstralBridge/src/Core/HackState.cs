using UnityEngine;

namespace AstralBridge.Core;

/// <summary>
/// Estado mutable compartilhado por todos os hacks.
/// Modificado pelo TCP server, lido pelos patches do Harmony.
/// Thread-safe via volatile / Interlocked onde necessário.
/// </summary>
public sealed class HackState
{
    // ─── Speed ───────────────────────────────────────────────
    public volatile bool  SpeedEnabled    = false;
    public float          SpeedMultiplier = 2.5f;
    private float         _originalSpeed  = -1f;

    public float GetOrSaveOriginalSpeed(float current)
    {
        if (_originalSpeed < 0f) _originalSpeed = current;
        return _originalSpeed;
    }
    public void ResetOriginalSpeed() => _originalSpeed = -1f;

    // ─── NoClip ───────────────────────────────────────────────
    public volatile bool NoClipEnabled = false;

    // ─── Kill ─────────────────────────────────────────────────
    public volatile bool KillCooldownEnabled = false;
    public volatile bool GodModeEnabled      = false;

    // ─── ESP / Reveal ─────────────────────────────────────────
    public volatile bool RevealEnabled = false;

    // ─── Vision ───────────────────────────────────────────────
    public volatile bool VisionEnabled     = false;
    public float         VisionMultiplier  = 10f;

    // ─── Tasks ────────────────────────────────────────────────
    // Quando true, o próximo FixedUpdate de NormalPlayerTask avança a task
    public volatile bool AutoTaskOnce = false;

    // ─── Teleport ─────────────────────────────────────────────
    // Lido e zerado atomicamente no patch. Usar lock para thread safety.
    private readonly object _tpLock = new();
    private Vector2? _teleportTarget;

    public void SetTeleport(Vector2 pos)
    {
        lock (_tpLock) { _teleportTarget = pos; }
    }

    public Vector2? TakeAndClearTeleport()
    {
        lock (_tpLock)
        {
            var v = _teleportTarget;
            _teleportTarget = null;
            return v;
        }
    }
}
