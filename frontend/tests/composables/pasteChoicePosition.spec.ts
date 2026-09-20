import { describe, it, expect } from "vitest"
import { computePasteChoiceStyle } from "@/composables/pasteChoicePosition"

describe("computePasteChoiceStyle", () => {
  const barSize = { width: 200, height: 44 }
  const viewport = { width: 800, height: 600 }

  it("places the bar just below the anchor when it fits", () => {
    const anchor = { top: 100, bottom: 120, left: 50, right: 90 }

    expect(computePasteChoiceStyle(anchor, barSize, viewport)).toEqual({
      top: "124px",
      left: "50px",
    })
  })

  it("flips above the anchor when it would not fit below the viewport", () => {
    const anchor = { top: 580, bottom: 590, left: 50, right: 90 }

    expect(computePasteChoiceStyle(anchor, barSize, viewport)).toEqual({
      top: "532px",
      left: "50px",
    })
  })

  it("clamps horizontally within the viewport when the anchor is near the right edge", () => {
    const anchor = { top: 100, bottom: 120, left: 750, right: 790 }

    expect(computePasteChoiceStyle(anchor, barSize, viewport)).toEqual({
      top: "124px",
      left: "592px",
    })
  })

  it("clamps to the viewport margin when the anchor is near the left edge", () => {
    const anchor = { top: 100, bottom: 120, left: -30, right: 10 }

    expect(computePasteChoiceStyle(anchor, barSize, viewport)).toEqual({
      top: "124px",
      left: "8px",
    })
  })

  it("does not overlap the anchor's own vertical span in either placement", () => {
    const below = computePasteChoiceStyle(
      { top: 100, bottom: 120, left: 50, right: 90 },
      barSize,
      viewport
    )
    expect(Number.parseFloat(below.top)).toBeGreaterThanOrEqual(120)

    const above = computePasteChoiceStyle(
      { top: 580, bottom: 590, left: 50, right: 90 },
      barSize,
      viewport
    )
    expect(Number.parseFloat(above.top) + barSize.height).toBeLessThanOrEqual(
      580
    )
  })
})
