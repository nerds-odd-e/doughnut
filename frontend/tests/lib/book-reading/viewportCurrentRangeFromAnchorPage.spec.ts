import { viewportCurrentAnchorIdFromAnchorPage } from "@/lib/book-reading/viewportCurrentRangeFromAnchorPage"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

describe("viewportCurrentAnchorIdFromAnchorPage", () => {
  it("returns last preorder anchor with start page <= anchor (page 0)", () => {
    expect(
      viewportCurrentAnchorIdFromAnchorPage(
        makeMe.bookReadingTopMathsLikeAnchors(),
        0
      )
    ).toBe(103)
  })

  it("returns last preorder anchor with start page <= anchor (page 1)", () => {
    expect(
      viewportCurrentAnchorIdFromAnchorPage(
        makeMe.bookReadingTopMathsLikeAnchors(),
        1
      )
    ).toBe(106)
  })

  it("returns null for empty list", () => {
    expect(viewportCurrentAnchorIdFromAnchorPage([], 0)).toBe(null)
  })

  it("returns null when every anchor is wrong format or unparseable", () => {
    const anchors = [
      makeMe.aBookAnchor.anchorFormat("other").value("{}").id(1).please(),
      makeMe.aBookAnchor.id(2).value("not-json").please(),
    ]
    expect(viewportCurrentAnchorIdFromAnchorPage(anchors, 0)).toBe(null)
  })

  it("returns null for negative or non-integer anchor page", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(viewportCurrentAnchorIdFromAnchorPage(list, -1)).toBe(null)
    expect(viewportCurrentAnchorIdFromAnchorPage(list, 1.5)).toBe(null)
  })
})
