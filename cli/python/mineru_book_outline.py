#!/usr/bin/env python3
"""
Doughnut CLI: heading outline (layers 1–3) from PDF or EPUB for attach-book layout extraction.

PDF: MinerU `do_parse` (pipeline). Example venv: `.venv-mineru/bin/pip install 'mineru[pipeline]'`.

EPUB: MinerU does not accept EPUB. For `.epub` we walk the OPF spine and collect <h1>–<h3> text
(BeautifulSoup — `pip install beautifulsoup4`; only loaded for `.epub`).

MinerU writes `{stem}_content_list.json` (not `document_content_list.json`).

With `--json-result`, stdout is one JSON object with `outline`, optional `note`, and `layout` with
nested `roots` / `children` for `POST …/attach-book`. Logs stay on stderr.

Example:
  python3 cli/python/mineru_book_outline.py "/path/to/file.pdf" --json-result --end-page 4
  python3 cli/python/mineru_book_outline.py "/path/to/book.epub" --json-result

E2E prepends `e2e_test/python_stubs/mineru_site` to `PYTHONPATH` to shadow `mineru` with a fake
`do_parse` (no real MinerU in CI).
"""

from __future__ import annotations

import argparse
import json
import sys
import tempfile
import zipfile
from pathlib import Path
from typing import Any
from xml.etree import ElementTree as ET

SUPPORTED_BOOK_SUFFIXES = frozenset({".pdf", ".epub"})


def _print_json_result(payload: dict) -> None:
    print(json.dumps(payload, ensure_ascii=False), flush=True)


def _emit_error(json_mode: bool, message: str) -> int:
    if json_mode:
        _print_json_result({"ok": False, "error": message})
    else:
        print(f"error: {message}", file=sys.stderr)
    return 1


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


def heading_records_from_epub(epub_path: Path) -> tuple[list[dict[str, Any]], str]:
    from bs4 import BeautifulSoup

    records: list[dict[str, Any]] = []
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
                records.append(
                    {
                        "level": level,
                        "title": text,
                        "source": "epub",
                        "spine_index": spine_idx,
                        "href": inner_path,
                    }
                )
    if records:
        return records, "epub (h1–h3 along OPF spine; MinerU does not parse EPUB)"
    return [], "no h1–h3 found in linear spine documents"


def outline_from_epub(epub_path: Path) -> tuple[list[str], str]:
    records, source = heading_records_from_epub(epub_path)
    if not records:
        return [], source
    lines = [f"[L{r['level']} s{r['spine_index']}] {r['title']}" for r in records]
    return lines, source


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


def _anchor(payload: dict[str, Any]) -> dict[str, str]:
    s = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    return {"value": s}


def _beginning_anchor_payload(first_orphan: dict[str, Any]) -> dict[str, Any]:
    payload: dict[str, Any] = {"kind": "beginning"}
    if first_orphan.get("page_idx") is not None:
        payload["page_idx"] = first_orphan["page_idx"]
    bbox = first_orphan.get("bbox")
    if isinstance(bbox, list) and len(bbox) == 4:
        try:
            x0, y0, x1, y1 = (float(b) for b in bbox)
            h = y1 - y0
            payload["bbox"] = [x0, max(0.0, y0 - h), x1, y0]
        except (TypeError, ValueError):
            pass
    return payload


def layout_roots_from_heading_records(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Build nested layout nodes (attach-book ``layout.roots``) from headings in reading order."""
    roots: list[dict[str, Any]] = []
    stack: list[tuple[int, dict[str, Any]]] = []
    for rec in records:
        level = int(rec["level"])
        title = str(rec["title"]).strip()
        if not title:
            continue
        payload = {k: rec[k] for k in ("page_idx", "bbox", "source", "spine_index", "href") if k in rec and rec[k] is not None}
        start_a = _anchor(payload if payload else {"kind": "heading"})
        node: dict[str, Any] = {
            "title": title,
            "startAnchor": start_a,
        }
        while stack and stack[-1][0] >= level:
            stack.pop()
        if not stack:
            roots.append(node)
        else:
            parent = stack[-1][1]
            parent.setdefault("children", []).append(node)
        stack.append((level, node))
    return roots


def layout_roots_with_content_blocks(data: list[Any]) -> list[dict[str, Any]]:
    """Build nested layout nodes with ordered contentBlocks from the full content_list.

    Each layout node's contentBlocks holds only non-heading MinerU items that belong
    to that section (body stream in reading order). The heading lives on the node's
    title and startAnchor only. Items before the first heading attach to a synthetic
    root block titled *beginning* (startAnchor one line height above the first orphan).
    """
    roots: list[dict[str, Any]] = []
    stack: list[tuple[int, dict[str, Any]]] = []
    beginning_node: dict[str, Any] | None = None

    def flush_beginning() -> None:
        nonlocal beginning_node
        if beginning_node is not None:
            roots.append(beginning_node)
            beginning_node = None

    for item in data:
        if not isinstance(item, dict):
            continue
        item_type = item.get("type")
        text_level = item.get("text_level") if item_type == "text" else None
        if item_type == "text" and text_level in (1, 2, 3):
            title = (item.get("text") or "").strip()
            if not title:
                continue
            flush_beginning()
            payload = {k: item[k] for k in ("page_idx", "bbox") if k in item and item[k] is not None}
            start_a = _anchor(payload if payload else {"kind": "heading"})
            node: dict[str, Any] = {
                "title": title,
                "startAnchor": start_a,
                "contentBlocks": [],
            }
            while stack and stack[-1][0] >= text_level:
                stack.pop()
            if not stack:
                roots.append(node)
            else:
                stack[-1][1].setdefault("children", []).append(node)
            stack.append((text_level, node))
        else:
            if stack:
                stack[-1][1]["contentBlocks"].append(item)
            else:
                if beginning_node is None:
                    beginning_node = {
                        "title": "*beginning*",
                        "startAnchor": _anchor(_beginning_anchor_payload(item)),
                        "contentBlocks": [],
                    }
                beginning_node["contentBlocks"].append(item)

    if beginning_node is not None:
        roots.append(beginning_node)

    return roots


def heading_records_from_content_list(data: list[Any]) -> tuple[list[dict[str, Any]], str | None]:
    records: list[dict[str, Any]] = []
    for item in data:
        if not isinstance(item, dict):
            continue
        if item.get("type") != "text":
            continue
        level = item.get("text_level")
        if level not in (1, 2, 3):
            continue
        text = (item.get("text") or "").strip()
        if not text:
            continue
        rec: dict[str, Any] = {"level": int(level), "title": text}
        if item.get("page_idx") is not None:
            rec["page_idx"] = item.get("page_idx")
        if item.get("bbox") is not None:
            rec["bbox"] = item.get("bbox")
        records.append(rec)
    if records:
        return records, None
    return [], "no text blocks with text_level in {1,2,3}"


def _text_from_para_block(block: dict) -> str:
    parts: list[str] = []
    for line in block.get("lines") or []:
        for span in line.get("spans") or []:
            c = span.get("content", "")
            if c:
                parts.append(c)
    return "".join(parts).strip()


def heading_records_from_middle_data(data: dict[str, Any]) -> tuple[list[dict[str, Any]], str | None]:
    records: list[dict[str, Any]] = []
    pdf_info = data.get("pdf_info") or []
    if not isinstance(pdf_info, list):
        return [], "pdf_info is not a list"
    for page in pdf_info:
        if not isinstance(page, dict):
            continue
        page_idx = page.get("page_idx")
        for block in page.get("para_blocks") or []:
            if not isinstance(block, dict):
                continue
            if block.get("type") != "title":
                continue
            level = block.get("level", 1)
            if level not in (1, 2, 3):
                continue
            text = _text_from_para_block(block)
            if not text:
                continue
            rec: dict[str, Any] = {"level": int(level), "title": text}
            if page_idx is not None:
                rec["page_idx"] = page_idx
            if block.get("bbox") is not None:
                rec["bbox"] = block.get("bbox")
            records.append(rec)
    if records:
        return records, None
    return [], "no title para_blocks with level in {1,2,3}"


def outline_from_content_list(path: Path) -> tuple[list[str], str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        return [], "content list is not a JSON array"
    records, err = heading_records_from_content_list(data)
    if err is not None:
        return [], err
    lines = [
        f"[L{rec['level']} {'p' + str(rec['page_idx']) if rec.get('page_idx') is not None else 'p?'}] {rec['title']}"
        for rec in records
    ]
    return lines, "content_list"


def outline_from_middle_json(path: Path) -> tuple[list[str], str]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        return [], "middle.json root is not an object"
    records, err = heading_records_from_middle_data(data)
    if err is not None:
        return [], err
    lines = [
        f"[L{rec['level']} {'p' + str(rec['page_idx']) if rec.get('page_idx') is not None else 'p?'}] {rec['title']}"
        for rec in records
    ]
    return lines, "middle.json (title blocks)"


def run_pdf(
    book_path: Path,
    stem: str,
    args: argparse.Namespace,
    out_dir: Path,
    cleanup_out: bool,
    json_mode: bool,
) -> int:
    from mineru.cli.common import do_parse, read_fn

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
        return _emit_error(json_mode, f"do_parse failed: {e}")

    cl_path = find_content_list_json(out_dir, stem)
    if not cl_path:
        return _emit_error(json_mode, "could not find *_content_list.json under output dir")

    records: list[dict[str, Any]] = []
    source = ""
    cl_note = ""
    try:
        raw_cl = json.loads(cl_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return _emit_error(json_mode, f"invalid content list JSON: {e}")

    if isinstance(raw_cl, list):
        recs, err = heading_records_from_content_list(raw_cl)
        if err is None:
            records = recs
            source = "content_list"
        else:
            cl_note = err
    else:
        cl_note = "content list is not a JSON array"

    if not records:
        print(f"note: {cl_note or 'no headings from content_list'}; trying middle.json fallback", file=sys.stderr)
        mid_path = find_middle_json(out_dir, stem)
        if not mid_path:
            return _emit_error(json_mode, "no middle.json for fallback")
        try:
            raw_mid = json.loads(mid_path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            return _emit_error(json_mode, f"invalid middle.json: {e}")
        if isinstance(raw_mid, dict):
            recs, err = heading_records_from_middle_data(raw_mid)
            if err is None:
                records = recs
                source = "middle.json (title blocks)"

    if not records:
        print("no heading layers detected (layers 1–3 empty).", file=sys.stderr)
        if cleanup_out:
            print(f"(output kept for inspection: {out_dir})", file=sys.stderr)
        if json_mode:
            _print_json_result(
                {
                    "ok": True,
                    "outline": "",
                    "source": "",
                    "note": "no heading layers detected (layers 1–3 empty)",
                }
            )
        return 0

    outline = [
        f"[L{r['level']} {'p' + str(r['page_idx']) if r.get('page_idx') is not None else 'p?'}] {r['title']}"
        for r in records
    ]
    if source == "content_list" and isinstance(raw_cl, list):
        layout_payload = {"roots": layout_roots_with_content_blocks(raw_cl)}
    else:
        layout_payload = {"roots": layout_roots_from_heading_records(records)}

    if json_mode:
        _print_json_result(
            {
                "ok": True,
                "outline": "\n".join(outline),
                "source": source,
                "layout": layout_payload,
            }
        )
    else:
        print(f"--- outline ({source}) ---")
        for line in outline:
            print(line)

    if cleanup_out:
        print(f"\n(output dir: {out_dir})", file=sys.stderr)
    return 0


def main() -> int:
    p = argparse.ArgumentParser(
        description="Heading outline (layers 1–3) from PDF (MinerU) or EPUB (spine h1–h3)."
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
    p.add_argument(
        "--json-result",
        action="store_true",
        help="Print one JSON object on stdout (CLI subprocess contract); logs stay on stderr",
    )
    args = p.parse_args()
    json_mode = args.json_result

    book_path = args.book.expanduser().resolve()
    if not book_path.is_file():
        return _emit_error(json_mode, f"file not found: {book_path}")

    suffix = book_path.suffix.lower()
    if suffix not in SUPPORTED_BOOK_SUFFIXES:
        return _emit_error(json_mode, f"expected .pdf or .epub, got {suffix!r}")

    stem = book_path.stem

    if suffix == ".epub":
        if args.start_page != 0 or args.end_page is not None:
            print("note: --start-page / --end-page apply to PDF only; ignored for EPUB", file=sys.stderr)
        epub_records, epub_note = heading_records_from_epub(book_path)
        if not epub_records:
            if json_mode:
                _print_json_result(
                    {
                        "ok": True,
                        "outline": "",
                        "source": "epub",
                        "note": epub_note,
                    }
                )
            else:
                print(f"{epub_note}.", file=sys.stderr)
            return 0
        outline = [f"[L{r['level']} s{r['spine_index']}] {r['title']}" for r in epub_records]
        layout_payload = {"roots": layout_roots_from_heading_records(epub_records)}
        if json_mode:
            _print_json_result(
                {
                    "ok": True,
                    "outline": "\n".join(outline),
                    "source": epub_note,
                    "layout": layout_payload,
                }
            )
        else:
            print(f"--- outline ({epub_note}) ---")
            for line in outline:
                print(line)
        return 0

    out_dir = args.output_dir
    cleanup_out = False
    if out_dir is None:
        out_dir = Path(tempfile.mkdtemp(prefix="doughnut-mineru-outline-"))
        cleanup_out = True
    else:
        out_dir = out_dir.expanduser().resolve()
        out_dir.mkdir(parents=True, exist_ok=True)

    return run_pdf(book_path, stem, args, out_dir, cleanup_out, json_mode)


if __name__ == "__main__":
    raise SystemExit(main())
