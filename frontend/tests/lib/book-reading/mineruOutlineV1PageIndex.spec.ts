import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  extractPageIndexZeroBased,
  mineruOutlineV1BboxToXyzDestArray,
  parseMineruOutlineV1StartAnchor,
} from "@/lib/book-reading/mineruOutlineV1PageIndex"
import { describe, expect, it } from "vitest"

describe("parseMineruOutlineV1StartAnchor", () => {
  it("returns page and null bbox when bbox absent", () => {
    expect(parseMineruOutlineV1StartAnchor('{"page_idx":0}')).toEqual({
      pageIndex: 0,
      bbox: null,
    })
  })

  it("returns page and bbox when valid", () => {
    expect(
      parseMineruOutlineV1StartAnchor('{"page_idx":0,"bbox":[10,20,100,200]}')
    ).toEqual({
      pageIndex: 0,
      bbox: [10, 20, 100, 200],
    })
  })

  it("treats invalid bbox as absent", () => {
    expect(
      parseMineruOutlineV1StartAnchor('{"page_idx":1,"bbox":[0,1,2]}')
    ).toEqual({ pageIndex: 1, bbox: null })
    expect(
      parseMineruOutlineV1StartAnchor('{"page_idx":1,"bbox":[2,1,0,0]}')
    ).toEqual({ pageIndex: 1, bbox: null })
    expect(
      parseMineruOutlineV1StartAnchor('{"page_idx":1,"bbox":"nope"}')
    ).toEqual({ pageIndex: 1, bbox: null })
    expect(
      parseMineruOutlineV1StartAnchor(
        '{"page_idx":1,"bbox":[0,0,100,100],"extra":1}'
      )
    ).toEqual({ pageIndex: 1, bbox: [0, 0, 100, 100] })
  })

  it("returns null when page_idx invalid", () => {
    expect(parseMineruOutlineV1StartAnchor("{}")).toBe(null)
    expect(parseMineruOutlineV1StartAnchor('{"page_idx":-1}')).toBe(null)
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

describe("mineruOutlineV1BboxToXyzDestArray", () => {
  it("maps bbox center to XYZ in PDF space", () => {
    const h = 792
    expect(mineruOutlineV1BboxToXyzDestArray(h, [0, 0, 100, 200])).toEqual([
      null,
      { name: "XYZ" },
      50,
      692,
      null,
    ])
  })

  it("maps a lower band center on the page", () => {
    const h = 600
    expect(mineruOutlineV1BboxToXyzDestArray(h, [10, 400, 200, 550])).toEqual([
      null,
      { name: "XYZ" },
      105,
      125,
      null,
    ])
  })
})

describe("ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1", () => {
  it("matches backend wire constant", () => {
    expect(ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1).toBe("pdf.mineru_outline_v1")
  })
})
