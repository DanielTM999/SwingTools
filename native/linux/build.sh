#!/usr/bin/env bash
set -euo pipefail

if [ -z "${JAVA_HOME:-}" ]; then
    echo "ERROR: JAVA_HOME is not set" >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/build"
mkdir -p "$OUT_DIR"
OUT_FILE="$OUT_DIR/libosfilepicker.so"

if ! command -v pkg-config >/dev/null 2>&1; then
    echo "ERROR: pkg-config not installed" >&2
    exit 1
fi

GTK_CFLAGS=$(pkg-config --cflags gtk+-3.0)
GTK_LIBS=$(pkg-config --libs gtk+-3.0)

g++ -std=c++17 -O2 -fPIC -shared \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
    $GTK_CFLAGS \
    "$SCRIPT_DIR/OsFilePicker.cpp" \
    -o "$OUT_FILE" \
    $GTK_LIBS

echo "Built: $OUT_FILE"
