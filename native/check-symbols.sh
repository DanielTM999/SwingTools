#!/usr/bin/env bash
# Verifica que cada metodo `native` declarado no Java tem o simbolo JNI
# correspondente dentro dos binarios nativos commitados.
#
# Uso: bash native/check-symbols.sh <dir-da-plataforma> [<dir-da-plataforma> ...]
#   ex: bash native/check-symbols.sh src/main/resources/native/linux/amd64
#
# Funciona igual para .so, .dll e .dylib: le os simbolos direto dos bytes do
# arquivo, sem depender de nm/objdump/dumpbin.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAVA_SRC="$ROOT_DIR/src/main/java"

GL_PKG="dtm.stools.component.panels.graphics.gl"
PICKER_PKG="dtm.stools.component.inputfields.osfilepicker"

# Classes que cada biblioteca deve exportar: "<pacote>:<Classe>"
LIBS_graphicsgl="$GL_PKG:GL $GL_PKG:GlNative"
LIBS_osfilepicker="$PICKER_PKG:OsFilePicker"

# Nomes dos metodos `native` declarados num .java (um por linha).
java_native_methods() {
    grep -oE 'native[[:space:]]+[A-Za-z0-9_.]+(\[\])?[[:space:]]+[A-Za-z0-9_]+[[:space:]]*\(' "$1" \
        | sed -E 's/[[:space:]]*\($//' \
        | sed -E 's/.*[[:space:]]//'
}

# Simbolos Java_* presentes nos bytes de um binario (um por linha, unicos).
binary_jni_symbols() {
    grep -ao 'Java_[A-Za-z0-9_]*' "$1" 2>/dev/null | sort -u
}

failures=0
checked_any=0

check_dir() {
    local dir="${1%/}"
    if [ ! -d "$dir" ]; then
        echo "ERROR: nao e um diretorio: $dir" >&2
        failures=$((failures + 1))
        return
    fi

    local found_lib=0
    local base classes present expected_file

    for bin in "$dir"/*; do
        [ -f "$bin" ] || continue
        base="$(basename "$bin")"

        case "$base" in
            graphicsgl.dll|libgraphicsgl.so|libgraphicsgl.dylib) classes="$LIBS_graphicsgl" ;;
            osfilepicker.dll|libosfilepicker.so|libosfilepicker.dylib) classes="$LIBS_osfilepicker" ;;
            *) continue ;;
        esac

        found_lib=1
        checked_any=1
        present="$(binary_jni_symbols "$bin")"

        local missing=0 total=0
        for entry in $classes; do
            local pkg="${entry%%:*}"
            local cls="${entry##*:}"
            local src="$JAVA_SRC/${pkg//.//}/$cls.java"

            if [ ! -f "$src" ]; then
                echo "ERROR: fonte Java nao encontrado: $src" >&2
                failures=$((failures + 1))
                continue
            fi

            local prefix="Java_${pkg//./_}_${cls}_"
            while IFS= read -r method; do
                [ -n "$method" ] || continue
                total=$((total + 1))
                local sym="${prefix}${method}"
                # match exato, ou nome longo de overload (Java_Cls_m__Sig)
                if ! printf '%s\n' "$present" | grep -qx -e "$sym" && \
                   ! printf '%s\n' "$present" | grep -q "^${sym}__"; then
                    if [ "$missing" -eq 0 ]; then
                        echo "FAIL: $bin"
                    fi
                    echo "    faltando: $sym"
                    missing=$((missing + 1))
                fi
            done <<< "$(java_native_methods "$src")"
        done

        if [ "$missing" -eq 0 ]; then
            echo "OK:   $bin ($total simbolos JNI conferidos)"
        else
            echo "      -> $missing de $total simbolos ausentes; recompile este binario"
            failures=$((failures + 1))
        fi
    done

    if [ "$found_lib" -eq 0 ]; then
        echo "ERROR: nenhuma biblioteca nativa conhecida em $dir" >&2
        failures=$((failures + 1))
    fi
}

if [ "$#" -eq 0 ]; then
    echo "Uso: $0 <dir-da-plataforma> [...]" >&2
    exit 2
fi

for d in "$@"; do
    check_dir "$d"
done

if [ "$checked_any" -eq 0 ] && [ "$failures" -eq 0 ]; then
    echo "ERROR: nada foi verificado" >&2
    exit 1
fi

if [ "$failures" -ne 0 ]; then
    echo ""
    echo "Paridade de simbolos JNI FALHOU ($failures biblioteca(s))." >&2
    exit 1
fi

echo ""
echo "Paridade de simbolos JNI OK."
