# ✦ Astral Launcher

> The open-source Among Us mod launcher for Android — compatible with Starlight mods, native .amod format, and Lua scripting.

[![Build APK](https://github.com/Sc-rhyan57/AstralLauncher/actions/workflows/build.yml/badge.svg)](https://github.com/Sc-rhyan57/AstralLauncher/actions/workflows/build.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

---

## Features

- **Mod Browser** — fetches mods from a GitHub RAW JSON repository
- **BepInEx / Starlight Compatible** — install `.dll` plugins compatible with Starlight mods
- **Native `.amod` Format** — Astral's own zip-based mod package format
- **Lua Scripting** — built-in code editor to write and execute Lua mods live
- **Floating Overlay** — a draggable info bubble while Among Us is running
- **Custom Servers** — add and manage Among Us custom server regions
- **Crash Reporter** — automatic crash log capture + copy to clipboard

---

## How Mod Injection Works

Astral Launcher injects mods into Among Us using **APK repackaging**:

1. Extract the Among Us APK (ZIP format)
2. Add the BepInEx `.dll` plugin into `assets/BepInEx/plugins/`
3. Repackage and sign the APK with a debug certificate
4. Prompt the user to install the patched APK

This is the same approach used by mods like [ImpostorModmenu](https://github.com/Devilx86/ImpostorModmenu).

Among Us on Android uses **IL2CPP + Unity**, so mods are BepInEx plugins targeting IL2CPP.

---

## Mod Repository JSON Format

Host your own repository by creating a JSON file at any public URL (GitHub RAW recommended):

```json
{
  "version": 1,
  "mods": [
    {
      "id": "my-mod",
      "name": "My Mod",
      "author": "YourName",
      "description": "Full description...",
      "short_description": "Short description",
      "version": "1.0.0",
      "game_version": "2026.x",
      "icon": "https://...",
      "banner": "https://...",
      "screenshots": ["https://..."],
      "tags": ["roles", "impostor"],
      "downloads": 0,
      "updated_at": "2026-01-01",
      "starlight_compatible": true,
      "astral_format": false,
      "supports_lua": false,
      "source_url": "https://github.com/...",
      "discord_url": "https://discord.gg/...",
      "releases": [
        {
          "version": "1.0.0",
          "url": "https://github.com/.../releases/download/v1.0.0/Mod.dll",
          "format": "dll",
          "size": 1048576,
          "checksum": "sha256:...",
          "game_version": "2026.x",
          "release_notes": "First release",
          "published_at": "2026-01-01"
        }
      ],
      "changelog": [
        {
          "version": "1.0.0",
          "date": "2026-01-01",
          "changes": ["Initial release"]
        }
      ]
    }
  ]
}
```

### Supported Mod Formats

| Format | Extension | Description |
|--------|-----------|-------------|
| `dll`  | `.dll`    | BepInEx IL2CPP plugin (Starlight-compatible) |
| `zip`  | `.zip`    | ZIP containing DLL(s) + assets |
| `amod` | `.amod`   | Astral native format (ZIP with `mod.json`, `plugins/`, `lua/`, `assets/`) |
| `lua`  | `.lua`    | Lua script mod (Astral-only) |

### .amod Package Structure

```
my-mod.amod (ZIP)
├── mod.json          ← ModEntry metadata
├── plugins/
│   └── MyMod.dll     ← BepInEx plugin DLL
├── lua/
│   └── helpers.lua   ← Optional Lua scripts
└── assets/
    └── textures/     ← Optional assets
```

---

## Building

### Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Build Debug APK
```bash
./gradlew :app:assembleDebug
```

### Build Release APK
```bash
export KEY_ALIAS=mykey
export KEY_PASSWORD=mypassword
./gradlew :app:assembleRelease
```

---

## Architecture

```
com.astrallauncher/
├── AstralApplication.kt       Application class
├── MainActivity.kt            Entry point, crash handler, nav
├── MainViewModel.kt           Central state management
├── model/
│   └── Models.kt              ModEntry, CustomServer, InstalledMod, etc.
├── network/
│   └── ModRepositoryApi.kt    GitHub RAW JSON fetcher + file downloader
├── service/
│   └── OverlayService.kt      Foreground service for floating bubble
├── ui/
│   ├── Theme.kt               Color palette (AL object)
│   ├── components/
│   │   └── Components.kt      Shared UI (TopBar, BottomBar, Cards, Buttons)
│   └── screens/
│       ├── HomeScreen.kt      Mod profiles + launch
│       ├── ExploreScreen.kt   Mod browser + detail view
│       ├── ServersScreen.kt   Custom server management
│       ├── ScriptScreen.kt    Lua editor + DLL injector + console
│       └── SettingsScreen.kt  Settings + About + GitHub
└── util/
    ├── ModInjector.kt         APK repackaging + injection logic
    └── Prefs.kt               DataStore preferences
```

---

## Contributing

Pull requests welcome! Please open an issue first to discuss major changes.

This project is **not affiliated** with Among Us or Innersloth LLC.

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE)
