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

## Architecture

Single-class plugin. Two files do everything:

- `src/main/resources/META-INF/plugin.xml` — registers the `Claude` tool window via the `com.intellij.toolWindow` extension point. Depends on the bundled `org.jetbrains.plugins.terminal` plugin.
- `src/main/kotlin/com/example/claudepanel/ClaudeToolWindowFactory.kt` — `ToolWindowFactory` that uses `LocalTerminalDirectRunner.createTerminalRunner(project).createTerminalWidget(...)` to embed a terminal widget into the tool window's content, then calls `widget.executeCommand("claude")` to launch the CLI on first open.

The shell used is whatever PhpStorm's Settings → Tools → Terminal → Shell path points to. `claude` must be on PATH.

Targets IntelliJ Platform 2024.3 (`sinceBuild = "243"`, `untilBuild = null`). Kotlin JVM toolchain 21. Uses the `org.jetbrains.intellij.platform` Gradle plugin (v2.x) — note this is the newer plugin, APIs differ from the legacy `org.jetbrains.intellij`.

## Known quirk

If `executeCommand` fires before the shell is ready on first launch, the `claude` command may not run. Workaround: close and re-open the tool window. Any fix should account for terminal-widget readiness before invoking `executeCommand`.
