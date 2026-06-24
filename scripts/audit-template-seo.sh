#!/usr/bin/env bash
# Fail when templates violate SEO crawlability / meta-description conventions.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMPLATES="$ROOT/src/main/resources/templates"

missing_href=0
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  echo "$line"
  missing_href=$((missing_href + 1))
done < <(python3 - <<'PY'
import re
from pathlib import Path

root = Path("src/main/resources/templates")
pat = re.compile(r"<a\b[^>]*>", re.I)
for f in sorted(root.rglob("*.html")):
    for m in pat.finditer(f.read_text()):
        tag = m.group(0)
        if ("data-hx-get" in tag or " hx-get=" in tag) and "href=" not in tag.lower():
            print(f"{f}: {tag[:120]}")
PY
)

echo "---"
echo "Anchors with hx navigation but no href: $missing_href"
if [[ "$missing_href" -gt 0 ]]; then
  exit 1
fi
exit 0
