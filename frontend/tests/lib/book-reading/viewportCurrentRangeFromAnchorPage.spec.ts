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

  it("on same page, viewport Y between bbox tops picks the earlier section (102)", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(viewportCurrentAnchorIdFromAnchorPage(list, 0, 400)).toBe(102)
  })

  it("on same page, viewport Y at second bbox top includes that section (103)", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(viewportCurrentAnchorIdFromAnchorPage(list, 0, 520)).toBe(103)
  })

  it("on same page, viewport Y past second bbox keeps last matching section (103)", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(viewportCurrentAnchorIdFromAnchorPage(list, 0, 800)).toBe(103)
  })

  it("on same page, viewport Y before first bbox top keeps page-top rows only (101)", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(viewportCurrentAnchorIdFromAnchorPage(list, 0, 0)).toBe(101)
  })

  it("with pdfPageCount, skips anchors whose page_idx is beyond the document", () => {
    const anchors = [
      makeMe.aBookAnchor.mineruStart(0).id(1).please(),
      makeMe.aBookAnchor.mineruStart(10).id(2).please(),
    ]
    expect(viewportCurrentAnchorIdFromAnchorPage(anchors, 1, null, 2)).toBe(1)
  })

  it("with pdfPageCount, returns null when every parseable anchor is beyond the document", () => {
    const anchors = [makeMe.aBookAnchor.mineruStart(10).id(2).please()]
    expect(viewportCurrentAnchorIdFromAnchorPage(anchors, 1, null, 2)).toBe(
      null
    )
  })

  it("with pdfPageCount, returns null when viewport page is out of range", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(viewportCurrentAnchorIdFromAnchorPage(list, 5, null, 3)).toBe(null)
  })

  it("with invalid pdfPageCount, returns null", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(viewportCurrentAnchorIdFromAnchorPage(list, 0, null, 0)).toBe(null)
    expect(viewportCurrentAnchorIdFromAnchorPage(list, 0, null, 1.5)).toBe(null)
  })

  it("partial outline: only valid mineru anchors participate", () => {
    const anchors = [
      makeMe.aBookAnchor.anchorFormat("other").value("{}").id(1).please(),
      makeMe.aBookAnchor.id(2).value("not-json").please(),
      makeMe.aBookAnchor.mineruStart(0).id(77).please(),
    ]
    expect(viewportCurrentAnchorIdFromAnchorPage(anchors, 0)).toBe(77)
  })

  it("returns null when mineru values parse as JSON but fail mineru validation", () => {
    const anchors = [
      makeMe.aBookAnchor.value("{}").id(1).please(),
      makeMe.aBookAnchor.value('{"page_idx":"1"}').id(2).please(),
    ]
    expect(viewportCurrentAnchorIdFromAnchorPage(anchors, 0)).toBe(null)
  })

  it("on a later page with viewport at top, first anchor on that page when none pass grace", () => {
    const anchors = [
      makeMe.aBookAnchor.mineruStart(0).id(1).please(),
      makeMe.aBookAnchor.mineruStart(0, [48, 72, 100, 100]).id(2).please(),
      makeMe.aBookAnchor.mineruStart(1, [87, 68, 200, 100]).id(3).please(),
    ]
    expect(viewportCurrentAnchorIdFromAnchorPage(anchors, 1, 0, 2)).toBe(3)
  })
})
