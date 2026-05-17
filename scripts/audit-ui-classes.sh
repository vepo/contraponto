#!/usr/bin/env bash
# Report template CSS classes missing from style/*.css (informational).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMPLATES="$ROOT/src/main/resources/templates"
STYLES="$ROOT/src/main/resources/META-INF/resources/style"

classes=$(grep -roh 'class="[^"]*"' "$TEMPLATES" 2>/dev/null \
  | sed 's/class="//;s/"$//' \
  | tr ' ' '\n' \
  | grep -v '^{' \
  | sort -u)

selectors=$(grep -roh '\.[a-zA-Z][a-zA-Z0-9_-]*' "$STYLES"/*.css 2>/dev/null \
  | sed 's/^\.//' \
  | sort -u)

missing=0
while IFS= read -r c; do
  [[ -z "$c" ]] && continue
  if ! grep -qxF "$c" <<< "$selectors"; then
    echo "template class without rule: $c"
    missing=$((missing + 1))
  fi
done <<< "$classes"

echo "---"
echo "Template classes: $(echo "$classes" | grep -c . || true)"
echo "Missing rules: $missing"
exit 0
