# claude-panel

PhpStorm plugin: dedicated tool window running `claude` instead of hijacking the Terminal tool window.

## Build

### Requirements

- JDK 21. Gradle 8.10 rejects newer Java (a Java 25 runtime fails the build), so a bundled IDE JBR is not usable unless it happens to be 21.
- Point `JAVA_HOME` at the JDK 21 for the build. Check the current one first:
  - Linux/macOS: `java -version`
  - Windows: `java -version` (any shell)
- If no JDK 21 is present, install Temurin 21 (https://adoptium.net/temurin/releases/?version=21) or any JDK 21, then set `JAVA_HOME` to it.

### Run the build

Linux/macOS:

```
JAVA_HOME=/path/to/jdk-21 ./gradlew buildPlugin
```

Windows (PowerShell):

```
$env:JAVA_HOME = "C:\path\to\jdk-21"; .\gradlew.bat buildPlugin
```

Windows (cmd):

```
set JAVA_HOME=C:\path\to\jdk-21 && gradlew.bat buildPlugin
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
