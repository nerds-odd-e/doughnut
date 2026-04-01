#!/usr/bin/env python3
"""
Phase A spike: heading outline (layers 1–3) from PDF or EPUB.

PDF: MinerU `do_parse` (pipeline). Venv `.venv-mineru/bin/python`; install e.g.:
  .venv-mineru/bin/pip install 'mineru[pipeline]'

EPUB: MinerU does not accept EPUB (only PDF / images / Office). For `.epub` we walk
the OPF spine and collect <h1>–<h3> text in order (BeautifulSoup; pulled in with MinerU).

MinerU writes `{stem}_content_list.json` (not `document_content_list.json`).

Example:
  CURSOR_DEV=true nix develop -c .venv-mineru/bin/python minerui-spike/spike_mineru_phase_a_outline.py \\
    "/path/to/file.pdf" --end-page 4
  CURSOR_DEV=true nix develop -c .venv-mineru/bin/python minerui-spike/spike_mineru_phase_a_outline.py \\
    "/path/to/book.epub"
"""

from __future__ import annotations

import argparse
import json
import sys
import tempfile
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

from bs4 import BeautifulSoup

from mineru.cli.common import do_parse, read_fn

SUPPORTED_BOOK_SUFFIXES = frozenset({".pdf", ".epub"})


def _xml_local(tag: str) -> str:
    return tag.split("}", 1)[1] if tag.startswith("{") else tag


def _container_opf_path(z: zipfile.ZipFile) -> str:
    data = z.read("META-INF/container.xml")
    root = ET.fromstring(data)
    for el in root.iter():
        if _xml_local(el.tag) == "rootfile" and el.get("full-path"):
            return el.get("full-path").replace("\\", "/")
    raise ValueError("no rootfile full-path in META-INF/container.xml")


def _parse_opf_manifest_spine(opf_bytes: bytes) -> tuple[dict[str, str], list[str]]:
    root = ET.fromstring(opf_bytes)
    manifest: dict[str, str] = {}
    spine: list[str] = []
    for el in root.iter():
        tag = _xml_local(el.tag)
        if tag == "item" and el.get("id") and el.get("href"):
            manifest[el.get("id")] = el.get("href")
        elif tag == "itemref":
            if el.get("linear", "yes").lower() == "no":
                continue
            idref = el.get("idref")
            if idref:
                spine.append(idref)
    return manifest, spine


def _href_from_opf(opf_zip_path: str, href: str) -> str:
    base = Path(opf_zip_path).parent.as_posix()
    if not base or base == ".":
        return href.replace("\\", "/")
    return (Path(base) / href).as_posix()


def outline_from_epub(epub_path: Path) -> tuple[list[str], str]:
    lines: list[str] = []
    with zipfile.ZipFile(epub_path) as z:
        opf_path = _container_opf_path(z)
        manifest, spine = _parse_opf_manifest_spine(z.read(opf_path))
        for spine_idx, idref in enumerate(spine):
            href = manifest.get(idref)
            if not href:
                continue
            inner_path = _href_from_opf(opf_path, href)
            low = href.lower()
            if low.endswith(
                (".css", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".woff", ".ttf", ".otf", ".ncx", ".mp3", ".mp4")
            ):
                continue
            try:
                raw = z.read(inner_path)
            except KeyError:
                continue
            if not raw.strip():
                continue
            soup = BeautifulSoup(raw, "html.parser")
            for tag in soup.find_all(["h1", "h2", "h3"]):
                text = tag.get_text(" ", strip=True)
                if not text:
                    continue
                level = int(tag.name[1])
                lines.append(f"[L{level} s{spine_idx}] {text}")
    if lines:
        return lines, "epub (h1–h3 along OPF spine; MinerU does not parse EPUB)"
    return [], "no h1–h3 found in linear spine documents"


def find_content_list_json(output_dir: Path, stem: str) -> Path | None:
    direct = output_dir / stem / "auto" / f"{stem}_content_list.json"
    if direct.is_file():
        return direct
    matches = sorted(output_dir.rglob(f"{stem}_content_list.json"))
    if matches:
        return matches[0]
    any_list = sorted(output_dir.rglob("*_content_list.json"))
    return any_list[0] if any_list else None


def find_middle_json(output_dir: Path, stem: str) -> Path | None:
    direct = output_dir / stem / "auto" / f"{stem}_middle.json"
    if direct.is_file():
        return direct
    matches = sorted(output_dir.rglob(f"{stem}_middle.json"))
    return matches[0] if matches else None


def outline_from_content_list(path: Path) -> tuple[list[str], str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        return [], "content list is not a JSON array"
    lines: list[str] = []
    for item in data:
        if item.get("type") != "text":
            continue
        level = item.get("text_level")
        if level not in (1, 2, 3):
            continue
        text = (item.get("text") or "").strip()
        if not text:
            continue
        page = item.get("page_idx")
        page_s = f"p{page}" if page is not None else "p?"
        lines.append(f"[L{level} {page_s}] {text}")
    if lines:
        return lines, "content_list"
    return [], "no text blocks with text_level in {1,2,3}"


def _text_from_para_block(block: dict) -> str:
    parts: list[str] = []
    for line in block.get("lines") or []:
        for span in line.get("spans") or []:
            c = span.get("content", "")
            if c:
                parts.append(c)
    return "".join(parts).strip()


def outline_from_middle_json(path: Path) -> tuple[list[str], str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    pdf_info = data.get("pdf_info") or []
    lines: list[str] = []
    for page in pdf_info:
        page_idx = page.get("page_idx")
        for block in page.get("para_blocks") or []:
            if block.get("type") != "title":
                continue
            level = block.get("level", 1)
            if level not in (1, 2, 3):
                continue
            text = _text_from_para_block(block)
            if not text:
                continue
            page_s = f"p{page_idx}" if page_idx is not None else "p?"
            lines.append(f"[L{level} {page_s}] {text}")
    if lines:
        return lines, "middle.json (title blocks)"
    return [], "no title para_blocks with level in {1,2,3}"


def run_pdf(
    book_path: Path,
    stem: str,
    args: argparse.Namespace,
    out_dir: Path,
    cleanup_out: bool,
) -> int:
    pdf_bytes = read_fn(book_path)
    print(f"MinerU output directory: {out_dir}", file=sys.stderr)
    print(f"Book stem: {stem!r}  pages {args.start_page}..{args.end_page}", file=sys.stderr)
    try:
        do_parse(
            str(out_dir),
            [stem],
            [pdf_bytes],
            [args.lang],
            backend=args.backend,
            parse_method="auto",
            f_draw_layout_bbox=False,
            f_draw_span_bbox=False,
            f_dump_md=True,
            f_dump_middle_json=True,
            f_dump_model_output=False,
            f_dump_orig_pdf=False,
            f_dump_content_list=True,
            start_page_id=args.start_page,
            end_page_id=args.end_page,
        )
    except Exception as e:
        print(f"error: do_parse failed: {e}", file=sys.stderr)
        return 1

    cl_path = find_content_list_json(out_dir, stem)
    if not cl_path:
        print("error: could not find *_content_list.json under output dir", file=sys.stderr)
        return 1

    outline, source = outline_from_content_list(cl_path)
    if not outline:
        print(f"note: {source}; trying middle.json fallback", file=sys.stderr)
        mid_path = find_middle_json(out_dir, stem)
        if mid_path:
            outline, source = outline_from_middle_json(mid_path)
        else:
            print("error: no middle.json for fallback", file=sys.stderr)
            return 1

    if not outline:
        print("no heading layers detected (layers 1–3 empty).", file=sys.stderr)
        if cleanup_out:
            print(f"(output kept for inspection: {out_dir})", file=sys.stderr)
        return 0

    print(f"--- outline ({source}) ---")
    for line in outline:
        print(line)

    if cleanup_out:
        print(f"\n(output dir: {out_dir})", file=sys.stderr)
    return 0


def main() -> int:
    p = argparse.ArgumentParser(
        description="Phase A: heading outline (layers 1–3) from PDF (MinerU) or EPUB (spine h1–h3)."
    )
    p.add_argument("book", type=Path, help="Path to .pdf or .epub")
    p.add_argument("--output-dir", type=Path, default=None, help="MinerU output (PDF only; default: temp dir)")
    p.add_argument("--start-page", type=int, default=0, help="PDF only: first page index")
    p.add_argument(
        "--end-page",
        type=int,
        default=None,
        help="PDF only: last page index (inclusive); omit for full document",
    )
    p.add_argument("--lang", default="en", help="MinerU language code (PDF only; default en)")
    p.add_argument("--backend", default="pipeline", help="MinerU backend (PDF only; default pipeline)")
    args = p.parse_args()

    book_path = args.book.expanduser().resolve()
    if not book_path.is_file():
        print(f"error: file not found: {book_path}", file=sys.stderr)
        return 1

    suffix = book_path.suffix.lower()
    if suffix not in SUPPORTED_BOOK_SUFFIXES:
        print(f"error: expected .pdf or .epub, got {suffix!r}", file=sys.stderr)
        return 1

    stem = book_path.stem

    if suffix == ".epub":
        if args.start_page != 0 or args.end_page is not None:
            print("note: --start-page / --end-page apply to PDF only; ignored for EPUB", file=sys.stderr)
        outline, source = outline_from_epub(book_path)
        if not outline:
            print(f"{source}.", file=sys.stderr)
            return 0
        print(f"--- outline ({source}) ---")
        for line in outline:
            print(line)
        return 0

    out_dir = args.output_dir
    cleanup_out = False
    if out_dir is None:
        out_dir = Path(tempfile.mkdtemp(prefix="mineru-phase-a-"))
        cleanup_out = True
    else:
        out_dir = out_dir.expanduser().resolve()
        out_dir.mkdir(parents=True, exist_ok=True)

    return run_pdf(book_path, stem, args, out_dir, cleanup_out)


if __name__ == "__main__":
    raise SystemExit(main())
