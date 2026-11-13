#!/usr/bin/env bash
#
# Compiles and runs the JVM verification suite for the platform-independent JARVIS core.
#
# Requirements (all optional but recommended):
#   - a JDK (JAVA_HOME must point at it)
#   - kotlin-compiler + kotlinx-coroutines jars (KC_LIB must point at the directory
#     that holds kotlin-compiler.jar, kotlin-stdlib.jar and kotlinx-coroutines-core-jvm.jar)
#
# Usage: tools/kotlin-verify/run.sh [repo-root]
set -euo pipefail

REPO_ROOT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
KC_LIB="${KC_LIB:-/home/user/.tools/kc/node_modules/kotlin-compiler/lib}"
OUT_DIR="${OUT_DIR:-/tmp/jarvis-verify}"

if [[ ! -d "$KC_LIB" ]]; then
  echo "KC_LIB not found: $KC_LIB" >&2
  exit 2
fi

CORE_DIR="$REPO_ROOT/android/app/src/main/java"
TEST_DIR="$REPO_ROOT/tools/kotlin-verify/src"

mkdir -p "$OUT_DIR"

mapfile -t CORE_ONLY < <(find "$CORE_DIR/com/mdfoysal/jarvis/core" -name '*.kt' | sort)
mapfile -t TEST_SOURCES < <(find "$TEST_DIR" -name '*.kt' | sort)

echo "Compiling ${#CORE_ONLY[@]} core sources + ${#TEST_SOURCES[@]} verification sources ..." 

java -cp "$KC_LIB/kotlin-compiler.jar:$KC_LIB/kotlin-stdlib.jar:$KC_LIB/kotlin-reflect.jar" \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -nowarn \
  -cp "$KC_LIB/kotlin-stdlib.jar:$KC_LIB/kotlinx-coroutines-core-jvm.jar" \
  "${CORE_ONLY[@]}" "$CORE_DIR/com/mdfoysal/jarvis/data/remote/JarvisBackendClient.kt" "${TEST_SOURCES[@]}" \
  -d "$OUT_DIR/classes"

echo "Running verification suite ..."
java -cp "$OUT_DIR/classes:$KC_LIB/kotlin-stdlib.jar:$KC_LIB/kotlinx-coroutines-core-jvm.jar" \
  verify.VerifyMainKt "$REPO_ROOT"
