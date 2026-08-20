#!/usr/bin/env bash
# Map IoT-related production classes after the foundation repo is cloned.
# Writes docs/foundation-inventory.md — this is the Phase 1 technical dependency map.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/foundation/poynt-cloudmessaging"
OUT="$ROOT/docs/foundation-inventory.md"

if [[ ! -d "$SRC" ]]; then
  echo "Foundation not cloned. Run ./scripts/clone-foundation.sh first."
  exit 1
fi

mkdir -p "$ROOT/docs"

{
  echo "# Foundation inventory"
  echo
  echo "Generated: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "Source: \`$SRC\`"
  echo
  echo "## Top-level layout"
  echo
  echo '```'
  ls -la "$SRC" | sed 's/^/ /'
  echo '```'
  echo
  echo "## Likely Gradle / Maven roots"
  echo
  find "$SRC" -maxdepth 3 \( -name 'settings.gradle*' -o -name 'build.gradle*' -o -name 'pom.xml' -o -name 'AndroidManifest.xml' \) \
    | sed 's|^| - |'
  echo
  echo "## IoT / MQTT / Discover / token / device hits"
  echo
  echo "These paths are the candidates to extract into \`shared/\` in Phase 2. Do not copy secrets."
  echo
} > "$OUT"

PATTERNS='IoT|Iot|MQTT|Mqtt|Discover|TokenManager|GdToken|AWS|AwsIot|CloudMessaging|PcmService|SharedPreferences'

if command -v rg >/dev/null 2>&1; then
  rg -n -i --glob '!**/build/**' --glob '!**/.git/**' --glob '!**/node_modules/**' \
    "$PATTERNS" "$SRC" \
    -g '*.java' -g '*.kt' -g '*.xml' -g '*.gradle' -g '*.kts' \
    | head -n 400 >> "$OUT" || true
else
  grep -RInE "$PATTERNS" \
    --include='*.java' --include='*.kt' --include='*.xml' --include='*.gradle' --include='*.kts' \
    "$SRC" 2>/dev/null | head -n 400 >> "$OUT" || true
fi

{
  echo
  echo "## Suggested next questions (fill after reading the inventory)"
  echo
  echo '```'
  echo "Where does IoT start?"
  echo "How is the device identified?"
  echo "How does Discover happen?"
  echo "Where is the token obtained?"
  echo "Where is it persisted?"
  echo "How is MQTT initialized?"
  echo "How does authentication happen?"
  echo "How are messages received?"
  echo "How does reconnect work?"
  echo '```'
} >> "$OUT"

echo "Wrote $OUT"
