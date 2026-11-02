#!/usr/bin/env bash
set -euo pipefail

echo "=== Claude Panel Plugin Installer ==="

# Find newest PhpStorm installation
PHPSHORTM_DIR=$(find ~/.local/share/JetBrains/PhpStorm* -type d 2>/dev/null | sort -r | head -1)
if [[ -z "$PHPSHORTM_DIR" ]]; then
    echo "ERROR: No PhpStorm installation found in ~/.local/share/JetBrains/"
    exit 1
fi

PLUGINS_DIR="$PHPSHORTM_DIR/plugins"
echo "Target PhpStorm: $PHPSHORTM_DIR"

# Verify plugins directory exists and is writable
if [[ ! -d "$PLUGINS_DIR" ]]; then
    mkdir -p "$PLUGINS_DIR" || { echo "ERROR: Cannot create plugins directory $PLUGINS_DIR"; exit 1; }
fi

if [[ ! -w "$PLUGINS_DIR" ]]; then
    echo "ERROR: Plugins directory $PLUGINS_DIR is not writable"
    exit 1
fi

ZIP_FILE="/home/brad/repo/claude-panel/build/distributions/claude-panel-*.zip"
if [[ ! -f "$ZIP_FILE" ]]; then
    echo "ERROR: No plugin jar found at $ZIP_FILE. Run ./gradlew buildPlugin first."
    exit 1
fi

echo "Latest build: $(basename "$ZIP_FILE")"

# Build if needed
if ! export JAVA_HOME=/home/brad/tools/jdk/jdk-21.0.12.1+1 && cd /home/brad/repo/claude-panel && ./gradlew buildPlugin 2>/dev/null; then
    echo "ERROR: gradlew buildPlugin failed"
    exit 1
fi

echo "Build successful."

# Remove existing plugin
echo "Removing old plugin..."
rm -rf "$PLUGINS_DIR/claude-panel"
if [[ -d "$PLUGINS_DIR/claude-panel" ]]; then
    echo "ERROR: Failed to remove existing plugin. Check permissions."
    exit 1
fi
echo "Existing plugin removed."

# Install fresh
echo "Installing plugin..."
unzip -q "$ZIP_FILE" -d "$PLUGINS_DIR"
if [[ ! -d "$PLUGINS_DIR/claude-panel" ]]; then
    echo "ERROR: Failed to unzip plugin. Check the jar file is valid."
    exit 1
fi

# Verify installation
if [[ -f "$PLUGINS_DIR/claude-panel/META-INF/plugin.xml" ]]; then
    echo "SUCCESS: Plugin installed to $PLUGINS_DIR/claude-panel"
else
    echo "ERROR: Plugin not found after extraction. Check for corruption."
    exit 1
fi

# Relaunch PhpStorm if possible
echo "Relaunching PhpStorm..."
if command -v phpstorm &>/dev/null; then
    phpstorm &
    echo "PhpStorm launched in background."
elif [[ -f /usr/local/bin/phpstorm ]]; then
    /usr/local/bin/phpstorm &
    echo "PhpStorm (toolbox) launched in background."
else
    echo "To finish, manually restart PhpStorm to load the plugin."
fi

echo "=== Installation complete ==="
