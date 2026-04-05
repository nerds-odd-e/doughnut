import type { BookAnchorFull } from "@generated/doughnut-backend-api"
import { ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1 } from "@/lib/book-reading/mineruOutlineV1PageIndex"
import { viewportCurrentAnchorIdFromAnchorPage } from "@/lib/book-reading/viewportCurrentRangeFromAnchorPage"
import { describe, expect, it } from "vitest"

function mineruAnchor(id: number, json: string): BookAnchorFull {
  return {
    id,
    anchorFormat: ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
    value: json,
  }
}

/** Same preorder and page_idx as E2E stub `top-maths` content list. */
function topMathsLikeAnchors(): BookAnchorFull[] {
  return [
    mineruAnchor(101, '{"page_idx":0}'),
    mineruAnchor(102, '{"page_idx":0,"bbox":[48,72,564,200]}'),
    mineruAnchor(103, '{"page_idx":0,"bbox":[48,520,564,756]}'),
    mineruAnchor(104, '{"page_idx":1}'),
    mineruAnchor(105, '{"page_idx":1}'),
    mineruAnchor(106, '{"page_idx":1}'),
  ]
}

describe("viewportCurrentAnchorIdFromAnchorPage", () => {
  it("returns last preorder anchor with start page <= anchor (page 0)", () => {
    expect(
      viewportCurrentAnchorIdFromAnchorPage(topMathsLikeAnchors(), 0)
    ).toBe(103)
  })

  it("returns last preorder anchor with start page <= anchor (page 1)", () => {
    expect(
      viewportCurrentAnchorIdFromAnchorPage(topMathsLikeAnchors(), 1)
    ).toBe(106)
  })

  it("returns null for empty list", () => {
    expect(viewportCurrentAnchorIdFromAnchorPage([], 0)).toBe(null)
  })

  it("returns null when every anchor is wrong format or unparseable", () => {
    const anchors: BookAnchorFull[] = [
      { id: 1, anchorFormat: "other", value: "{}" },
      mineruAnchor(2, "not-json"),
    ]
    expect(viewportCurrentAnchorIdFromAnchorPage(anchors, 0)).toBe(null)
  })

  it("returns null for negative or non-integer anchor page", () => {
    expect(
      viewportCurrentAnchorIdFromAnchorPage(topMathsLikeAnchors(), -1)
    ).toBe(null)
    expect(
      viewportCurrentAnchorIdFromAnchorPage(topMathsLikeAnchors(), 1.5)
    ).toBe(null)
  })
})
