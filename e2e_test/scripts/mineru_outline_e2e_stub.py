#!/usr/bin/env python3
"""
E2E / CI stub for DOUGHNUT_MINERU_OUTLINE_SCRIPT.

Mirrors argv of minerui-spike/spike_mineru_phase_a_outline.py (Phase B contract):
  python3 this_script.py <book> --json-result [--output-dir DIR] [--start-page N] [--end-page N]

- Validates the book path exists and is readable; extension .pdf or .epub.
- Does not read PDF/EPUB bytes for structure (fixtures may be minimal PDFs).
- Prints one JSON object on stdout: ok/outline/source plus optional `layout` for attach-book (Phase 3).
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ANCHOR_FMT = "pdf.mineru_outline_v1"


def _anchor_stub(key: str) -> dict[str, str]:
    return {"anchorFormat": ANCHOR_FMT, "value": json.dumps({"stub": key})}


def _layout_payload() -> dict:
    """Nested tree matching POST …/attach-book `layout.roots` / children."""
    return {
        "roots": [
            {
                "title": "Stub Part A",
                "startAnchor": _anchor_stub("part-a-start"),
                "endAnchor": _anchor_stub("part-a-end"),
                "children": [
                    {
                        "title": "Stub Section One",
                        "startAnchor": _anchor_stub("sec-1-start"),
                        "endAnchor": _anchor_stub("sec-1-end"),
                    }
                ],
            },
            {
                "title": "Stub Part B",
                "startAnchor": _anchor_stub("part-b-start"),
                "endAnchor": _anchor_stub("part-b-end"),
            },
        ]
    }


def _emit(obj: dict) -> None:
    print(json.dumps(obj, ensure_ascii=False), flush=True)


def main() -> int:
    p = argparse.ArgumentParser(
        description="E2E stub: fake MinerU JSON outline + layout (no MinerU)."
    )
    p.add_argument("book", type=Path, help="Path to .pdf or .epub")
    p.add_argument(
        "--output-dir",
        type=Path,
        default=None,
        help="Ignored; accepted for argv compatibility with PDF pipeline",
    )
    p.add_argument(
        "--start-page",
        type=int,
        default=0,
        help="Ignored; accepted for argv compatibility",
    )
    p.add_argument(
        "--end-page",
        type=int,
        default=None,
        help="Ignored; accepted for argv compatibility",
    )
    p.add_argument(
        "--json-result",
        action="store_true",
        help="Required contract: print one JSON object on stdout",
    )
    args = p.parse_args()

    if not args.json_result:
        print("error: --json-result is required", file=sys.stderr)
        return 1

    book_path = args.book.expanduser().resolve()
    if not book_path.is_file():
        _emit({"ok": False, "error": f"file not found: {book_path}"})
        return 1

    try:
        with book_path.open("rb") as f:
            f.read(1)
    except OSError as e:
        _emit({"ok": False, "error": f"not readable: {book_path} ({e})"})
        return 1

    suffix = book_path.suffix.lower()
    if suffix not in (".pdf", ".epub"):
        _emit({"ok": False, "error": f"expected .pdf or .epub, got {suffix!r}"})
        return 1

    outline_lines = [
        "[L1 p0] Stub Part A",
        "[L2 p0] Stub Section One",
        "[L1 p0] Stub Part B",
    ]
    _emit(
        {
            "ok": True,
            "outline": "\n".join(outline_lines),
            "source": "e2e_stub",
            "layout": _layout_payload(),
        }
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
