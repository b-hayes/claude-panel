# claude-panel

PhpStorm plugin: dedicated tool window running `claude` instead of hijacking the Terminal tool window.

## Build

```
./gradlew buildPlugin
```

Output: `build/distributions/claude-panel-0.1.0.zip`

## Install

PhpStorm → Settings → Plugins → ⚙ → Install Plugin from Disk → pick the zip → Restart.

A "Claude" tool window appears on the right. First open auto-runs `claude` in the project root.

## Notes

- Requires `claude` on PATH (same as your Terminal already does).
- Shell defaults to PhpStorm's Settings → Tools → Terminal → Shell path.
- Tested baseline: PhpStorm 2024.3+. Bump `phpstorm("...")` and `sinceBuild` in `build.gradle.kts` if you target newer.
- If `executeCommand` fires before the shell is ready on first launch, restart the tool window (gear → close, then re-open from View → Tool Windows → Claude).
