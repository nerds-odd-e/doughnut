import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import { ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 } from "@/lib/book-reading/mineruOutlineV1PageIndex"
import {
  type OutlineAnchorNode,
  viewportCurrentRangeIdFromAnchorPage,
} from "@/lib/book-reading/viewportCurrentRangeFromAnchorPage"
import { describe, expect, it } from "vitest"

function mineruAnchor(json: string): BookAnchorFull {
  return {
    id: 0,
    anchorFormat: ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
    value: json,
  }
}

/** Same preorder and page_idx as E2E stub `top-maths` content list (arbitrary range ids). */
function topMathsLikeOutline(): OutlineAnchorNode[] {
  return [
    { id: 101, startAnchor: mineruAnchor('{"page_idx":0}') },
    {
      id: 102,
      startAnchor: mineruAnchor('{"page_idx":0,"bbox":[48,72,564,200]}'),
    },
    {
      id: 103,
      startAnchor: mineruAnchor('{"page_idx":0,"bbox":[48,520,564,756]}'),
    },
    { id: 104, startAnchor: mineruAnchor('{"page_idx":1}') },
    { id: 105, startAnchor: mineruAnchor('{"page_idx":1}') },
    { id: 106, startAnchor: mineruAnchor('{"page_idx":1}') },
  ]
}

describe("viewportCurrentRangeIdFromAnchorPage", () => {
  it("returns last preorder node with start page <= anchor (page 0)", () => {
    expect(viewportCurrentRangeIdFromAnchorPage(topMathsLikeOutline(), 0)).toBe(
      103
    )
  })

  it("returns last preorder node with start page <= anchor (page 1)", () => {
    expect(viewportCurrentRangeIdFromAnchorPage(topMathsLikeOutline(), 1)).toBe(
      106
    )
  })

  it("returns null for empty outline", () => {
    expect(viewportCurrentRangeIdFromAnchorPage([], 0)).toBe(null)
  })

  it("returns null when every anchor is missing or unparseable", () => {
    const nodes: OutlineAnchorNode[] = [
      { id: 1, startAnchor: { id: 0, anchorFormat: "other", value: "{}" } },
      { id: 2, startAnchor: mineruAnchor("not-json") },
      { id: 3 },
    ]
    expect(viewportCurrentRangeIdFromAnchorPage(nodes, 0)).toBe(null)
  })

  it("returns null for negative or non-integer anchor page", () => {
    expect(
      viewportCurrentRangeIdFromAnchorPage(topMathsLikeOutline(), -1)
    ).toBe(null)
    expect(
      viewportCurrentRangeIdFromAnchorPage(topMathsLikeOutline(), 1.5)
    ).toBe(null)
  })
})
