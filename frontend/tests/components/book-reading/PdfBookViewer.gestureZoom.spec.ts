import { describe, expect, it } from "vitest"
import {
  dispatchPinchZoom,
  dispatchWheelOnViewer,
  flushAnimationFrame,
  mountGestureZoomViewerReady,
  setupPdfBookViewerGestureZoomTests,
} from "./pdfBookViewerGestureZoomTestSupport"

describe("PdfBookViewer gesture zoom (mocked pdf.js)", () => {
  setupPdfBookViewerGestureZoomTests()

  it.each([
    { modifier: "ctrl", ctrlKey: true, metaKey: false },
    { modifier: "meta", ctrlKey: false, metaKey: true },
  ])(
    "$modifier+wheel on the viewer prevents default and updates pdf scale",
    async ({ ctrlKey, metaKey }) => {
      const { wrapper, host, container, viewer } =
        await mountGestureZoomViewerReady()
      expect(viewer).not.toBeNull()
      const baseline = viewer!.currentScale

      const { ev, notCanceled } = dispatchWheelOnViewer(container, host, {
        ctrlKey,
        metaKey,
      })

      expect(ev.defaultPrevented).toBe(true)
      expect(notCanceled).toBe(false)
      await flushAnimationFrame()
      expect(viewer!.currentScale).toBeGreaterThan(baseline)

      wrapper.unmount()
    }
  )

  it("wheel without ctrl/meta does not cancel (no browser-zoom block path)", async () => {
    const { wrapper, host, container, viewer } =
      await mountGestureZoomViewerReady()
    const baseline = viewer!.currentScale

    const { ev } = dispatchWheelOnViewer(container, host, {
      ctrlKey: false,
      metaKey: false,
    })

    expect(ev.defaultPrevented).toBe(false)
    expect(viewer!.currentScale).toBe(baseline)

    wrapper.unmount()
  })

  it("two-finger pinch touchmove updates scale around the midpoint", async () => {
    const { wrapper, container, viewer } = await mountGestureZoomViewerReady()
    expect(viewer).not.toBeNull()
    const baseline = viewer!.currentScale

    dispatchPinchZoom(container)

    expect(viewer!.currentScale).toBeGreaterThan(baseline)

    wrapper.unmount()
  })
})
