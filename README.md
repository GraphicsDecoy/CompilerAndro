# CodeIDE — A lightweight VS Code-style IDE for Android

Mobile-first code editor + integrated terminal built with native **Kotlin + Jetpack Compose**.
Focus: **C/C++** authoring on Android, with Python / JS / Kotlin syntax support too.

![status](https://img.shields.io/badge/platform-Android%2024%2B-0a7aca)
![language](https://img.shields.io/badge/Kotlin-2.0-7F52FF)
![license](https://img.shields.io/badge/license-MIT-green)

## Features

- **VS Code-style UI** — dark theme, sidebar explorer, tabbed editor, status bar, terminal pane docked at the bottom
- **File / folder explorer** with long-press / kebab context menu: Open, New File, New Folder, Rename, Delete
- **Tabbed multi-file editor** — switch between open files, dirty indicator, close tabs
- **Syntax highlighting** for C/C++, C, Python, JavaScript/TypeScript, Kotlin (regex-based, zero native deps)
- **Integrated terminal** — interactive shell (`/system/bin/sh`), auto-detects and uses Termux toolchain when installed
- **Run / Build button** — saves current file and compiles/runs it in the terminal:
  - `.cpp` → `g++ -std=c++17 -O2 main.cpp -o main && ./main`
  - `.c`   → `gcc -O2 main.c -o main && ./main`
  - `.py`  → `python3 main.py`
  - `.js`  → `node main.js`
- **Developer keyboard bar** above IME — Esc, Tab, brackets, arrows, semicolons, `->`, etc.
- **Settings**:
  - Toggle **Show Android soft keyboard** (off = use the developer key bar / external keyboard only)
  - Font size, tab size, word wrap, syntax highlighting on/off
  - Toolchain status — shows detected paths of `g++`, `gcc`, `clang`, `python`, `node`, `cmake`, `make`
- **Lightweight** — no embedded compiler, no background services, pure Compose UI (APK ≈ 6-10 MB debug)

## Building

### Option 1 — GitHub Actions (recommended, zero setup)
Every push to any branch triggers `.github/workflows/build-apk.yml`:
1. Push this repo to GitHub
2. Open the **Actions** tab → latest run → download **CodeIDE-debug-apk** artifact
3. Transfer the APK to your phone and install (enable *Install unknown apps* for your browser/file manager)

Tag a release (`git tag v1.0.0 && git push --tags`) to auto-publish a GitHub Release with APKs attached.

### Option 2 — Android Studio
1. Open `android/` in Android Studio Giraffe or newer
2. Let Gradle sync (Kotlin 2.0.21, AGP 8.5.2, compileSdk 34)
3. Run ▶ on a device/emulator

### Option 3 — Command line
```bash
cd android
gradle wrapper --gradle-version 8.9
./gradlew :app:assembleDebug
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```

## Running C++ on device

CodeIDE does **not** bundle a compiler (that would violate the "lightweight" goal). It cooperates with **Termux**:

1. Install **Termux** from F-Droid (the Play Store version is outdated):
   https://f-droid.org/packages/com.termux/
2. Inside Termux, run once:
   ```
   pkg update
   pkg install clang python cmake make git nodejs
   ```
3. Open CodeIDE → *Settings* → you should now see the toolchain paths light up.
4. Tap **Run ▶** on any `.cpp` file — it compiles and executes in the bottom terminal.

If Termux is not installed, the terminal still works for any command available on stock Android (`ls`, `cat`, `echo`, etc.) — only compilation is gated.

## Project layout

```
android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/emergent/codeide/
│       │   ├── MainActivity.kt
│       │   ├── data/         (ViewModel, Settings DataStore)
│       │   ├── ui/
│       │   │   ├── editor/   (CodeEditor, Tabs, KeyboardShortcutBar)
│       │   │   ├── explorer/ (FileExplorer tree view)
│       │   │   ├── terminal/ (TerminalPane with interactive shell)
│       │   │   ├── settings/ (SettingsScreen)
│       │   │   ├── topbar/   (TopBar)
│       │   │   └── theme/    (Color, Theme, Type)
│       │   └── util/         (SyntaxHighlighter, ShellSession, ToolchainResolver)
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/wrapper/gradle-wrapper.properties
.github/workflows/build-apk.yml
```

## Permissions

- **MANAGE_EXTERNAL_STORAGE** (All files access) — required on Android 11+ to open arbitrary project folders outside the app's sandbox. First-time "Open Folder" prompts the system settings page.
- **INTERNET** — only used by the "Install Termux" link.
- **com.termux.permission.RUN_COMMAND** — optional; enables deeper Termux integration in future.

## Known limitations

- No native debugger (lldb/gdb). Build + run only.
- Symbol-aware autocomplete is not implemented (regex highlighting only). This keeps the APK tiny; plug-in language servers are a roadmap item.
- Undo/Redo currently ride on the TextField's built-in behaviour (long-press the text to access).

## License
MIT © 2026 — contributions welcome.
