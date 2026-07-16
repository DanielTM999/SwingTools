#!/usr/bin/env bash
set -euo pipefail

if [ -z "${JAVA_HOME:-}" ]; then
    echo "ERROR: JAVA_HOME is not set" >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/build"
mkdir -p "$OUT_DIR"
OUT_FILE="$OUT_DIR/libgraphicsgl.so"

g++ -std=c++17 -O2 -fPIC -shared \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
    -I"$SCRIPT_DIR/../shared" \
    "$SCRIPT_DIR/GraphicsGl.cpp" "$SCRIPT_DIR/../shared/GlBindings.cpp" \
    -o "$OUT_FILE" \
    -L"$JAVA_HOME/lib" -ljawt -lGL -lX11 -ldl

echo "Built: $OUT_FILE"
