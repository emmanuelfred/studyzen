#!/usr/bin/env bash
# Prints how much of StudyZen's Kotlin/Swift code is shared by every platform.
# Counts non-blank lines. Run from the repository root:  bash scripts/shared-code-ratio.sh
set -euo pipefail
count() { find "$@" -type f \( -name '*.kt' -o -name '*.swift' \) -not -path '*/build/*' 2>/dev/null | xargs cat 2>/dev/null | grep -cv '^[[:space:]]*$' || true; }

shared=$(count shared/src/commonMain)
platform=$(( $(count shared/src/androidMain) + $(count shared/src/jvmMain) + $(count shared/src/iosMain) \
  + $(count shared/src/jsMain) + $(count shared/src/wasmJsMain) + $(count androidApp/src) \
  + $(count desktopApp/src) + $(count webApp/src) + $(count iosApp) ))

echo "Shared (commonMain):             $shared lines"
echo "Platform entry points and APIs:  $platform lines"
echo "Shared share of app code:        $(( shared * 100 / (shared + platform) ))%"
