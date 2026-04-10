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
        self.assertEqual(len(roots), 2)
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

    def test_orphan_with_valid_bbox_creates_beginning_root(self) -> None:
        data = [{"type": "text", "text": "intro", "bbox": [10, 100, 200, 130], "page_idx": 0}]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(len(roots), 1)
        self.assertEqual(roots[0]["title"], "*beginning*")
        self.assertEqual(len(roots[0]["contentBlocks"]), 1)

    def test_orphan_without_bbox_is_ignored(self) -> None:
        data = [{"type": "page_number", "text": "1", "page_idx": 0}]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(roots, [])

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


class IsValidBboxTest(unittest.TestCase):
    def test_valid_bbox(self) -> None:
        self.assertTrue(mbo._is_valid_bbox([0.0, 10.0, 100.0, 200.0]))

    def test_invalid_bbox_x0_equals_x1(self) -> None:
        self.assertFalse(mbo._is_valid_bbox([50.0, 10.0, 50.0, 200.0]))

    def test_invalid_bbox_x0_greater_than_x1(self) -> None:
        self.assertFalse(mbo._is_valid_bbox([100.0, 10.0, 50.0, 200.0]))

    def test_invalid_bbox_y0_equals_y1(self) -> None:
        self.assertFalse(mbo._is_valid_bbox([0.0, 100.0, 100.0, 100.0]))

    def test_invalid_bbox_y0_greater_than_y1(self) -> None:
        self.assertFalse(mbo._is_valid_bbox([0.0, 200.0, 100.0, 100.0]))

    def test_invalid_bbox_not_a_list(self) -> None:
        self.assertFalse(mbo._is_valid_bbox(None))
        self.assertFalse(mbo._is_valid_bbox("0,0,1,1"))

    def test_invalid_bbox_wrong_length(self) -> None:
        self.assertFalse(mbo._is_valid_bbox([1.0, 2.0, 3.0]))

    def test_invalid_bbox_non_numeric_element(self) -> None:
        self.assertFalse(mbo._is_valid_bbox([0, 0, "a", 1]))

    def test_invalid_bbox_infinite_value(self) -> None:
        self.assertFalse(mbo._is_valid_bbox([0.0, 0.0, float("inf"), 1.0]))

    def test_invalid_bbox_nan_value(self) -> None:
        self.assertFalse(mbo._is_valid_bbox([0.0, float("nan"), 100.0, 1.0]))


class LayoutRootsFilteringByBboxTest(unittest.TestCase):
    def test_heading_without_bbox_is_filtered_out(self) -> None:
        data = [{"type": "text", "text": "Chapter One", "text_level": 1, "page_idx": 0}]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(roots, [])

    def test_heading_without_page_idx_is_filtered_out(self) -> None:
        data = [{"type": "text", "text": "Chapter One", "text_level": 1, "bbox": [0, 0, 100, 50]}]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(roots, [])

    def test_heading_with_invalid_bbox_is_filtered_out(self) -> None:
        data = [
            {
                "type": "text",
                "text": "Chapter One",
                "text_level": 1,
                "page_idx": 0,
                "bbox": [100, 0, 50, 100],
            }
        ]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(roots, [])

    def test_valid_heading_is_included(self) -> None:
        data = [
            {
                "type": "text",
                "text": "Chapter One",
                "text_level": 1,
                "page_idx": 2,
                "bbox": [10, 50, 300, 90],
            }
        ]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(len(roots), 1)
        self.assertEqual(roots[0]["title"], "Chapter One")
        anchor = json.loads(roots[0]["startAnchor"]["value"])
        self.assertEqual(anchor["page_idx"], 2)
        self.assertEqual(anchor["bbox"], [10, 50, 300, 90])

    def test_beginning_node_not_created_when_orphan_has_no_bbox(self) -> None:
        data = [
            {"type": "page_number", "text": "1", "page_idx": 0},
            {"type": "text", "text": "Chapter One", "text_level": 1, "page_idx": 0, "bbox": [0, 50, 200, 80]},
        ]
        roots = mbo.layout_roots_with_content_blocks(data)
        self.assertEqual(len(roots), 1)
        self.assertEqual(roots[0]["title"], "Chapter One")


if __name__ == "__main__":
    unittest.main()
