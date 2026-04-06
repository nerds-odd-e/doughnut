#!/usr/bin/env bash
set -euo pipefail

# Capture MinerU content_list for e2e_test/fixtures/book_reading/refactoring.pdf into
# mineru_output_for_refactoring.captured.json (not the E2E canonical fixture).
#
# When to re-run:
#   - refactoring.pdf changed
#   - MinerU / mineru[pipeline] version bump and you need fresh outline geometry
#   - Preparing sp-2.2: promote this file to mineru_output_for_refactoring.json after updating E2E
#
# Prerequisite (real MinerU — not e2e_test/python_stubs/mineru_site on PYTHONPATH):
#   python3 -m venv .venv-mineru
#   source .venv-mineru/bin/activate
#   pip install 'mineru[pipeline]'
#
# From repo root:
#   ./e2e_test/fixtures/book_reading/regenerate_mineru_output_for_refactoring.sh
#
# E2E keeps using mineru_output_for_refactoring.json until sp-2.2.

here=$(cd "$(dirname "$0")" && pwd)
repo_root=$(cd "$here/../../.." && pwd)
pdf_path=$repo_root/e2e_test/fixtures/book_reading/refactoring.pdf
outline_script=$repo_root/cli/python/mineru_book_outline.py
out_json=$here/mineru_output_for_refactoring.captured.json
stem=refactoring

if [[ ! -f $pdf_path ]]; then
  echo "error: missing PDF: $pdf_path" >&2
  exit 1
fi
if [[ ! -f $outline_script ]]; then
  echo "error: missing $outline_script" >&2
  exit 1
fi

if ! env -u PYTHONPATH python3 -c 'import mineru' 2>/dev/null; then
  echo "error: cannot import mineru. Activate .venv-mineru and: pip install 'mineru[pipeline]'" >&2
  echo "       (use a clean env; do not set PYTHONPATH to the E2E mineru stub.)" >&2
  exit 1
fi

tmp=$(mktemp -d)
cleanup() {
  rm -rf "$tmp"
}
trap cleanup EXIT

env -u PYTHONPATH python3 "$outline_script" "$pdf_path" --output-dir "$tmp" --json-result >/dev/null

cl_path=
if [[ -f $tmp/$stem/auto/${stem}_content_list.json ]]; then
  cl_path=$tmp/$stem/auto/${stem}_content_list.json
else
  cl_path=$(find "$tmp" -type f -name "${stem}_content_list.json" 2>/dev/null | head -n1 || true)
fi
if [[ -z $cl_path ]]; then
  cl_path=$(find "$tmp" -type f -name '*_content_list.json' 2>/dev/null | head -n1 || true)
fi
if [[ -z $cl_path || ! -f $cl_path ]]; then
  echo "error: no *_content_list.json under MinerU output in $tmp" >&2
  exit 1
fi

env -u PYTHONPATH python3 -c '
import json
import sys

src, dst = sys.argv[1], sys.argv[2]
data = json.loads(open(src, encoding="utf-8").read())
if not isinstance(data, list):
    print("error: content_list root must be a JSON array", file=sys.stderr)
    sys.exit(1)
open(dst, "w", encoding="utf-8").write(
    json.dumps(data, ensure_ascii=False, indent=2) + "\n"
)
' "$cl_path" "$out_json"

echo "wrote $out_json"
