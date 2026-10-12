import {
  computeDropdownPortalStyle,
  parseDropdownPlacementFromDetails,
} from "@/composables/dropdownPortalPlacement"
import { describe, expect, it, afterEach, vi } from "vitest"

function mockMatchMedia(matchesLg: boolean) {
  return vi.spyOn(window, "matchMedia").mockImplementation((query) => ({
    matches: matchesLg && query === "(min-width: 1024px)",
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }))
}

describe("dropdownPortalPlacement", () => {
  describe("parseDropdownPlacementFromDetails", () => {
    it("defaults to bottom start when details is null", () => {
      expect(parseDropdownPlacementFromDetails(null)).toEqual({
        side: "bottom",
        align: "start",
      })
    })

    it("reads start and bottom from classList", () => {
      const details = document.createElement("details")
      details.className =
        "daisy-dropdown daisy-dropdown-start daisy-dropdown-bottom"
      expect(parseDropdownPlacementFromDetails(details)).toEqual({
        side: "bottom",
        align: "start",
      })
    })

    it("reads end alignment from daisy-dropdown-end", () => {
      const details = document.createElement("details")
      details.className =
        "daisy-dropdown daisy-dropdown-end daisy-dropdown-bottom"
      expect(parseDropdownPlacementFromDetails(details)).toEqual({
        side: "bottom",
        align: "end",
      })
    })
  })

  describe("computeDropdownPortalStyle", () => {
    const anchor = new DOMRect(100, 200, 40, 32)

    it("places panel below and left-aligned for bottom start", () => {
      expect(
        computeDropdownPortalStyle(anchor, 200, 120, {
          side: "bottom",
          align: "start",
        })
      ).toEqual({ top: "240px", left: "100px" })
    })

    it("places panel below and right-aligned for bottom end", () => {
      expect(
        computeDropdownPortalStyle(anchor, 100, 120, {
          side: "bottom",
          align: "end",
        })
      ).toEqual({ top: "240px", left: "40px" })
    })

    it("keeps a panel wider than the space left of its button inside the screen", () => {
      expect(
        computeDropdownPortalStyle(anchor, 200, 120, {
          side: "bottom",
          align: "end",
        })
      ).toEqual({ top: "240px", left: "8px" })
    })

    it("keeps a panel wider than the space right of its button inside the screen", () => {
      const screenWidth = document.documentElement.clientWidth
      const anchorNearRightEdge = new DOMRect(screenWidth - 40, 200, 40, 32)
      expect(
        computeDropdownPortalStyle(anchorNearRightEdge, 200, 120, {
          side: "top",
          align: "start",
        })
      ).toEqual({ top: "72px", left: `${screenWidth - 208}px` })
    })

    it("places panel above for top start", () => {
      expect(
        computeDropdownPortalStyle(anchor, 200, 120, {
          side: "top",
          align: "start",
        })
      ).toEqual({ top: "72px", left: "100px" })
    })

    it("places panel to the right of the anchor for right start", () => {
      expect(
        computeDropdownPortalStyle(anchor, 200, 120, {
          side: "right",
          align: "start",
        })
      ).toEqual({ top: "200px", left: "148px" })
    })
  })
})

describe("parseDropdownPlacementFromDetails responsive classes", () => {
  let matchMediaSpy: ReturnType<typeof vi.spyOn> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
  })

  it("prefers lg:daisy-dropdown-right at large viewport for account nav", () => {
    matchMediaSpy = mockMatchMedia(true)

    const details = document.createElement("details")
    details.className =
      "daisy-dropdown daisy-dropdown-bottom daisy-dropdown-end lg:daisy-dropdown-top lg:daisy-dropdown-right"
    expect(parseDropdownPlacementFromDetails(details)).toEqual({
      side: "right",
      align: "end",
    })
  })

  it("uses base bottom placement below large viewport", () => {
    matchMediaSpy = mockMatchMedia(false)

    const details = document.createElement("details")
    details.className =
      "daisy-dropdown daisy-dropdown-bottom daisy-dropdown-end lg:daisy-dropdown-top lg:daisy-dropdown-right"
    expect(parseDropdownPlacementFromDetails(details)).toEqual({
      side: "bottom",
      align: "end",
    })
  })
})
