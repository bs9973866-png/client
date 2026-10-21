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

## Notes
This repository keeps source in the repository root. The generated icon is decoded from `icons/nightfall-icon-64.png.b64` by the build workflow before Gradle runs. This is a client-side mod; it does not modify a server.
