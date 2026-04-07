import {
  MAX_COMFORTABLE_PDF_WIDTH_PX,
  pdfScaleAfterPageWidth,
} from "@/lib/book-reading/pdfDefaultScale"
import { describe, expect, it } from "vitest"

describe("pdfScaleAfterPageWidth", () => {
  const usPaper = 612 // US Letter width in PDF points at scale 1

  it("keeps pdf.js page-width scale when it is below the comfort cap", () => {
    const fitted = 500 / usPaper
    expect(pdfScaleAfterPageWidth(fitted, usPaper)).toBeCloseTo(fitted)
  })

  it("applies comfort cap when pdf.js page-width scale would exceed it", () => {
    const fitted = 1400 / usPaper
    expect(pdfScaleAfterPageWidth(fitted, usPaper)).toBeCloseTo(
      MAX_COMFORTABLE_PDF_WIDTH_PX / usPaper
    )
  })

  it("uses cap scale exactly when fitted equals cap", () => {
    const cap = MAX_COMFORTABLE_PDF_WIDTH_PX / usPaper
    expect(pdfScaleAfterPageWidth(cap, usPaper)).toBeCloseTo(cap)
  })

  it("returns 1 when intrinsicPageWidth is zero", () => {
    expect(pdfScaleAfterPageWidth(1.2, 0)).toBe(1)
  })

  it("returns 1 when intrinsicPageWidth is negative", () => {
    expect(pdfScaleAfterPageWidth(1.2, -100)).toBe(1)
  })

  it("returns 1 when pdfJsPageWidthScale is not finite", () => {
    expect(pdfScaleAfterPageWidth(Number.NaN, usPaper)).toBe(1)
  })

  it("returns 1 when pdfJsPageWidthScale is zero or negative", () => {
    expect(pdfScaleAfterPageWidth(0, usPaper)).toBe(1)
    expect(pdfScaleAfterPageWidth(-1, usPaper)).toBe(1)
  })
})
