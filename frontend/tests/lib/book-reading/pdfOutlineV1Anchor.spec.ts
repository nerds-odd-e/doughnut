import {
  contentBboxWireItemsToNavigationTargets,
  extractPageIndexZeroBased,
  normalizedYToViewportY,
  outlineV1BboxToPdfJsXyzDestArray,
  outlineV1BboxToPixelRect,
  parseNormalizedBBoxArray,
  parsePdfOutlineV1Anchor,
  parsePdfOutlineV1StartAnchor,
  screenYToNormalizedY,
} from "@/lib/book-reading/pdfOutlineV1Anchor"
import { describe, expect, it } from "vitest"

describe("parsePdfOutlineV1StartAnchor", () => {
  it("returns page and null bbox when bbox absent", () => {
    expect(parsePdfOutlineV1StartAnchor('{"page_idx":0}')).toEqual({
      pageIndex: 0,
      bbox: null,
    })
  })

  it("returns page and bbox when valid", () => {
    expect(
      parsePdfOutlineV1StartAnchor('{"page_idx":0,"bbox":[10,20,100,200]}')
    ).toEqual({
      pageIndex: 0,
      bbox: [10, 20, 100, 200],
    })
  })

  it("treats invalid bbox as absent", () => {
    expect(
      parsePdfOutlineV1StartAnchor('{"page_idx":1,"bbox":[0,1,2]}')
    ).toEqual({ pageIndex: 1, bbox: null })
    expect(
      parsePdfOutlineV1StartAnchor('{"page_idx":1,"bbox":[2,1,0,0]}')
    ).toEqual({ pageIndex: 1, bbox: null })
    expect(
      parsePdfOutlineV1StartAnchor('{"page_idx":1,"bbox":"nope"}')
    ).toEqual({ pageIndex: 1, bbox: null })
    expect(
      parsePdfOutlineV1StartAnchor(
        '{"page_idx":1,"bbox":[0,0,100,100],"extra":1}'
      )
    ).toEqual({ pageIndex: 1, bbox: [0, 0, 100, 100] })
  })

  it("returns null when page_idx invalid", () => {
    expect(parsePdfOutlineV1StartAnchor("{}")).toBe(null)
    expect(parsePdfOutlineV1StartAnchor('{"page_idx":-1}')).toBe(null)
  })

  it("returns null for invalid JSON", () => {
    expect(parsePdfOutlineV1StartAnchor("")).toBe(null)
    expect(parsePdfOutlineV1StartAnchor("{")).toBe(null)
    expect(parsePdfOutlineV1StartAnchor("not json")).toBe(null)
  })

  it("returns null when page_idx missing or only bbox", () => {
    expect(parsePdfOutlineV1StartAnchor('{"bbox":[0,0,1,1]}')).toBe(null)
    expect(parsePdfOutlineV1StartAnchor('{"kind":"heading"}')).toBe(null)
  })

  it("returns null when page_idx has wrong type", () => {
    expect(parsePdfOutlineV1StartAnchor('{"page_idx":"0"}')).toBe(null)
    expect(parsePdfOutlineV1StartAnchor('{"page_idx":1.5}')).toBe(null)
    expect(parsePdfOutlineV1StartAnchor('{"page_idx":null}')).toBe(null)
  })

  it("returns null for JSON null, array, or primitive roots", () => {
    expect(parsePdfOutlineV1StartAnchor("null")).toBe(null)
    expect(parsePdfOutlineV1StartAnchor("[]")).toBe(null)
    expect(parsePdfOutlineV1StartAnchor("[1,2]")).toBe(null)
    expect(parsePdfOutlineV1StartAnchor('"x"')).toBe(null)
    expect(parsePdfOutlineV1StartAnchor("42")).toBe(null)
  })

  it("never throws for arbitrary string input", () => {
    const inputs = [
      "",
      "{",
      "not json",
      "{}",
      '{"page_idx":-1}',
      "null",
      "[]",
      '{"page_idx":1e100}',
    ]
    for (const s of inputs) {
      expect(() => parsePdfOutlineV1StartAnchor(s)).not.toThrow()
      const out = parsePdfOutlineV1StartAnchor(s)
      expect(
        out === null ||
          (typeof out.pageIndex === "number" &&
            Number.isInteger(out.pageIndex) &&
            out.pageIndex >= 0)
      ).toBe(true)
    }
  })
})

describe("parsePdfOutlineV1Anchor", () => {
  it("recognizes the pdf.mineru_outline_v1 wire format", () => {
    const anchor = {
      id: 1,
      anchorFormat: "pdf.mineru_outline_v1",
      value: '{"page_idx":0}',
    }
    expect(parsePdfOutlineV1Anchor(anchor)).toEqual({
      pageIndex: 0,
      bbox: null,
    })
  })

  it("returns null for wrong anchor format", () => {
    const anchor = {
      id: 1,
      anchorFormat: "other",
      value: '{"page_idx":0}',
    }
    expect(parsePdfOutlineV1Anchor(anchor)).toBe(null)
  })

  it("returns null when value is malformed", () => {
    const anchor = {
      id: 1,
      anchorFormat: "pdf.mineru_outline_v1",
      value: "not-json",
    }
    expect(parsePdfOutlineV1Anchor(anchor)).toBe(null)
  })
})

describe("parseNormalizedBBoxArray", () => {
  it("accepts valid four-number bbox", () => {
    expect(parseNormalizedBBoxArray([10, 20, 100, 200])).toEqual([
      10, 20, 100, 200,
    ])
  })

  it("rejects wrong length or non-finite or inverted ranges", () => {
    expect(parseNormalizedBBoxArray([1, 2, 3])).toBe(null)
    expect(parseNormalizedBBoxArray([2, 1, 0, 0])).toBe(null)
    expect(parseNormalizedBBoxArray(undefined)).toBe(null)
  })
})

describe("contentBboxWireItemsToNavigationTargets", () => {
  it("maps API items to navigation targets", () => {
    expect(
      contentBboxWireItemsToNavigationTargets([
        { pageIndex: 0, bbox: [1, 2, 3, 4] },
        { pageIndex: 1, bbox: [10, 20, 30, 40] },
      ])
    ).toEqual([
      { pageIndex: 0, bbox: [1, 2, 3, 4] },
      { pageIndex: 1, bbox: [10, 20, 30, 40] },
    ])
  })

  it("drops invalid bbox or page", () => {
    expect(
      contentBboxWireItemsToNavigationTargets([
        { pageIndex: 0, bbox: [1, 2, 3] },
        { pageIndex: -1, bbox: [0, 0, 1, 1] },
        { pageIndex: 0, bbox: [0, 0, 1, 1] },
      ])
    ).toEqual([{ pageIndex: 0, bbox: [0, 0, 1, 1] }])
  })

  it("returns empty for undefined or empty input", () => {
    expect(contentBboxWireItemsToNavigationTargets(undefined)).toEqual([])
    expect(contentBboxWireItemsToNavigationTargets([])).toEqual([])
  })
})

describe("extractPageIndexZeroBased", () => {
  it("returns page_idx for minimal JSON", () => {
    expect(extractPageIndexZeroBased('{"page_idx":0}')).toBe(0)
    expect(extractPageIndexZeroBased('{"page_idx":1}')).toBe(1)
  })

  it("returns page_idx when extra keys present", () => {
    expect(extractPageIndexZeroBased('{"page_idx":2,"bbox":[0,1,2,3]}')).toBe(2)
  })

  it("returns null for invalid JSON", () => {
    expect(extractPageIndexZeroBased("")).toBe(null)
    expect(extractPageIndexZeroBased("{")).toBe(null)
    expect(extractPageIndexZeroBased("not json")).toBe(null)
  })

  it("returns null when page_idx missing", () => {
    expect(extractPageIndexZeroBased("{}")).toBe(null)
    expect(extractPageIndexZeroBased('{"kind":"heading"}')).toBe(null)
  })

  it("returns null when page_idx is not a non-negative integer", () => {
    expect(extractPageIndexZeroBased('{"page_idx":-1}')).toBe(null)
    expect(extractPageIndexZeroBased('{"page_idx":1.5}')).toBe(null)
    expect(extractPageIndexZeroBased('{"page_idx":"0"}')).toBe(null)
  })
})

describe("outlineV1BboxToPdfJsXyzDestArray", () => {
  it("converts 0-1000 bbox to PDF user space XYZ with top padding clamped at page top", () => {
    const w = 612
    const h = 792
    expect(outlineV1BboxToPdfJsXyzDestArray(w, h, [0, 0, 100, 200])).toEqual([
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
    expect(outlineV1BboxToPdfJsXyzDestArray(w, h, [10, 400, 200, 550])).toEqual(
      [null, { name: "XYZ" }, (105 / 1000) * 400, h - yTopPdf, null]
    )
  })
})

describe("outlineV1BboxToPixelRect", () => {
  it("converts normalized bbox to pixel coordinates", () => {
    expect(outlineV1BboxToPixelRect([100, 200, 300, 400], 800, 600)).toEqual({
      left: 80,
      top: 120,
      width: 160,
      height: 120,
    })
  })

  it("maps full-page bbox to full viewport", () => {
    expect(outlineV1BboxToPixelRect([0, 0, 1000, 1000], 500, 400)).toEqual({
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
