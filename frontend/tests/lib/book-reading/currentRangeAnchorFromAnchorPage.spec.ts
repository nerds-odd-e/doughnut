import { currentRangeAnchorIdFromAnchorPage } from "@/lib/book-reading/currentRangeAnchorFromAnchorPage"
import type { ViewportYRange } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

function vp(top: number, mid: number, bottom: number): ViewportYRange {
  return { top, mid, bottom }
}

describe("currentRangeAnchorIdFromAnchorPage", () => {
  it("null viewport returns last anchor on page", () => {
    expect(
      currentRangeAnchorIdFromAnchorPage(
        makeMe.bookReadingTopMathsLikeAnchors(),
        0
      )
    ).toBe(103)
  })

  it("null viewport on page 1 returns last anchor on that page", () => {
    expect(
      currentRangeAnchorIdFromAnchorPage(
        makeMe.bookReadingTopMathsLikeAnchors(),
        1
      )
    ).toBe(106)
  })

  it("returns null for empty list", () => {
    expect(currentRangeAnchorIdFromAnchorPage([], 0)).toBe(null)
  })

  it("returns null when every anchor is wrong format or unparseable", () => {
    const anchors = [
      makeMe.aBookAnchor.anchorFormat("other").value("{}").id(1).please(),
      makeMe.aBookAnchor.id(2).value("not-json").please(),
    ]
    expect(currentRangeAnchorIdFromAnchorPage(anchors, 0)).toBe(null)
  })

  it("returns null for negative or non-integer anchor page", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, -1)).toBe(null)
    expect(currentRangeAnchorIdFromAnchorPage(list, 1.5)).toBe(null)
  })

  it("earliest visible anchor is current when its top has passed viewport mid", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, 0, vp(0, 300, 600))).toBe(
      102
    )
  })

  it("earliest visible anchor whose top > mid → use previous anchor", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, 0, vp(200, 400, 600))).toBe(
      102
    )
  })

  it("earliest visible anchor with top below mid → is current", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, 0, vp(400, 600, 800))).toBe(
      103
    )
  })

  it("gap case: no visible anchor → last anchor with y0 ≤ mid (id=103)", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, 0, vp(210, 340, 380))).toBe(
      102
    )
  })

  it("gap case when viewport mid is past all anchors on page → last anchor", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, 0, vp(700, 800, 900))).toBe(
      103
    )
  })

  it("first visible has no-bbox predecessor (y0=0) whose top > mid → use no-bbox anchor", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, 0, vp(0, 50, 100))).toBe(
      101
    )
  })

  it("with pdfPageCount, skips anchors whose page_idx is beyond the document", () => {
    const anchors = [
      makeMe.aBookAnchor.pdfOutlineV1Start(0).id(1).please(),
      makeMe.aBookAnchor.pdfOutlineV1Start(10).id(2).please(),
    ]
    expect(currentRangeAnchorIdFromAnchorPage(anchors, 1, null, 2)).toBe(1)
  })

  it("with pdfPageCount, returns null when every parseable anchor is beyond the document", () => {
    const anchors = [makeMe.aBookAnchor.pdfOutlineV1Start(10).id(2).please()]
    expect(currentRangeAnchorIdFromAnchorPage(anchors, 1, null, 2)).toBe(null)
  })

  it("with pdfPageCount, returns null when viewport page is out of range", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, 5, null, 3)).toBe(null)
  })

  it("with invalid pdfPageCount, returns null", () => {
    const list = makeMe.bookReadingTopMathsLikeAnchors()
    expect(currentRangeAnchorIdFromAnchorPage(list, 0, null, 0)).toBe(null)
    expect(currentRangeAnchorIdFromAnchorPage(list, 0, null, 1.5)).toBe(null)
  })

  it("partial book layout: only valid pdf.mineru_outline_v1 anchors participate", () => {
    const anchors = [
      makeMe.aBookAnchor.anchorFormat("other").value("{}").id(1).please(),
      makeMe.aBookAnchor.id(2).value("not-json").please(),
      makeMe.aBookAnchor.pdfOutlineV1Start(0).id(77).please(),
    ]
    expect(currentRangeAnchorIdFromAnchorPage(anchors, 0)).toBe(77)
  })

  it("returns null when values parse as JSON but fail outline v1 validation", () => {
    const anchors = [
      makeMe.aBookAnchor.value("{}").id(1).please(),
      makeMe.aBookAnchor.value('{"page_idx":"1"}').id(2).please(),
    ]
    expect(currentRangeAnchorIdFromAnchorPage(anchors, 0)).toBe(null)
  })

  it("close-nodes: first visible partially above viewport, next within SCROLL_PADDING → use next", () => {
    const anchors = [
      makeMe.aBookAnchor
        .pdfOutlineV1Start(0, [48, 400, 100, 450])
        .id(10)
        .please(),
      makeMe.aBookAnchor
        .pdfOutlineV1Start(0, [48, 478, 100, 530])
        .id(20)
        .please(),
    ]
    expect(
      currentRangeAnchorIdFromAnchorPage(anchors, 0, vp(430, 550, 660))
    ).toBe(20)
  })

  it("close-nodes: first visible partially above, next beyond SCROLL_PADDING → use first visible", () => {
    const anchors = [
      makeMe.aBookAnchor
        .pdfOutlineV1Start(0, [48, 400, 100, 450])
        .id(10)
        .please(),
      makeMe.aBookAnchor
        .pdfOutlineV1Start(0, [48, 510, 100, 560])
        .id(20)
        .please(),
    ]
    expect(
      currentRangeAnchorIdFromAnchorPage(anchors, 0, vp(430, 550, 660))
    ).toBe(10)
  })

  it("on a later page with viewport showing page top, first anchor on that page is current", () => {
    const anchors = [
      makeMe.aBookAnchor.pdfOutlineV1Start(0).id(1).please(),
      makeMe.aBookAnchor
        .pdfOutlineV1Start(0, [48, 72, 100, 100])
        .id(2)
        .please(),
      makeMe.aBookAnchor
        .pdfOutlineV1Start(1, [87, 68, 200, 100])
        .id(3)
        .please(),
    ]
    expect(
      currentRangeAnchorIdFromAnchorPage(anchors, 1, vp(0, 300, 600), 2)
    ).toBe(3)
  })
})
