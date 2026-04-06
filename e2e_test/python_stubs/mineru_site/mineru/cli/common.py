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

_E2E_CONTENT_LIST_FIXTURE = (
    Path(__file__).resolve().parents[4]
    / "fixtures"
    / "book_reading"
    / "mineru_output_for_refactoring.json"
)

_E2E_CONTENT_LIST: list[dict[str, Any]] = json.loads(
    _E2E_CONTENT_LIST_FIXTURE.read_text(encoding="utf-8")
)


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
