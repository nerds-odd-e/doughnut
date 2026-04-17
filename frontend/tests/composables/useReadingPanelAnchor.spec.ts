import { clampReadingPanelAnchorTop } from "@/composables/useReadingPanelAnchor"
import { describe, expect, it } from "vitest"

describe("clampReadingPanelAnchorTop", () => {
  it("returns null when the anchor would sit too low in the main pane", () => {
    const mainH = 400
    const minReserve = 88
    const edgeInset = 8
    const threshold = mainH - edgeInset - minReserve
    const topTooLow = threshold + 1
    expect(
      clampReadingPanelAnchorTop(topTooLow, mainH, minReserve, edgeInset)
    ).toBeNull()
  })

  it("returns the top value when there is enough room below the anchor", () => {
    const mainH = 400
    const top = 100
    expect(clampReadingPanelAnchorTop(top, mainH)).toBe(100)
  })

  it("returns null for a null top", () => {
    expect(clampReadingPanelAnchorTop(null, 400)).toBeNull()
  })
})
