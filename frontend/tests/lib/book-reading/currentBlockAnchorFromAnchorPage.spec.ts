import {
  currentBlockAnchorIdFromAnchorPage,
  type BookBlockFirstBboxRow,
} from "@/lib/book-reading/currentBlockAnchorFromAnchorPage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import { describe, expect, it } from "vitest"

/** Top-maths-like preorder: ids 101–106, `firstBbox` matches `allBboxes[0]` wire shape. */
const topMathsLikePreorderRows: BookBlockFirstBboxRow[] = [
  { id: 101, firstBbox: { pageIndex: 0 } },
  { id: 102, firstBbox: { pageIndex: 0, bbox: [48, 72, 564, 200] } },
  { id: 103, firstBbox: { pageIndex: 0, bbox: [48, 520, 564, 756] } },
  { id: 104, firstBbox: { pageIndex: 1 } },
  { id: 105, firstBbox: { pageIndex: 1 } },
  { id: 106, firstBbox: { pageIndex: 1 } },
]

function vp(top: number, mid: number, bottom: number): ViewportYRange {
  return { top, mid, bottom }
}

describe("currentBlockAnchorIdFromAnchorPage", () => {
  it("null viewport returns last block on page", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, 0)
    ).toBe(103)
  })

  it("null viewport on page 1 returns last block on that page", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(topMathsLikePreorderRows, 1)
    ).toBe(106)
  })

  it("returns null for empty list", () => {
    expect(currentBlockAnchorIdFromAnchorPage([], 0)).toBe(null)
  })

  it("returns null when no row has a usable first bbox", () => {
    const rows: BookBlockFirstBboxRow[] = [{ id: 1 }, { id: 2 }]
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

  it("earliest visible block is current when its top has passed viewport mid", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(0, 300, 600)
      )
    ).toBe(102)
  })

  it("earliest visible block whose top > mid → use previous block", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(200, 400, 600)
      )
    ).toBe(102)
  })

  it("earliest visible block with top below mid → is current", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(400, 600, 800)
      )
    ).toBe(103)
  })

  it("gap case: no visible block → last block with y0 ≤ mid (id=102)", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(210, 340, 380)
      )
    ).toBe(102)
  })

  it("gap case when viewport mid is past all blocks on page → last block", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(700, 800, 900)
      )
    ).toBe(103)
  })

  it("first visible has no-bbox predecessor (y0=0) whose top > mid → use no-bbox block", () => {
    expect(
      currentBlockAnchorIdFromAnchorPage(
        topMathsLikePreorderRows,
        0,
        vp(0, 50, 100)
      )
    ).toBe(101)
  })

  it("with pdfPageCount, skips blocks whose page index is beyond the document", () => {
    const rows: BookBlockFirstBboxRow[] = [
      { id: 1, firstBbox: { pageIndex: 0 } },
      { id: 2, firstBbox: { pageIndex: 10 } },
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 1, null, 2)).toBe(1)
  })

  it("with pdfPageCount, returns null when every row is beyond the document", () => {
    const rows: BookBlockFirstBboxRow[] = [
      { id: 2, firstBbox: { pageIndex: 10 } },
    ]
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

  it("partial book layout: only rows with valid first bbox participate", () => {
    const rows: BookBlockFirstBboxRow[] = [
      { id: 1 },
      { id: 2, firstBbox: { pageIndex: -1 } },
      { id: 77, firstBbox: { pageIndex: 0 } },
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0)).toBe(77)
  })

  it("returns null when bbox is present but fails validation", () => {
    const rows: BookBlockFirstBboxRow[] = [
      { id: 1 },
      { id: 2, firstBbox: { pageIndex: 0, bbox: [48, 72] } },
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0)).toBe(null)
  })

  it("close-nodes: first visible partially above viewport, next within SCROLL_PADDING → use next", () => {
    const rows: BookBlockFirstBboxRow[] = [
      { id: 10, firstBbox: { pageIndex: 0, bbox: [48, 400, 100, 450] } },
      { id: 20, firstBbox: { pageIndex: 0, bbox: [48, 478, 100, 530] } },
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0, vp(430, 550, 660))).toBe(
      20
    )
  })

  it("close-nodes: first visible partially above, next beyond SCROLL_PADDING → use first visible", () => {
    const rows: BookBlockFirstBboxRow[] = [
      { id: 10, firstBbox: { pageIndex: 0, bbox: [48, 400, 100, 450] } },
      { id: 20, firstBbox: { pageIndex: 0, bbox: [48, 510, 100, 560] } },
    ]
    expect(currentBlockAnchorIdFromAnchorPage(rows, 0, vp(430, 550, 660))).toBe(
      10
    )
  })

  it("on a later page with viewport showing page top, first block on that page is current", () => {
    const rows: BookBlockFirstBboxRow[] = [
      { id: 1, firstBbox: { pageIndex: 0 } },
      { id: 2, firstBbox: { pageIndex: 0, bbox: [48, 72, 100, 100] } },
      { id: 3, firstBbox: { pageIndex: 1, bbox: [87, 68, 200, 100] } },
    ]
    expect(
      currentBlockAnchorIdFromAnchorPage(rows, 1, vp(0, 300, 600), 2)
    ).toBe(3)
  })
})
