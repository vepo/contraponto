#!/usr/bin/env bash
# Refresh vendored browser assets under src/main/resources/META-INF/resources/.
# Run from repo root: scripts/update-third-party-assets.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JS_DIR="$ROOT/src/main/resources/META-INF/resources/js/third-party"
CSS_DIR="$ROOT/src/main/resources/META-INF/resources/style/third-party"
LANG_DIR="$JS_DIR/languages"

HTMX_VERSION="2.0.10"
MARKED_VERSION="18.0.5"
HIGHLIGHT_VERSION="11.11.1"
ASCIIDOCTOR_VERSION="3.0.4"

mkdir -p "$LANG_DIR" "$CSS_DIR"

download() {
  local url="$1"
  local dest="$2"
  echo "→ $dest"
  curl -fsSL "$url" -o "$dest"
}

download "https://cdn.jsdelivr.net/npm/htmx.org@${HTMX_VERSION}/dist/htmx.min.js" \
  "$JS_DIR/htmx.min.js"

download "https://cdn.jsdelivr.net/npm/marked@${MARKED_VERSION}/lib/marked.umd.min.js" \
  "$JS_DIR/marked.min.js"

download "https://cdn.jsdelivr.net/npm/@asciidoctor/core@${ASCIIDOCTOR_VERSION}/dist/browser/asciidoctor.min.js" \
  "$JS_DIR/asciidoctor.min.js"

download "https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@${HIGHLIGHT_VERSION}/build/highlight.min.js" \
  "$JS_DIR/highlight.min.js"

download "https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@${HIGHLIGHT_VERSION}/build/styles/default.min.css" \
  "$CSS_DIR/default.min.css"

for lang in java yaml json bash graphql xml protobuf dockerfile; do
  download "https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@${HIGHLIGHT_VERSION}/build/languages/${lang}.min.js" \
    "$LANG_DIR/${lang}.min.js"
done

rm -f "$JS_DIR/htmx.js"

cat >"$JS_DIR/VERSIONS.txt" <<EOF
htmx.org ${HTMX_VERSION} (htmx.min.js)
marked ${MARKED_VERSION} (marked.min.js)
@asciidoctor/core ${ASCIIDOCTOR_VERSION} (asciidoctor.min.js)
highlight.js ${HIGHLIGHT_VERSION} (highlight.min.js, languages/*.min.js, default.min.css)
EOF

echo "Done. Versions recorded in js/third-party/VERSIONS.txt"
