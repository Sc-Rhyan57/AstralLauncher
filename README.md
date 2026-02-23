# ✦ Astral Launcher

Launcher Android para Among Us com suporte a mods BepInEx + bridge TCP para controle em tempo real pelo overlay.

## Estrutura

```
AstralLauncher/          ← Projeto Android (Kotlin + Compose)
  app/src/main/
    java/com/astrallauncher/
      bridge/            AstralBridgeClient.kt  ← TCP client
      network/           ModRepositoryApi.kt     ← download de mods
      service/           OverlayService.kt       ← overlay flutuante
      ui/screens/        HomeScreen.kt           ← tela principal
      util/              ApkPatcher, GameHelper, Prefs, AppLogger
      model/             Models.kt               ← data classes
      MainViewModel.kt
      MainActivity.kt

AstralBridge/            ← Plugin C# (BepInEx IL2CPP)
  src/
    Plugin.cs            ← entry point
    Bridge/CommandServer.cs  ← TCP server :7777
    Core/HackState.cs        ← estado dos hacks
    Hacks/
      SpeedHack.cs       ← PlayerSpeedMod
      TeleportHack.cs    ← NetTransform.RpcSnapTo
      KillHacks.cs       ← SetKillTimer + Die patch
      RevealHack.cs      ← IsImpostor + visão
      TasksHack.cs       ← NormalPlayerTask.NextStep
      NoClipHack.cs      ← Physics2D.IgnoreLayerCollision
```

## Como compilar o app Android

```bash
./gradlew assembleDebug
```

## Como compilar o plugin C#

Veja `AstralBridge/BUILD.md`.

## Fluxo completo

1. Compile `AstralBridge.dll` com `dotnet build -c Release`
2. Coloque o DLL em `app/src/main/assets/` (ou configure no patch)
3. Instale o Astral Launcher no Android
4. Abra o app → baixe mods → toque em **Patch & Instalar**
5. Abra o Among Us patcheado
6. A bolha dourada do overlay aparece → conecta automaticamente ao plugin
7. Execute scripts como `speed(2.5)` ou use os Quick Hacks
