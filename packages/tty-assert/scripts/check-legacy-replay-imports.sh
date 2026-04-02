#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$ROOT"
files=$(rg -l "from ['\"].*ptyTranscriptToVisiblePlaintext['\"]" --glob '*.ts' --glob '*.tsx' 2>/dev/null || true)
for f in $files; do
  case "$f" in
    packages/tty-assert/tests/ptyTranscriptReplayParity.test.ts) ;;
    packages/tty-assert/tests/ptyTranscriptToVisiblePlaintext.test.ts) ;;
    *)
      echo "Forbidden import of legacy ptyTranscriptToVisiblePlaintext (allowlist: tty-assert parity/unit tests only): $f" >&2
      exit 1
      ;;
  esac
done
