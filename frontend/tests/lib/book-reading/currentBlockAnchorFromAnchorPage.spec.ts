import { currentBlockAnchorIdFromAnchorPage } from "@/lib/book-reading/currentBlockAnchorFromAnchorPage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import { describe, expect, it } from "vitest"

/** Same JSON as `BookAnchorFullBuilder.pdfOutlineV1Start` — `currentBlockAnchorIdFromAnchorPage` parses `value` only. */
function outlineV1Anchor(
  id: number,
  pageIndex: number,
  bbox?: readonly [number, number, number, number]
): BookAnchorFull {
  const payload: { page_idx: number; bbox?: number[] } = {
    page_idx: pageIndex,
  }
  if (bbox !== undefined) {
    payload.bbox = [...bbox]
  }
  return { id, value: JSON.stringify(payload) }
}

const topMathsLikePreorderRows: BookAnchorFull[] = [
  outlineV1Anchor(101, 0),
  outlineV1Anchor(102, 0, [48, 72, 564, 200]),
  outlineV1Anchor(103, 0, [48, 520, 564, 756]),
  outlineV1Anchor(104, 1),
  outlineV1Anchor(105, 1),
  outlineV1Anchor(106, 1),
]

function vp(top: number, mid: number, bottom: number): ViewportYRange {
  return { top, mid, bottom }
}

describe("currentBlockAnchorIdFromAnchorPage", () => {
  it("null viewport returns last anchor on page", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, 0)
    ).toBe(103)
  })

  it("null viewport on page 1 returns last anchor on that page", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, 1)
    ).toBe(106)
  })

  it("returns null for empty list", () => {
    expect(currentBlockAnchorIdFromAnchorPage([], 0)).toBe(null)
  })

  it("returns null when every anchor value is unparseable as outline v1", () => {
    const rows: BookAnchorFull[] = [
      { id: 1, value: "{}" },
      { id: 2, value: "not-json" },
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0)).toBe(null)
  })

  it("returns null for negative or non-integer anchor page", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, -1)
    ).toBe(null)
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, 1.5)
    ).toBe(null)
  })

  it("earliest visible anchor is current when its top has passed viewport mid", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(0, 300, 600)
      )
    ).toBe(102)
  })

  it("earliest visible anchor whose top > mid → use previous anchor", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(200, 400, 600)
      )
    ).toBe(102)
  })

  it("earliest visible anchor with top below mid → is current", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(400, 600, 800)
      )
    ).toBe(103)
  })

  it("gap case: no visible anchor → last anchor with y0 ≤ mid (id=103)", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(210, 340, 380)
      )
    ).toBe(102)
  })

  it("gap case when viewport mid is past all anchors on page → last anchor", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(700, 800, 900)
      )
    ).toBe(103)
  })

  it("first visible has no-bbox predecessor (y0=0) whose top > mid → use no-bbox anchor", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(0, 50, 100)
      )
    ).toBe(101)
  })

  it("with pdfPageCount, skips anchors whose page_idx is beyond the document", () => {
    const rows: BookAnchorFull[] = [
      outlineV1Anchor(1, 0),
      outlineV1Anchor(2, 10),
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 1, null, 2)).toBe(1)
  })

  it("with pdfPageCount, returns null when every parseable anchor is beyond the document", () => {
    const rows: BookAnchorFull[] = [outlineV1Anchor(2, 10)]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 1, null, 2)).toBe(null)
  })

  it("with pdfPageCount, returns null when viewport page is out of range", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, 5, null, 3)
    ).toBe(null)
  })

  it("with invalid pdfPageCount, returns null", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, 0, null, 0)
    ).toBe(null)
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, 0, null, 1.5)
    ).toBe(null)
  })

  it("partial book layout: only anchors with valid outline v1 values participate", () => {
    const rows: BookAnchorFull[] = [
      { id: 1, value: "{}" },
      { id: 2, value: "not-json" },
      outlineV1Anchor(77, 0),
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0)).toBe(77)
  })

  it("returns null when values parse as JSON but fail outline v1 validation", () => {
    const rows: BookAnchorFull[] = [
      { id: 1, value: "{}" },
      { id: 2, value: '{"page_idx":"1"}' },
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0)).toBe(null)
  })

  it("close-nodes: first visible partially above viewport, next within SCROLL_PADDING → use next", () => {
    const rows: BookAnchorFull[] = [
      outlineV1Anchor(10, 0, [48, 400, 100, 450]),
      outlineV1Anchor(20, 0, [48, 478, 100, 530]),
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0, vp(430, 550, 660))).toBe(
      20
    )
  })

  it("close-nodes: first visible partially above, next beyond SCROLL_PADDING → use first visible", () => {
    const rows: BookAnchorFull[] = [
      outlineV1Anchor(10, 0, [48, 400, 100, 450]),
      outlineV1Anchor(20, 0, [48, 510, 100, 560]),
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0, vp(430, 550, 660))).toBe(
      10
    )
  })

  it("on a later page with viewport showing page top, first anchor on that page is current", () => {
    const rows: BookAnchorFull[] = [
      outlineV1Anchor(1, 0),
      outlineV1Anchor(2, 0, [48, 72, 100, 100]),
      outlineV1Anchor(3, 1, [87, 68, 200, 100]),
    ]
    expect(
      currentBlockAnchorIdFromAnchorPage(rows, 1, vp(0, 300, 600), 2)
    ).toBe(3)
  })
})
