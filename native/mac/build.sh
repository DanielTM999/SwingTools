#!/usr/bin/env bash
set -euo pipefail

if [ -z "${JAVA_HOME:-}" ]; then
    echo "ERROR: JAVA_HOME is not set" >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/build"
mkdir -p "$OUT_DIR"
OUT_FILE="$OUT_DIR/libosfilepicker.dylib"

clang++ -std=c++17 -O2 -fPIC -dynamiclib \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
    -framework Cocoa -framework AppKit \
    "$SCRIPT_DIR/OsFilePicker.mm" \
    -o "$OUT_FILE"

echo "Built: $OUT_FILE"
