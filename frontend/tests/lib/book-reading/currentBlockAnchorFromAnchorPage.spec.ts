import { currentBlockAnchorIdFromAnchorPage } from "@/lib/book-reading/currentBlockAnchorFromAnchorPage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import { describe, expect, it } from "vitest"

/** `pageIndex` + optional `bbox` (four numbers when present); aligns with `PageBboxFull` when `bbox` is set. */
type FirstBboxInput = {
  pageIndex: number
  bbox?: readonly number[]
}

/** Matches `BookAnchorFullBuilder.topMathsLikePreorder()` — wire shape uses `allBboxes[0]`, not JSON `value`. */
type BlockFirstBboxRow = {
  id: number
  firstBbox?: FirstBboxInput
}

function asLegacyAnchorArg(
  rows: readonly BlockFirstBboxRow[]
): readonly BookAnchorFull[] {
  return rows as unknown as readonly BookAnchorFull[]
}

const topMathsLikePreorderRows: BlockFirstBboxRow[] = [
  { id: 101 },
  { id: 102, firstBbox: { pageIndex: 0, bbox: [48, 72, 564, 200] } },
  { id: 103, firstBbox: { pageIndex: 0, bbox: [48, 520, 564, 756] } },
  { id: 104 },
  { id: 105 },
  { id: 106 },
]

function vp(top: number, mid: number, bottom: number): ViewportYRange {
  return { top, mid, bottom }
}

describe("currentBlockAnchorIdFromAnchorPage", () => {
  it("null viewport returns last anchor on page", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0
      )
    ).toBe(103)
  })

  it("null viewport on page 1 returns last anchor on that page", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        1
      )
    ).toBe(106)
  })

  it("returns null for empty list", () => {
    expect(currentBlockAnchorIdFromAnchorPage([], 0)).toBe(null)
  })

  it("returns null when every anchor value is unparseable as outline v1", () => {
    const rows: BlockFirstBboxRow[] = [{ id: 1 }, { id: 2 }]
    expect(currentBlockAnchorIdFromAnchorPage(asLegacyAnchorArg(rows), 0)).toBe(
      null
    )
  })

  it("returns null for negative or non-integer anchor page", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        -1
      )
    ).toBe(null)
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        1.5
      )
    ).toBe(null)
  })

  it("earliest visible anchor is current when its top has passed viewport mid", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0,
        vp(0, 300, 600)
      )
    ).toBe(102)
  })

  it("earliest visible anchor whose top > mid → use previous anchor", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0,
        vp(200, 400, 600)
      )
    ).toBe(102)
  })

  it("earliest visible anchor with top below mid → is current", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0,
        vp(400, 600, 800)
      )
    ).toBe(103)
  })

  it("gap case: no visible anchor → last anchor with y0 ≤ mid (id=103)", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0,
        vp(210, 340, 380)
      )
    ).toBe(102)
  })

  it("gap case when viewport mid is past all anchors on page → last anchor", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0,
        vp(700, 800, 900)
      )
    ).toBe(103)
  })

  it("first visible has no-bbox predecessor (y0=0) whose top > mid → use no-bbox anchor", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0,
        vp(0, 50, 100)
      )
    ).toBe(101)
  })

  it("with pdfPageCount, skips anchors whose page_idx is beyond the document", () => {
    const rows: BlockFirstBboxRow[] = [
      { id: 1 },
      { id: 2, firstBbox: { pageIndex: 10 } },
    ]
    expect(
      currentBlockAnchorIdFromAnchorPage(asLegacyAnchorArg(rows), 1, null, 2)
    ).toBe(1)
  })

  it("with pdfPageCount, returns null when every parseable anchor is beyond the document", () => {
    const rows: BlockFirstBboxRow[] = [{ id: 2, firstBbox: { pageIndex: 10 } }]
    expect(
      currentBlockAnchorIdFromAnchorPage(asLegacyAnchorArg(rows), 1, null, 2)
    ).toBe(null)
  })

  it("with pdfPageCount, returns null when viewport page is out of range", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        5,
        null,
        3
      )
    ).toBe(null)
  })

  it("with invalid pdfPageCount, returns null", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0,
        null,
        0
      )
    ).toBe(null)
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(topMathsLikePreorderRows),
        0,
        null,
        1.5
      )
    ).toBe(null)
  })

  it("partial book layout: only anchors with valid outline v1 values participate", () => {
    const rows: BlockFirstBboxRow[] = [{ id: 1 }, { id: 2 }, { id: 77 }]
    expect(currentBlockAnchorIdFromAnchorPage(asLegacyAnchorArg(rows), 0)).toBe(
      77
    )
  })

  it("returns null when values parse as JSON but fail outline v1 validation", () => {
    const rows: BlockFirstBboxRow[] = [
      { id: 1 },
      { id: 2, firstBbox: { pageIndex: 0, bbox: [48, 72] } },
    ]
    expect(currentBlockAnchorIdFromAnchorPage(asLegacyAnchorArg(rows), 0)).toBe(
      null
    )
  })

  it("close-nodes: first visible partially above viewport, next within SCROLL_PADDING → use next", () => {
    const rows: BlockFirstBboxRow[] = [
      { id: 10, firstBbox: { pageIndex: 0, bbox: [48, 400, 100, 450] } },
      { id: 20, firstBbox: { pageIndex: 0, bbox: [48, 478, 100, 530] } },
    ]
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(rows),
        0,
        vp(430, 550, 660)
      )
    ).toBe(20)
  })

  it("close-nodes: first visible partially above, next beyond SCROLL_PADDING → use first visible", () => {
    const rows: BlockFirstBboxRow[] = [
      { id: 10, firstBbox: { pageIndex: 0, bbox: [48, 400, 100, 450] } },
      { id: 20, firstBbox: { pageIndex: 0, bbox: [48, 510, 100, 560] } },
    ]
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(rows),
        0,
        vp(430, 550, 660)
      )
    ).toBe(10)
  })

  it("on a later page with viewport showing page top, first anchor on that page is current", () => {
    const rows: BlockFirstBboxRow[] = [
      { id: 1 },
      { id: 2, firstBbox: { pageIndex: 0, bbox: [48, 72, 100, 100] } },
      { id: 3, firstBbox: { pageIndex: 1, bbox: [87, 68, 200, 100] } },
    ]
    expect(
      currentBlockAnchorIdFromAnchorPage(
        asLegacyAnchorArg(rows),
        1,
        vp(0, 300, 600),
        2
      )
    ).toBe(3)
  })
})
