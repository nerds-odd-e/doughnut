import { describe, expect, it } from "vitest"
import { snapBackNormalizedSpanFitsViewport } from "@/composables/useBookReadingSnapBack"

describe("snapBackNormalizedSpanFitsViewport", () => {
  it("returns true when normalized span in px fits below the obstruction", () => {
    expect(
      snapBackNormalizedSpanFitsViewport({
        pageHeightPx: 1000,
        viewportHeightPx: 830,
        normalizedBlockTopY: 0,
        normalizedContentBottomY: 750,
        obstructionPx: 80,
      })
    ).toBe(true)
  })

  it("returns false when the span exceeds viewport minus obstruction", () => {
    expect(
      snapBackNormalizedSpanFitsViewport({
        pageHeightPx: 1000,
        viewportHeightPx: 100,
        normalizedBlockTopY: 0,
        normalizedContentBottomY: 750,
        obstructionPx: 80,
      })
    ).toBe(false)
  })
})
