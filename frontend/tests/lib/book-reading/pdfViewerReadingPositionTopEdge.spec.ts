import {
  pdfViewerReadingPositionTopEdge,
  type PdfJsViewerForViewport,
} from "@/lib/book-reading/pdfViewerViewportTopYDown"
import { describe, expect, it } from "vitest"

function mockViewer(
  pageRects: Array<{ top: number; bottom: number }>
): PdfJsViewerForViewport {
  return {
    pagesCount: pageRects.length,
    pdfDocument: {},
    currentPageNumber: 1,
    getPageView: (i: number) => ({
      div: {
        getBoundingClientRect: () => {
          const r = pageRects[i]!
          const height = r.bottom - r.top
          return {
            top: r.top,
            bottom: r.bottom,
            left: 0,
            right: 100,
            width: 100,
            height,
            x: 0,
            y: r.top,
            toJSON: () => ({}),
          } as DOMRect
        },
      },
    }),
  } as unknown as PdfJsViewerForViewport
}

function mockContainer(top: number): HTMLElement {
  const el = document.createElement("div")
  el.getBoundingClientRect = () =>
    ({
      top,
      bottom: top + 400,
      left: 0,
      right: 200,
      width: 200,
      height: 400,
      x: 0,
      y: top,
      toJSON: () => ({}),
    }) as DOMRect
  return el
}

describe("pdfViewerReadingPositionTopEdge", () => {
  it("uses the page that contains the container top, not the center page", () => {
    const containerTop = 50
    const page0 = { top: 0, bottom: 200 }
    const page1 = { top: 200, bottom: 600 }
    const viewer = mockViewer([page0, page1])
    const container = mockContainer(containerTop)

    const res = pdfViewerReadingPositionTopEdge(container, viewer)
    expect(res).not.toBeNull()
    expect(res!.pageIndexZeroBased).toBe(0)
    expect(res!.normalizedTop).toBeCloseTo(250, 0)
  })

  it("returns page 1 when container top is on the second page", () => {
    const containerTop = 350
    const page0 = { top: 0, bottom: 200 }
    const page1 = { top: 200, bottom: 800 }
    const viewer = mockViewer([page0, page1])
    const container = mockContainer(containerTop)

    const res = pdfViewerReadingPositionTopEdge(container, viewer)
    expect(res).not.toBeNull()
    expect(res!.pageIndexZeroBased).toBe(1)
    expect(res!.normalizedTop).toBeCloseTo((150 / 600) * 1000, 0)
  })
})
