import {
  ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1,
  extractPageIndexZeroBased,
} from "@/lib/book-reading/mineruOutlineV1PageIndex"
import { describe, expect, it } from "vitest"

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

describe("ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1", () => {
  it("matches backend wire constant", () => {
    expect(ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1).toBe("pdf.mineru_outline_v1")
  })
})
