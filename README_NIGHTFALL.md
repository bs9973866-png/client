# NIGHTFALL — Minecraft 1.21.4 (Fabric)

Custom client-side Minecraft mod for Fabric 1.21.4, built with Java 21.

## Features
- Custom animated dark/purple main menu.
- ClickGUI, opened with **Right Shift**, grouped by category.
- HUD overlays: FPS, coordinates, keystrokes, watermark, target HUD, hitmarker, custom crosshair.
- Utility options: FullBright, Auto Sprint, Zoom (hold **C**), No Fall, player trail and optional singleplayer-only combat helpers.
- JSON configuration saved to `config/nightfall.json`.

## Build
GitHub Actions automatically builds on pushes. Open **Actions → Build NIGHTFALL**, then download the artifact named `nightfall-minecraft-1.21.4`. The output includes the built mod JAR.

## Install
Use Fabric Loader for Minecraft **1.21.4**, install Fabric API for 1.21.4, then place the built JAR in `.minecraft/mods`.

## Controls and troubleshooting
- **Right Shift** opens ClickGUI. Left-click a module to toggle it; right-click a module, then press a keyboard key to bind it. Press **Backspace** to clear a bind or **Esc** to cancel binding.
- Module binds are saved to `config/nightfall/binds.json`; module toggles remain in `config/nightfall.json`.
- **Right Ctrl** opens HUD Editor. Enable **Media Player** to show the current Windows media session in the HUD; press **K** to open the player screen and use Previous / Play-Pause / Next.
- **World Lyrics** uses synchronized LRCLIB lyrics for the active Windows media session. It now displays a syncing/error message instead of silently disappearing if lyrics are not yet available. Internet access is required for lyric lookup.
- **Auto Totem** keeps a totem in the offhand when one exists in the inventory. **KillAura** selects a visible valid target and smoothly rotates toward it without requiring the crosshair to already be on that target.

## Notes
This repository keeps source in the repository root. The generated icon is decoded from `icons/nightfall-icon-64.png.b64` by the build workflow before Gradle runs. This is a client-side mod; it does not modify a server.
