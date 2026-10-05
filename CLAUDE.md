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

Single-class plugin. Two files do everything:

- `src/main/resources/META-INF/plugin.xml` — registers the `Claude` tool window via the `com.intellij.toolWindow` extension point. Depends on the bundled `org.jetbrains.plugins.terminal` plugin.
- `src/main/kotlin/com/example/claudepanel/ClaudeToolWindowFactory.kt` — `ToolWindowFactory` that uses `LocalTerminalDirectRunner.createTerminalRunner(project).createTerminalWidget(...)` to embed a terminal widget into the tool window's content, then calls `widget.executeCommand("claude")` to launch the CLI on first open.

The shell used is whatever PhpStorm's Settings → Tools → Terminal → Shell path points to. `claude` must be on PATH.

Targets IntelliJ Platform 2024.3 (`sinceBuild = "243"`, `untilBuild = null`). Kotlin JVM toolchain 21. Uses the `org.jetbrains.intellij.platform` Gradle plugin (v2.x) — note this is the newer plugin, APIs differ from the legacy `org.jetbrains.intellij`.

## Known quirk

If `executeCommand` fires before the shell is ready on first launch, the `claude` command may not run. Workaround: close and re-open the tool window. Any fix should account for terminal-widget readiness before invoking `executeCommand`.
