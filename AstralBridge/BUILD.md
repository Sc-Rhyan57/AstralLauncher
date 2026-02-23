# AstralBridge — Guia de Compilação e Instalação

## O que é

Plugin BepInEx IL2CPP que roda **dentro** do processo do Among Us Android.
Abre um servidor TCP em `127.0.0.1:7777` que recebe comandos JSON do
**Astral Launcher** e aplica os hacks usando Harmony patches.

---

## Pré-requisitos

| Ferramenta | Versão | Link |
|---|---|---|
| .NET SDK | 6.x | https://dotnet.microsoft.com/download |
| BepInEx IL2CPP | 6.0.0-be.735 | https://builds.bepinex.dev/projects/bepinex_be |
| Among Us Android APK | 2024.x ou 2025.x | (seu APK) |
| dnSpyEx (opcional) | latest | https://github.com/dnSpyEx/dnSpy |

---

## Passo 1 — Obter Assembly-CSharp.dll

Você precisa do assembly real do jogo para compilar.

### Opção A — Do APK diretamente
```bash
# Extraia o APK (é um ZIP)
unzip -o "AmongUs.apk" "assets/bin/Data/Managed/*.dll" -d au_libs/

# Os DLLs ficam em:
# au_libs/assets/bin/Data/Managed/Assembly-CSharp.dll   ← principal
# au_libs/assets/bin/Data/Managed/Hazel.dll             ← rede
```

### Opção B — Reactor Discord (recomendado)
1. Entre em https://reactor.gg/discord
2. Vá no canal `#resources`
3. Baixe o pacote de assemblies da versão do AU que você usa
4. Extraia os DLLs

### Coloque os DLLs aqui:
```
AstralBridge/
  libs/
    Assembly-CSharp.dll    ← obrigatório
    Hazel.dll              ← obrigatório
```

---

## Passo 2 — Compilar

```bash
cd AstralBridge/
dotnet restore
dotnet build -c Release
# Saída: bin/Release/net6.0/AstralBridge.dll
```

---

## Passo 3 — Instalar no APK via Astral Launcher

O Astral Launcher já faz isso automaticamente durante o patch!

O fluxo de patch do Astral:
1. Baixa BepInEx IL2CPP para Android
2. Reempacota o APK injetando os arquivos BepInEx em `assets/`
3. Copia `AstralBridge.dll` para `assets/BepInEx/plugins/`
4. Assina e instala o APK modificado

**Coloque `AstralBridge.dll` em:**
```
Astral Launcher → assets/
  BepInEx/
    core/       ← runtime BepInEx
    plugins/
      AstralBridge.dll   ← o plugin compilado
```

---

## Passo 4 — Verificar funcionamento

1. Abra o Among Us com o APK patcheado
2. Abra o Astral Launcher → overlay aparece
3. A bolha dourada fica com badge **"LIVE"** quando conectado
4. Execute comandos no executor de scripts

---

## Comandos disponíveis (TCP JSON)

```json
{"action":"speed_on",  "value":2.5}
{"action":"speed_off"}
{"action":"speed_set", "value":3.0}
{"action":"noclip_on"}
{"action":"noclip_off"}
{"action":"kill_cooldown_on"}
{"action":"kill_cooldown_off"}
{"action":"godmode_on"}
{"action":"godmode_off"}
{"action":"reveal_on"}
{"action":"reveal_off"}
{"action":"vision_on", "value":10.0}
{"action":"vision_off"}
{"action":"tasks_complete"}
{"action":"teleport",  "x":0.0, "y":0.0}
```

## Sintaxe do executor de scripts (Astral overlay)

```lua
-- Comentários com --
speed(2.5)
noclip_on()
kill_cooldown_on()
reveal()
godmode()
vision(10)
tasks()
tp(0, 0)
speed_off()
noclip_off()
```

---

## Arquitetura

```
┌─────────────────────────────────────────────┐
│  Among Us Android (processo IL2CPP)         │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │  AstralBridge.dll (BepInEx plugin)  │    │
│  │  • Plugin.cs → Load() → PatchAll()  │    │
│  │  • SpeedHack  → PlayerControl patch │    │
│  │  • TeleportHack → NetTransform.Rpc  │    │
│  │  • KillHacks  → SetKillTimer patch  │    │
│  │  • RevealHack → HudManager patch    │    │
│  │  • TasksHack  → NormalPlayerTask    │    │
│  │  • NoClipHack → Physics2D layers    │    │
│  │                                     │    │
│  │  CommandServer (TCP :7777)          │    │
│  │  └─ ConcurrentQueue<AstralCommand>  │    │
│  └─────────────────────────────────────┘    │
│              ↕ 127.0.0.1:7777              │
└─────────────────────────────────────────────┘
              ↕ TCP socket local
┌─────────────────────────────────────────────┐
│  Astral Launcher (processo Android separado)│
│  • AstralBridgeClient.kt                    │
│  • OverlayService.kt → executor de scripts  │
│  • Quick Hacks buttons → comandos JSON      │
└─────────────────────────────────────────────┘
```
