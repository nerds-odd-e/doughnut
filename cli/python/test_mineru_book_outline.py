"""Unit tests for mineru_book_outline layout extraction (no MinerU runtime)."""

from __future__ import annotations

import importlib.util
import json
import unittest
from pathlib import Path


def _load_mineru_book_outline():
    path = Path(__file__).resolve().parent / "mineru_book_outline.py"
    spec = importlib.util.spec_from_file_location("mineru_book_outline", path)
    assert spec and spec.loader
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


mbo = _load_mineru_book_outline()


class LayoutRootsWithContentBlocksTest(unittest.TestCase):
    def test_orphan_prefix_then_heading_yields_beginning_first(self) -> None:
        data = [
            {"type": "text", "text": "Orphan body", "bbox": [10, 100, 200, 130], "page_idx": 0},
            {
                "type": "text",
                "text": "Chapter One",
                "text_level": 2,
                "bbox": [1, 200, 300, 240],
                "page_idx": 1,
            },
        ]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(len(roots), 3)
        self.assertEqual(roots[0]["title"], "*beginning*")
        self.assertEqual(len(roots[0]["contentBlocks"]), 1)
        self.assertEqual(roots[0]["contentBlocks"][0]["text"], "Orphan body")
        self.assertEqual(roots[1]["title"], "Chapter One")
        self.assertEqual(roots[1]["contentBlocks"], [])

    def test_synthetic_bbox_one_line_above_first_orphan(self) -> None:
        data = [
            {"type": "text", "text": "x", "bbox": [10.0, 100.0, 20.0, 130.0], "page_idx": 0},
            {"type": "text", "text": "H", "text_level": 1, "bbox": [0, 0, 1, 1], "page_idx": 0},
        ]
        roots = mbo.layout_roots_with_content_blocks(data)
        inner = json.loads(roots[0]["startAnchor"]["value"])
        self.assertEqual(inner["page_idx"], 0)
        self.assertEqual(inner["kind"], "beginning")
        self.assertEqual(inner["bbox"], [10.0, 70.0, 20.0, 100.0])

    def test_synthetic_bbox_clamps_y0_at_zero(self) -> None:
        data = [
            {"type": "text", "text": "x", "bbox": [0, 15, 100, 40], "page_idx": 0},
            {"type": "text", "text": "H", "text_level": 1, "bbox": [0, 0, 1, 1], "page_idx": 0},
        ]
        roots = mbo.layout_roots_with_content_blocks(data)
        inner = json.loads(roots[0]["startAnchor"]["value"])
        self.assertEqual(inner["bbox"], [0.0, 0.0, 100.0, 15.0])

    def test_no_orphan_prefix_matches_heading_first_layout(self) -> None:
        data = [
            {
                "type": "text",
                "text": "Code Refactoring",
                "text_level": 2,
                "bbox": [90, 72, 576, 115],
                "page_idx": 0,
            },
            {
                "type": "text",
                "text": "Refactoring is often explained",
                "bbox": [87, 135, 897, 219],
                "page_idx": 0,
            },
        ]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(len(roots), 1)
        self.assertEqual(roots[0]["title"], "Code Refactoring")
        self.assertEqual(len(roots[0]["contentBlocks"]), 1)
        self.assertIn("Refactoring", roots[0]["contentBlocks"][0]["text"])

    def test_orphan_only_yields_single_beginning_root(self) -> None:
        data = [{"type": "page_number", "text": "1", "page_idx": 0}]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(len(roots), 1)
        self.assertEqual(roots[0]["title"], "*beginning*")
        self.assertEqual(len(roots[0]["contentBlocks"]), 1)

    def test_refactoring_fixture_starts_with_heading_no_beginning_root(self) -> None:
        fixture = (
            Path(__file__).resolve().parents[2]
            / "e2e_test"
            / "fixtures"
            / "book_reading"
            / "mineru_output_for_refactoring.json"
        )
        raw = json.loads(fixture.read_text(encoding="utf-8"))
        roots = mbo.layout_roots_with_content_blocks(raw)
        self.assertGreater(len(roots), 0)
        self.assertEqual(roots[0]["title"], "Code Refactoring")


if __name__ == "__main__":
    unittest.main()
