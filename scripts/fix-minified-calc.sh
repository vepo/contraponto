#!/usr/bin/env bash
# Restore required whitespace around + in calc() after YUI Compressor.
# CSS Values: binary + in calc() must be surrounded by whitespace.
# Do not rewrite - ; YUI keeps minus spacing and hyphenated custom property names intact.
set -euo pipefail

fix_calc_plus_in_file() {
  local file="$1"
  perl -i -0777 -pe '
    sub fix_calc_plus {
      my ($inner) = @_;
      1 while $inner =~ s/(\)|[0-9a-zA-Z_%])(\+)(?=[0-9a-zA-Z_(var])/$1 $2 /g;
      return $inner;
    }
    s/calc\(((?:[^()]++|\([^()]*\))*+)\)/"calc(".fix_calc_plus($1).")"/ge;
  ' "$file"
}

assert_calc_plus_spacing() {
  local file="$1"
  if grep -E 'calc\([^)]*[0-9a-z%)](\+)[0-9a-z_(var]' "$file"; then
    echo "fix-minified-calc: invalid calc() + spacing remains in $file" >&2
    return 1
  fi
}

if [[ "$#" -lt 1 ]]; then
  echo "Usage: $0 <minified.css>..." >&2
  exit 1
fi

for file in "$@"; do
  fix_calc_plus_in_file "$file"
  assert_calc_plus_spacing "$file"
done
