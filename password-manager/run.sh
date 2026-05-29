#!/bin/bash
# Run script for SecureVault Password Manager
# Requires Java 11+ with JavaFX on the module path

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/password-manager-1.0.0.jar"

# Try to find JavaFX (common locations)
FX_PATHS=(
  "/usr/share/maven-repo/org/openjfx/javafx-controls/11"
  "/usr/share/openjfx/lib"
  "$HOME/.openjfx/lib"
  "$SCRIPT_DIR/javafx-lib"
)

FX_MODULE_PATH=""
for p in "${FX_PATHS[@]}"; do
  if [ -d "$p" ]; then
    FX_MODULE_PATH="$p"
    break
  fi
done

if [ -n "$FX_MODULE_PATH" ]; then
  # Build full module path from all FX jar directories
  FX_ALL="/usr/share/maven-repo/org/openjfx/javafx-controls/11:/usr/share/maven-repo/org/openjfx/javafx-fxml/11:/usr/share/maven-repo/org/openjfx/javafx-graphics/11:/usr/share/maven-repo/org/openjfx/javafx-base/11"
  java --module-path "$FX_ALL" \
       --add-modules javafx.controls,javafx.fxml \
       -jar "$JAR"
else
  # JavaFX bundled in the jar – try plain launch
  java -jar "$JAR"
fi
