import { describe, expect, it } from "vitest"
import {
  clampScrollAxis,
  scrollAfterUniformContentScale,
  wheelDeltaYToScaleFactor,
} from "@/lib/book-reading/pdfBookViewerZoomAroundPoint"

describe("scrollAfterUniformContentScale", () => {
  it("scales scrollTop from top of viewport (y=0)", () => {
    expect(
      scrollAfterUniformContentScale({
        scrollLeft: 0,
        scrollTop: 100,
        originXInContainer: 0,
        originYInContainer: 0,
        scaleFactor: 2,
      })
    ).toEqual({ scrollLeft: 0, scrollTop: 200 })
  })

  it("keeps focal content under viewport offset y=50 when zooming 2x", () => {
    expect(
      scrollAfterUniformContentScale({
        scrollLeft: 0,
        scrollTop: 100,
        originXInContainer: 0,
        originYInContainer: 50,
        scaleFactor: 2,
      })
    ).toEqual({ scrollLeft: 0, scrollTop: 250 })
  })

  it("adjusts both axes", () => {
    expect(
      scrollAfterUniformContentScale({
        scrollLeft: 40,
        scrollTop: 60,
        originXInContainer: 10,
        originYInContainer: 20,
        scaleFactor: 2,
      })
    ).toEqual({ scrollLeft: 90, scrollTop: 140 })
  })

  it("is identity when scaleFactor is 1", () => {
    expect(
      scrollAfterUniformContentScale({
        scrollLeft: 33,
        scrollTop: 77,
        originXInContainer: 5,
        originYInContainer: 9,
        scaleFactor: 1,
      })
    ).toEqual({ scrollLeft: 33, scrollTop: 77 })
  })
})

describe("clampScrollAxis", () => {
  it("clamps to [0, scrollSize - clientSize]", () => {
    expect(clampScrollAxis(500, 800, 200)).toBe(500)
    expect(clampScrollAxis(-10, 800, 200)).toBe(0)
    expect(clampScrollAxis(9999, 800, 200)).toBe(600)
  })

  it("returns 0 when content fits", () => {
    expect(clampScrollAxis(50, 100, 200)).toBe(0)
  })
})

describe("wheelDeltaYToScaleFactor", () => {
  it("maps positive deltaY toward zoom out", () => {
    expect(wheelDeltaYToScaleFactor(100)).toBeLessThan(1)
  })

  it("maps negative deltaY toward zoom in", () => {
    expect(wheelDeltaYToScaleFactor(-100)).toBeGreaterThan(1)
  })

  it("clamps extreme deltas per event", () => {
    expect(wheelDeltaYToScaleFactor(5000)).toBe(0.88)
    expect(wheelDeltaYToScaleFactor(-5000)).toBe(1.12)
  })
})
