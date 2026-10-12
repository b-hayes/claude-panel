# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

PhpStorm/IntelliJ Platform plugin that adds a "Claude" tool window (right anchor) which embeds a terminal running the `claude` CLI, so the main Terminal tool window stays free.

## Build

```
./gradlew buildPlugin
```

Output: `build/distributions/claude-panel-<version>.zip`. Install via PhpStorm → Settings → Plugins → ⚙ → Install Plugin from Disk.

Run sandbox IDE for manual testing: `./gradlew runIde`.

Build needs JDK 21 (Gradle 8.10 rejects newer Java). Paths differ per machine — don't assume. Find a JDK 21: check `JAVA_HOME`, then `java -version`, then `~/tools/jdk/*`, `/usr/lib/jvm/*`, any JetBrains `*/jbr`. If none is version 21, install one (e.g. Temurin 21 into `~/tools/jdk`) and point `JAVA_HOME` at it.

## CLI reinstall into the real IDE

Faster than Install Plugin from Disk. Each plugin is its own folder/jar in the plugins dir, so swapping `claude-panel` leaves the others untouched.

All paths below vary per machine and IDE version — discover them, never hardcode:

- Plugins dir: glob `~/.local/share/JetBrains/PhpStorm*/` (pick the newest, or the one that already contains `claude-panel`). On macOS it's `~/Library/Application Support/JetBrains/PhpStorm*/`.
- Zip: `build/distributions/claude-panel-*.zip` (version varies).
- Launcher: `~/.local/share/JetBrains/Toolbox/scripts/phpstorm`, else the Toolbox app's `bin/phpstorm.sh`, else whatever is on PATH.

Flow: resolve those paths, then `rm -rf "$PLUGINS/claude-panel"`, `unzip -q <zip> -d "$PLUGINS"`, relaunch the IDE detached.

PhpStorm must be closed first — it rewrites plugin state on exit and will clobber the swap otherwise. Can't restart a running IDE from CLI.

After making code changes to this project, offer to build and run this reinstall flow (reminding the user to close PhpStorm first).

## Architecture

Single-window plugin with a small controller. Key files:

- `src/main/resources/META-INF/plugin.xml` — registers the `Claude` tool window via the `com.intellij.toolWindow` extension point. Depends on the bundled `org.jetbrains.plugins.terminal` plugin.
- `src/main/kotlin/com/example/claudepanel/ClaudeToolWindowFactory.kt` — creates a `ClaudePanelController` panel as the tool window content.
- `src/main/kotlin/com/example/claudepanel/ClaudePanelController.kt` — a `CardLayout` panel with two cards:
  - **menu** — buttons for the fixed agent commands (`claude`, `ollama launch claude`, `ollama launch pi`);
  - **terminal** — the live `ShellTerminalWidget` (created via `LocalTerminalDirectRunner.createTerminalRunner(project).createTerminalWidget(...)`, started with `widget.executeCommand(cmd)` after a short delay for shell readiness).

  A `TerminalWidgetListener.allSessionsClosed` listener switches back to the menu card when the process exits. An `IdeEventQueue.EventDispatcher` preserves the Escape (submit, `0x1b`) / Shift+Enter (bracketed paste) key bindings each session needs. Note: this 2024.3 target has no `JBButton` on the compile classpath — use `javax.swing.JButton`.

Targets IntelliJ Platform 2024.3 (`sinceBuild = "243"`, `untilBuild = null`). Kotlin JVM toolchain 21. Uses the `org.jetbrains.intellij.platform` Gradle plugin (v2.x) — note this is the newer plugin, APIs differ from the legacy `org.jetbrains.intellij`.

## Notes

The shell used is whatever PhpStorm's Settings → Tools → Terminal → Shell path points to; the chosen command must be on PATH. `executeCommand` must only be called once the widget's shell is ready — the controller delays the first command via a one-shot `javax.swing.Timer`. Kotlin quirk: `javax.swing.Timer(300)` alone fails overload resolution ("No value passed for parameter 'p1'") — pass the listener in the constructor: `Timer(300, ActionListener)`.
