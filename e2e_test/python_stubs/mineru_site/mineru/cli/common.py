"""
E2E-only fake MinerU: implements `read_fn` and `do_parse` used by
cli/python/mineru_book_outline.py. No models or GPU.

When e2e_test/python_stubs/mineru_site is prepended to PYTHONPATH, this module
replaces the real `mineru.cli.common` for the spike script subprocess.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

# Same heading sequence as the former mineru_outline_e2e_stub layout (book_reading E2E).
_E2E_CONTENT_LIST: list[dict[str, Any]] = [
    {"type": "text", "text_level": 1, "text": "Stub Part A", "page_idx": 0},
    {"type": "text", "text_level": 2, "text": "Stub Section One", "page_idx": 0},
    {"type": "text", "text_level": 1, "text": "Stub Part B", "page_idx": 0},
]


def read_fn(book_path: str | Path) -> bytes:
    return Path(book_path).read_bytes()


def do_parse(
    output_dir: str,
    pdf_file_names: list[str],
    pdf_bytes_list: list[bytes],
    lang_list: list[str],
    **kwargs: Any,
) -> None:
    del pdf_bytes_list, lang_list, kwargs
    if not pdf_file_names:
        return
    stem = pdf_file_names[0]
    out = Path(output_dir) / stem / "auto"
    out.mkdir(parents=True, exist_ok=True)
    target = out / f"{stem}_content_list.json"
    target.write_text(
        json.dumps(_E2E_CONTENT_LIST, ensure_ascii=False),
        encoding="utf-8",
    )
