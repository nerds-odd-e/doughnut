import {
  normalizedBboxToPdfJsXyzDestArray,
  normalizedBboxToPixelRect,
  normalizedYToViewportY,
  screenYToNormalizedY,
  wireItemsToNavigationTargets,
} from "@/lib/book-reading/pdfOutlineV1Anchor"
import { describe, expect, it } from "vitest"

describe("wireItemsToNavigationTargets", () => {
  it("maps allBboxes wire items to navigation targets", () => {
    expect(
      wireItemsToNavigationTargets([
        { pageIndex: 0, bbox: [1, 2, 3, 4] },
        { pageIndex: 1, bbox: [10, 20, 30, 40] },
      ])
    ).toEqual([
      { pageIndex: 0, bbox: [1, 2, 3, 4] },
      { pageIndex: 1, bbox: [10, 20, 30, 40] },
    ])
  })

  it("drops items with invalid bbox or negative page", () => {
    expect(
      wireItemsToNavigationTargets([
        { pageIndex: 0, bbox: [1, 2, 3] },
        { pageIndex: -1, bbox: [0, 0, 1, 1] },
        { pageIndex: 0, bbox: [0, 0, 1, 1] },
      ])
    ).toEqual([{ pageIndex: 0, bbox: [0, 0, 1, 1] }])
  })

  it("maps page-only wire items to targets with null bbox", () => {
    expect(
      wireItemsToNavigationTargets([
        { pageIndex: 0 },
        { pageIndex: 1, bbox: [0, 0, 1, 1] },
      ])
    ).toEqual([
      { pageIndex: 0, bbox: null },
      { pageIndex: 1, bbox: [0, 0, 1, 1] },
    ])
  })

  it("returns empty for undefined or empty input", () => {
    expect(wireItemsToNavigationTargets(undefined)).toEqual([])
    expect(wireItemsToNavigationTargets([])).toEqual([])
  })
})

describe("normalizedBboxToPdfJsXyzDestArray", () => {
  it("converts 0-1000 bbox to PDF user space XYZ with top padding clamped at page top", () => {
    const w = 612
    const h = 792
    expect(normalizedBboxToPdfJsXyzDestArray(w, h, [0, 0, 100, 200])).toEqual([
      null,
      { name: "XYZ" },
      (50 / 1000) * 612,
      792,
      null,
    ])
  })

  it("maps a lower band with top padding above bbox top", () => {
    const w = 400
    const h = 600
    const yTopPdf = Math.max(0, (400 / 1000) * 600 - 40)
    expect(
      normalizedBboxToPdfJsXyzDestArray(w, h, [10, 400, 200, 550])
    ).toEqual([null, { name: "XYZ" }, (105 / 1000) * 400, h - yTopPdf, null])
  })
})

describe("normalizedBboxToPixelRect", () => {
  it("converts normalized bbox to pixel coordinates", () => {
    expect(normalizedBboxToPixelRect([100, 200, 300, 400], 800, 600)).toEqual({
      left: 80,
      top: 120,
      width: 160,
      height: 120,
    })
  })

  it("maps full-page bbox to full viewport", () => {
    expect(normalizedBboxToPixelRect([0, 0, 1000, 1000], 500, 400)).toEqual({
      left: 0,
      top: 0,
      width: 500,
      height: 400,
    })
  })
})

describe("normalizedYToViewportY", () => {
  it("scales normalized Y to viewport height", () => {
    expect(normalizedYToViewportY(500, 800)).toBe(400)
  })

  it("clamps to 0 at the low end", () => {
    expect(normalizedYToViewportY(-50, 800)).toBe(0)
  })

  it("clamps to page height at the high end", () => {
    expect(normalizedYToViewportY(1500, 800)).toBe(800)
  })
})

describe("screenYToNormalizedY", () => {
  it("maps screen position within page to normalized Y", () => {
    expect(screenYToNormalizedY(150, { top: 100, height: 200 })).toBe(250)
  })

  it("clamps to 0 when above the page", () => {
    expect(screenYToNormalizedY(50, { top: 100, height: 200 })).toBe(0)
  })

  it("clamps to 1000 when below the page", () => {
    expect(screenYToNormalizedY(400, { top: 100, height: 200 })).toBe(1000)
  })
})
